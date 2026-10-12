# Highlight & Shadow Editing – Android Implementation Blueprint

> **Purpose**: Claude Code implementation reference for building a professional-quality highlight/shadow/sharpness editing pipeline in an Android (Kotlin) app, operating on JPEG (8-bit) input.

---

## Architecture Overview

```
JPEG decode (8-bit sRGB)
    │
    ▼
Convert to Float32 RGB [0.0–1.0]       ← CRITICAL: Never process in 8-bit
    │
    ▼
Linearize (remove gamma)                ← sRGB → linear RGB
    │
    ▼
Extract Luminance Y                     ← BT.709: Y = 0.2126R + 0.7152G + 0.0722B
    │
    ├──► Edge-Aware Filter (Guided Filter) on Y  → Base Layer
    │
    ▼
Detail Layer = Y − Base
    │
    ▼
Remap Base Layer (shadow lift / highlight compress)
    │
    ▼
Reconstruct: Y_new = Remapped_Base + Detail
    │
    ▼
Scale RGB: output = input × (Y_new / Y)  ← preserves chrominance
    │
    ▼
Apply Adaptive Sharpening (luminance-only, tone-masked)
    │
    ▼
Add Film Grain to shadows (anti-banding dither)
    │
    ▼
Gamma encode (linear → sRGB)
    │
    ▼
Quantize to 8-bit, encode JPEG
```

---

## Module 1: JPEG Decode & Color Space Conversion

### 1.1 Decode to Float32

```kotlin
// Use Android BitmapFactory to decode, then immediately convert to float buffer
fun decodeToFloat(bitmap: Bitmap): FloatArray {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    // 4 channels: R, G, B, A as Float32 in [0.0, 1.0]
    val buffer = FloatArray(width * height * 3)
    for (i in pixels.indices) {
        val pixel = pixels[i]
        buffer[i * 3 + 0] = ((pixel shr 16) and 0xFF) / 255f  // R
        buffer[i * 3 + 1] = ((pixel shr 8) and 0xFF) / 255f   // G
        buffer[i * 3 + 2] = (pixel and 0xFF) / 255f            // B
    }
    return buffer
}
```

### 1.2 sRGB Linearization

```kotlin
// sRGB gamma decode (per channel)
fun srgbToLinear(c: Float): Float {
    return if (c <= 0.04045f) {
        c / 12.92f
    } else {
        ((c + 0.055f) / 1.055f).pow(2.4f)
    }
}

// Inverse: linear to sRGB
fun linearToSrgb(c: Float): Float {
    return if (c <= 0.0031308f) {
        c * 12.92f
    } else {
        1.055f * c.pow(1f / 2.4f) - 0.055f
    }
}
```

### 1.3 Luminance Extraction (BT.709)

```kotlin
fun extractLuminance(linearRgb: FloatArray, width: Int, height: Int): FloatArray {
    val lum = FloatArray(width * height)
    for (i in 0 until width * height) {
        lum[i] = 0.2126f * linearRgb[i * 3] +
                 0.7152f * linearRgb[i * 3 + 1] +
                 0.0722f * linearRgb[i * 3 + 2]
    }
    return lum
}
```

---

## Module 2: Guided Filter (Edge-Aware Base/Detail Decomposition)

The guided filter is the recommended edge-aware filter for Android: O(N) complexity, no gradient reversals, and available in OpenCV. This is the same approach used by darktable's Tone Equalizer and RawTherapee.

### 2.1 Algorithm (He, Sun & Tang 2010/2013)

**Input**: guidance image I (luminance), filtering input p (same as I for self-guided), window radius r, regularization ε

**For each local window ω_k centered at pixel k:**

```
mean_I_k    = mean(I) in window ω_k
mean_p_k    = mean(p) in window ω_k
corr_I_k    = mean(I * I) in window ω_k
corr_Ip_k   = mean(I * p) in window ω_k

var_I_k     = corr_I_k - mean_I_k²
cov_Ip_k    = corr_Ip_k - mean_I_k * mean_p_k

a_k = cov_Ip_k / (var_I_k + ε)
b_k = mean_p_k - a_k * mean_I_k
```

**Output at pixel i:**

```
q_i = mean(a_k) * I_i + mean(b_k)     (averaged over all windows containing i)
```

All means are computed via **box filter** (integral images / summed area tables) → O(N) total.

### 2.2 Kotlin/OpenCV Implementation

```kotlin
import org.opencv.core.Mat
import org.opencv.core.CvType
import org.opencv.imgproc.Imgproc
import org.opencv.ximgproc.Ximgproc

/**
 * Guided filter for base/detail decomposition.
 *
 * @param luminance  Single-channel float luminance image (CV_32F)
 * @param radius     Filter radius in pixels. Rule of thumb: 2% of image diagonal
 *                   e.g. for 4000x3000 image → radius ≈ 100
 * @param eps        Regularization. Controls edge sensitivity.
 *                   - eps = 0.01² (0.0001) → strong edge preservation (for tone mapping)
 *                   - eps = 0.1² (0.01)    → moderate smoothing
 *                   - eps = 0.4² (0.16)    → heavy smoothing
 * @return Pair(baseLayer, detailLayer) both CV_32F
 */
fun guidedFilterDecompose(
    luminance: Mat,
    radius: Int,
    eps: Float
): Pair<Mat, Mat> {
    val base = Mat()
    // Self-guided: guidance = input
    Ximgproc.guidedFilter(
        /* guide = */  luminance,
        /* src = */    luminance,
        /* dst = */    base,
        /* radius = */ radius,
        /* eps = */    eps.toDouble()
    )

    val detail = Mat()
    // detail = luminance - base
    org.opencv.core.Core.subtract(luminance, base, detail)

    return Pair(base, detail)
}
```

### 2.3 Pure Kotlin Implementation (No OpenCV)

If you want to avoid the OpenCV dependency, implement via integral images:

```kotlin
/**
 * Fast guided filter using integral images.
 * All arrays are [height][width] in row-major order.
 */
class GuidedFilter(
    private val width: Int,
    private val height: Int
) {
    // Box filter via integral image (SAT = Summed Area Table)
    private fun boxFilter(input: FloatArray, radius: Int): FloatArray {
        val sat = DoubleArray(width * height) // use double for precision
        val output = FloatArray(width * height)

        // Build SAT
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                sat[idx] = input[idx].toDouble() +
                    (if (x > 0) sat[idx - 1] else 0.0) +
                    (if (y > 0) sat[(y - 1) * width + x] else 0.0) -
                    (if (x > 0 && y > 0) sat[(y - 1) * width + x - 1] else 0.0)
            }
        }

        // Query box sums
        for (y in 0 until height) {
            for (x in 0 until width) {
                val x1 = maxOf(x - radius - 1, -1)
                val y1 = maxOf(y - radius - 1, -1)
                val x2 = minOf(x + radius, width - 1)
                val y2 = minOf(y + radius, height - 1)

                val area = ((x2 - x1) * (y2 - y1)).toFloat()

                fun satVal(yy: Int, xx: Int): Double {
                    if (yy < 0 || xx < 0) return 0.0
                    return sat[yy * width + xx]
                }

                val sum = satVal(y2, x2) - satVal(y1, x2) - satVal(y2, x1) + satVal(y1, x1)
                output[y * width + x] = (sum / area).toFloat()
            }
        }
        return output
    }

    /**
     * Self-guided filter: I = p = luminance
     */
    fun filter(luminance: FloatArray, radius: Int, eps: Float): FloatArray {
        val N = width * height

        // Step 1: Compute means and correlations via box filter
        val meanI = boxFilter(luminance, radius)

        val II = FloatArray(N) { luminance[it] * luminance[it] }
        val corrII = boxFilter(II, radius)

        // Step 2: Compute a and b
        val a = FloatArray(N)
        val b = FloatArray(N)
        for (i in 0 until N) {
            val varI = corrII[i] - meanI[i] * meanI[i]
            a[i] = varI / (varI + eps)
            b[i] = meanI[i] * (1f - a[i])
        }

        // Step 3: Average a and b over windows
        val meanA = boxFilter(a, radius)
        val meanB = boxFilter(b, radius)

        // Step 4: Output
        val output = FloatArray(N)
        for (i in 0 until N) {
            output[i] = meanA[i] * luminance[i] + meanB[i]
        }
        return output
    }
}
```

### 2.4 Parameter Tuning Guide

| Parameter | Effect | Recommended Range |
|-----------|--------|-------------------|
| `radius` | Size of local neighborhood. Larger = smoother base, more detail preserved | 1–4% of image diagonal. Start at 2%. |
| `eps` (ε) | Edge sensitivity. Smaller = sharper edge preservation | 0.0001 (strong edges) to 0.01 (softer). Start at 0.001. |

**Critical**: If `radius` is too small, large tonal regions bleed into the detail layer → halos. If `eps` is too large, edges are smoothed into the base layer → lost sharpness at edges.

---

## Module 3: Tone Remapping (Shadow/Highlight Sliders)

### 3.1 Base Layer Remapping Function

This operates on the **base layer** (not the raw luminance), preserving all texture in the detail layer.

```kotlin
/**
 * Remaps the base layer to lift shadows and compress highlights.
 *
 * @param base        Base layer from guided filter decomposition [0.0–1.0]
 * @param shadows     Shadow lift amount [-100, +100]. Positive = lift shadows.
 * @param highlights  Highlight compression [-100, +100]. Negative = recover highlights.
 * @param width       Image width
 * @param height      Image height
 * @return            Remapped base layer
 */
fun remapBaseTone(
    base: FloatArray,
    shadows: Float,      // [-100, 100] user slider value
    highlights: Float,   // [-100, 100] user slider value
    width: Int,
    height: Int
): FloatArray {
    val N = width * height
    val output = FloatArray(N)

    // Normalize slider values to [0, 1] working range
    val shadowAmt = shadows / 100f      // [-1, 1]
    val highlightAmt = highlights / 100f // [-1, 1]

    for (i in 0 until N) {
        val v = base[i].coerceIn(0.0001f, 0.9999f) // avoid div-by-zero and log(0)

        // Shadow mask: strongest effect on dark values, tapers toward midtones
        // Uses power curve for smooth falloff
        val shadowMask = (1f - v).pow(2f)  // quadratic falloff from dark

        // Highlight mask: strongest effect on bright values
        val highlightMask = v.pow(2f)      // quadratic falloff from bright

        // Shadow adjustment: lift using power curve (gamma-style)
        // Positive shadowAmt → lift shadows → lower gamma → brighter darks
        val shadowShift = if (shadowAmt > 0f) {
            // Lift: apply gamma < 1 weighted by shadow mask
            val gamma = 1f - shadowAmt * 0.6f  // range [0.4, 1.0]
            (v.pow(gamma) - v) * shadowMask
        } else {
            // Darken: apply gamma > 1 weighted by shadow mask
            val gamma = 1f - shadowAmt * 0.6f  // range [1.0, 1.6]
            (v.pow(gamma) - v) * shadowMask
        }

        // Highlight adjustment: compress using inverse power curve
        val highlightShift = if (highlightAmt < 0f) {
            // Recover (compress): darken highlights
            val strength = -highlightAmt * 0.7f
            (v - v.pow(1f / (1f + strength))) * highlightMask
        } else {
            // Boost highlights
            val strength = highlightAmt * 0.5f
            (v.pow(1f / (1f + strength)) - v) * highlightMask
        }

        output[i] = (v + shadowShift - highlightShift).coerceIn(0f, 1f)
    }
    return output
}
```

### 3.2 Advanced: Darktable-Style Tone Equalizer (9-Band)

For finer control, divide the luminance range into 9 EV bands and allow independent adjustment of each:

```kotlin
/**
 * Tone Equalizer: adjusts exposure independently for 9 EV zones.
 * Inspired by darktable's tone equalizer module.
 *
 * @param base        Base luminance layer (linear, [0–1])
 * @param bandGains   Array of 9 gain values in EV (e.g. [+1.0, +0.5, 0, 0, -0.3, ...])
 *                    Index 0 = deepest shadows (~-8 EV), index 8 = brightest highlights (~0 EV)
 * @return            Remapped base layer
 */
fun toneEqualizer(
    base: FloatArray,
    bandGains: FloatArray  // 9 values, each in EV [-4.0, +4.0]
): FloatArray {
    require(bandGains.size == 9) { "Exactly 9 band gains required" }

    val output = FloatArray(base.size)
    val bandCenters = floatArrayOf(-8f, -7f, -6f, -5f, -4f, -3f, -2f, -1f, 0f) // EV

    for (i in base.indices) {
        val v = base[i].coerceIn(1e-6f, 1f)
        val ev = ln(v.toDouble()).toFloat() / ln(2f) // convert to EV (log2)

        // Interpolate gain from neighboring bands using Gaussian weighting
        var totalWeight = 0f
        var totalGain = 0f
        val sigma = 1.0f // band width in EV

        for (b in 0 until 9) {
            val dist = ev - bandCenters[b]
            val weight = exp(-dist * dist / (2f * sigma * sigma))
            totalWeight += weight
            totalGain += weight * bandGains[b]
        }

        val gainEV = totalGain / totalWeight
        val multiplier = 2f.pow(gainEV) // convert EV gain to linear multiplier
        output[i] = (v * multiplier).coerceIn(0f, 1f)
    }
    return output
}
```

### 3.3 Luminance Reconstruction (Chrominance Preservation)

```kotlin
/**
 * Reconstruct full RGB from modified luminance, preserving original chrominance.
 *
 * @param originalLinearRgb  Original linear RGB buffer (3 channels interleaved)
 * @param originalLuminance  Original luminance (Y)
 * @param newLuminance       Modified luminance (Y_new = remapped_base + detail)
 * @return                   New linear RGB buffer
 */
fun reconstructRgb(
    originalLinearRgb: FloatArray,
    originalLuminance: FloatArray,
    newLuminance: FloatArray
): FloatArray {
    val N = originalLuminance.size
    val output = FloatArray(N * 3)

    for (i in 0 until N) {
        val ratio = if (originalLuminance[i] > 1e-6f) {
            newLuminance[i] / originalLuminance[i]
        } else {
            1f // avoid division by near-zero in very dark pixels
        }

        // Scale each RGB channel by the luminance ratio
        output[i * 3 + 0] = (originalLinearRgb[i * 3 + 0] * ratio).coerceIn(0f, 1f)
        output[i * 3 + 1] = (originalLinearRgb[i * 3 + 1] * ratio).coerceIn(0f, 1f)
        output[i * 3 + 2] = (originalLinearRgb[i * 3 + 2] * ratio).coerceIn(0f, 1f)
    }
    return output
}
```

---

## Module 4: Adaptive Sharpening (Luminance-Only, Tone-Masked)

### 4.1 Unsharp Mask on Luminance Channel

```kotlin
/**
 * Luminance-only unsharp mask with tone-aware masking.
 *
 * @param luminance    Luminance channel (linear float)
 * @param width        Image width
 * @param height       Image height
 * @param amount       Sharpening strength [0.0–3.0]. Recommended: 0.5–1.5
 * @param radius       Blur radius in pixels. Recommended: 1–3
 * @param threshold    Minimum difference to sharpen (noise gate). Recommended: 0.005–0.02
 * @param shadowProtect How much to reduce sharpening in shadows [0.0–1.0].
 *                      1.0 = no sharpening in deep shadows
 * @return             Sharpened luminance
 */
fun adaptiveUnsharpMask(
    luminance: FloatArray,
    width: Int,
    height: Int,
    amount: Float = 1.0f,
    radius: Int = 2,
    threshold: Float = 0.01f,
    shadowProtect: Float = 0.8f
): FloatArray {
    val N = width * height
    val output = FloatArray(N)

    // Step 1: Gaussian blur for the "unsharp" reference
    val blurred = gaussianBlur(luminance, width, height, radius)

    for (i in 0 until N) {
        val original = luminance[i]
        val diff = original - blurred[i]

        // Threshold gate: ignore differences smaller than threshold (noise)
        if (abs(diff) < threshold) {
            output[i] = original
            continue
        }

        // Tone-aware mask: reduce sharpening in shadows, full in midtones/highlights
        // Shadow region: luminance < 0.15 (roughly -3 EV)
        val toneMask = if (original < 0.15f) {
            val t = (original / 0.15f)  // 0 at black, 1 at shadow boundary
            1f - shadowProtect * (1f - t * t)  // quadratic ramp
        } else {
            1f // full sharpening in midtones and highlights
        }

        // Also reduce sharpening near highlight clipping to prevent overshoot
        val highlightMask = if (original > 0.9f) {
            val t = (1f - original) / 0.1f
            t.coerceIn(0f, 1f)
        } else {
            1f
        }

        val effectiveAmount = amount * toneMask * highlightMask
        output[i] = (original + effectiveAmount * diff).coerceIn(0f, 1f)
    }
    return output
}

/**
 * Simple Gaussian blur via two-pass separable box filter approximation.
 * For production, use 3-pass box filter (≈ Gaussian) or actual Gaussian kernel.
 */
fun gaussianBlur(
    input: FloatArray,
    width: Int,
    height: Int,
    radius: Int
): FloatArray {
    // Horizontal pass
    val temp = FloatArray(input.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            var sum = 0f
            var count = 0
            for (dx in -radius..radius) {
                val nx = (x + dx).coerceIn(0, width - 1)
                sum += input[y * width + nx]
                count++
            }
            temp[y * width + x] = sum / count
        }
    }
    // Vertical pass
    val output = FloatArray(input.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            var sum = 0f
            var count = 0
            for (dy in -radius..radius) {
                val ny = (y + dy).coerceIn(0, height - 1)
                sum += temp[ny * width + x]
                count++
            }
            output[y * width + x] = sum / count
        }
    }
    return output
}
```

---

## Module 5: Anti-Banding Film Grain (Shadow Dithering)

### 5.1 Perceptual Film Grain

```kotlin
import kotlin.random.Random

/**
 * Add subtle film grain to shadow regions to mask JPEG banding artifacts.
 * Applied AFTER tone remapping, BEFORE final sRGB encode.
 *
 * @param linearRgb   Linear RGB buffer (3-channel interleaved)
 * @param luminance   Luminance channel (used for tone masking)
 * @param grainAmount Grain intensity [0.0–0.05]. Recommended: 0.008–0.015
 * @param seed        Random seed for reproducible grain
 */
fun addFilmGrain(
    linearRgb: FloatArray,
    luminance: FloatArray,
    grainAmount: Float = 0.012f,
    seed: Long = 42L
) {
    val rng = Random(seed)
    val N = luminance.size

    for (i in 0 until N) {
        val lum = luminance[i]

        // Grain strength is inversely proportional to luminance
        // Full grain in deep shadows, none in highlights
        val grainMask = when {
            lum < 0.05f -> 1.0f                     // deep shadows: full grain
            lum < 0.25f -> 1f - (lum - 0.05f) / 0.2f // taper in shadows/midtones
            else -> 0f                                // no grain in midtones/highlights
        }

        if (grainMask > 0f) {
            // Gaussian-ish noise via Box-Muller or simple triangular PDF
            val noise1 = rng.nextFloat() - 0.5f
            val noise2 = rng.nextFloat() - 0.5f
            val grain = (noise1 + noise2) * grainAmount * grainMask // triangular PDF

            // Apply equally to all channels (luminance grain, not chroma)
            linearRgb[i * 3 + 0] = (linearRgb[i * 3 + 0] + grain).coerceIn(0f, 1f)
            linearRgb[i * 3 + 1] = (linearRgb[i * 3 + 1] + grain).coerceIn(0f, 1f)
            linearRgb[i * 3 + 2] = (linearRgb[i * 3 + 2] + grain).coerceIn(0f, 1f)
        }
    }
}
```

---

## Module 6: Local Laplacian Filter (Advanced, Highest Quality)

This is the algorithm behind Lightroom's Shadows/Highlights/Clarity sliders. More complex than guided filter decomposition but produces superior results, especially at extreme settings.

### 6.1 Algorithm Overview

```
For each pixel (x₀, y₀) at each pyramid level ℓ:
  1. Read reference value g₀ = GaussianPyramid[ℓ](x₀, y₀)
  2. Apply remapping r(I, g₀) to entire image:
     - |I - g₀| < σ_r  →  detail region: scale by α (clarity)
     - |I - g₀| ≥ σ_r  →  edge region: compress by β (shadow/highlight)
  3. Build Laplacian pyramid of remapped image
  4. Extract coefficient at (x₀, y₀) level ℓ → output pyramid
Reconstruct image from output Laplacian pyramid
```

### 6.2 Remapping Function

```kotlin
/**
 * Local Laplacian remapping function.
 *
 * @param pixelValue  Input pixel intensity [0, 1]
 * @param reference   Reference (center) intensity from Gaussian pyramid
 * @param sigmaR      Detail/edge threshold. Recommended: 0.1–0.4
 * @param alpha       Detail boost/smooth. <1 = boost clarity, >1 = smooth. Range: [0.25, 4.0]
 * @param beta        Edge compression. <1 = compress (recover shadows/highlights). Range: [0, 1]
 */
fun localLaplacianRemap(
    pixelValue: Float,
    reference: Float,
    sigmaR: Float,
    alpha: Float,
    beta: Float
): Float {
    val delta = pixelValue - reference

    return if (abs(delta) < sigmaR) {
        // Detail region: scale detail magnitude by alpha
        // Using smooth curve (derivative of Gaussian) for natural transition
        val fraction = delta / sigmaR
        val detailScaled = sigmaR * sign(delta) *
            abs(fraction).pow(alpha) // power curve for detail manipulation
        reference + detailScaled
    } else {
        // Edge region: compress/expand by beta
        val sign = sign(delta)
        val absDelta = abs(delta)
        val excessEdge = absDelta - sigmaR

        // Smoothly transition at the boundary
        val detailPart = sigmaR * sign * abs(1f).pow(alpha) // detail at boundary
        val edgePart = sign * (sigmaR + beta * excessEdge)  // compressed edge

        reference + edgePart
    }
}
```

### 6.3 Fast Approximation (Aubry et al. 2014)

Instead of computing per-pixel, sample at N discrete intensity levels and interpolate:

```kotlin
/**
 * Fast Local Laplacian filter using intensity sampling.
 * Complexity: O(S × N log N) where S = number of samples, N = pixels
 *
 * @param luminance  Input luminance [0, 1]
 * @param numLevels  Pyramid levels (typically 5–7 for full-res images)
 * @param numSamples Number of intensity samples (10–20 for good quality)
 * @param sigmaR     Detail/edge threshold
 * @param alpha      Detail manipulation (clarity)
 * @param beta       Edge compression (shadow/highlight)
 */
fun fastLocalLaplacian(
    luminance: FloatArray,
    width: Int,
    height: Int,
    numLevels: Int = 6,
    numSamples: Int = 15,
    sigmaR: Float = 0.2f,
    alpha: Float = 1.0f,  // 1.0 = neutral clarity
    beta: Float = 1.0f    // 1.0 = neutral; <1.0 = compress dynamic range
): FloatArray {
    // Sample reference intensities uniformly across [0, 1]
    val sampleValues = FloatArray(numSamples) { it.toFloat() / (numSamples - 1) }

    // For each sample: remap full image, build Laplacian pyramid
    val remappedPyramids = Array(numSamples) { s ->
        val remapped = FloatArray(luminance.size) { i ->
            localLaplacianRemap(luminance[i], sampleValues[s], sigmaR, alpha, beta)
        }
        buildLaplacianPyramid(remapped, width, height, numLevels)
    }

    // Build Gaussian pyramid of original (for reference values)
    val gaussPyramid = buildGaussianPyramid(luminance, width, height, numLevels)

    // For each level and pixel, interpolate between the two nearest samples
    val outputPyramid = Array(numLevels + 1) { level ->
        val levelWidth = width shr level
        val levelHeight = height shr level
        val outputLevel = FloatArray(levelWidth * levelHeight)

        for (i in outputLevel.indices) {
            val ref = gaussPyramid[level][i]
            val sampleIdx = ref * (numSamples - 1)
            val lo = sampleIdx.toInt().coerceIn(0, numSamples - 2)
            val hi = lo + 1
            val t = sampleIdx - lo // interpolation weight

            // Linear interpolation between two nearest sample pyramids
            outputLevel[i] = (1f - t) * remappedPyramids[lo][level][i] +
                                   t  * remappedPyramids[hi][level][i]
        }
        outputLevel
    }

    return collapseLaplacianPyramid(outputPyramid, width, height)
}
```

### 6.4 Mapping User Sliders to Local Laplacian Parameters

```kotlin
/**
 * Convert user-facing slider values to Local Laplacian parameters.
 *
 * Shadows:    [-100, +100]  → controls β for dark tones
 * Highlights: [-100, +100]  → controls β for bright tones
 * Clarity:    [-100, +100]  → controls α (detail manipulation)
 */
data class LocalLaplacianParams(
    val sigmaR: Float,
    val alpha: Float,
    val betaShadows: Float,
    val betaHighlights: Float
)

fun slidersToParams(
    shadows: Float,       // [-100, 100]
    highlights: Float,    // [-100, 100]
    clarity: Float        // [-100, 100]
): LocalLaplacianParams {
    // Clarity → alpha
    // -100 → alpha = 2.0 (smooth details)
    //    0 → alpha = 1.0 (neutral)
    // +100 → alpha = 0.25 (boost details / local contrast)
    val alpha = if (clarity >= 0) {
        1f - clarity / 100f * 0.75f    // [1.0, 0.25]
    } else {
        1f - clarity / 100f * 1.0f     // [1.0, 2.0]
    }

    // Shadows → beta for dark tones
    // +100 → beta = 0.2 (strong shadow lift = strong compression)
    //    0 → beta = 1.0 (neutral)
    // -100 → beta = 2.0 (deepen shadows)
    val betaShadows = 1f - shadows / 100f * 0.8f

    // Highlights → beta for bright tones
    // -100 → beta = 0.2 (strong highlight recovery)
    //    0 → beta = 1.0 (neutral)
    // +100 → beta = 2.0 (boost highlights)
    val betaHighlights = 1f + highlights / 100f * 1.0f

    return LocalLaplacianParams(
        sigmaR = 0.2f,  // fixed; could expose as "Radius" control
        alpha = alpha.coerceIn(0.1f, 4f),
        betaShadows = betaShadows.coerceIn(0.1f, 3f),
        betaHighlights = betaHighlights.coerceIn(0.1f, 3f)
    )
}
```

---

## Module 7: GPU Acceleration (RenderScript / Vulkan Compute)

### 7.1 GLSL Fragment Shader – Simple Highlight/Shadow (GPUImage-compatible)

For a quick GPU implementation without full decomposition:

```glsl
// highlight_shadow.frag
precision highp float;

uniform sampler2D inputTexture;
uniform float shadows;     // [0.0, 1.0] where 0.5 = neutral
uniform float highlights;  // [0.0, 1.0] where 0.5 = neutral

varying vec2 textureCoordinate;

void main() {
    vec4 source = texture2D(inputTexture, textureCoordinate);

    // Luminance (BT.709)
    float luminance = dot(source.rgb, vec3(0.2126, 0.7152, 0.0722));

    // Shadow adjustment (power curve)
    float shadowExp = 1.0 / (shadows + 0.5);  // 0.67–2.0 range
    float shadowMask = pow(1.0 - luminance, 2.0);
    float shadowAdj = shadowMask * (pow(luminance, shadowExp) - luminance);

    // Highlight adjustment (inverse power curve)
    float highlightExp = 1.0 / (1.5 - highlights);  // 0.67–2.0 range
    float highlightMask = pow(luminance, 2.0);
    float highlightAdj = highlightMask * (pow(luminance, highlightExp) - luminance);

    // Apply adjustments while preserving color ratios
    float newLum = luminance + shadowAdj + highlightAdj;
    float ratio = (luminance > 0.001) ? (newLum / luminance) : 1.0;

    vec3 result = source.rgb * ratio;
    gl_FragColor = vec4(clamp(result, 0.0, 1.0), source.a);
}
```

### 7.2 Compute Shader – Guided Filter (Vulkan/OpenGL ES 3.1)

```glsl
// guided_filter_box_pass.comp
#version 310 es
layout(local_size_x = 16, local_size_y = 16) in;

layout(binding = 0, r32f) readonly uniform image2D inputImage;
layout(binding = 1, r32f) writeonly uniform image2D outputImage;
// Use shared memory for integral image tile computation

uniform int radius;
uniform ivec2 imageSize;

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
    if (pos.x >= imageSize.x || pos.y >= imageSize.y) return;

    // Box filter via loop (for simplicity; production should use SAT)
    float sum = 0.0;
    int count = 0;
    for (int dy = -radius; dy <= radius; dy++) {
        for (int dx = -radius; dx <= radius; dx++) {
            ivec2 samplePos = clamp(pos + ivec2(dx, dy), ivec2(0), imageSize - 1);
            sum += imageLoad(inputImage, samplePos).r;
            count++;
        }
    }
    imageStore(outputImage, pos, vec4(sum / float(count)));
}
```

---

## Module 8: Complete Pipeline Orchestration

```kotlin
/**
 * Full highlight/shadow editing pipeline.
 *
 * @param inputBitmap     Source JPEG decoded bitmap
 * @param shadows         Shadow slider [-100, +100]
 * @param highlights      Highlight slider [-100, +100]
 * @param clarity         Clarity/local contrast [-100, +100]
 * @param sharpness       Sharpening amount [0, 100]
 * @param grainAmount     Film grain intensity [0, 100]
 * @param useLocalLaplacian  true = highest quality (slower), false = guided filter (faster)
 * @return                Processed bitmap
 */
fun processImage(
    inputBitmap: Bitmap,
    shadows: Float = 0f,
    highlights: Float = 0f,
    clarity: Float = 0f,
    sharpness: Float = 50f,
    grainAmount: Float = 10f,
    useLocalLaplacian: Boolean = false
): Bitmap {
    val width = inputBitmap.width
    val height = inputBitmap.height

    // Step 1: Decode to float32
    val srgbFloat = decodeToFloat(inputBitmap)

    // Step 2: Linearize
    for (i in srgbFloat.indices) {
        srgbFloat[i] = srgbToLinear(srgbFloat[i])
    }
    val linearRgb = srgbFloat.copyOf()

    // Step 3: Extract luminance
    val luminance = extractLuminance(linearRgb, width, height)

    // Step 4: Tone adjustment
    val newLuminance: FloatArray

    if (useLocalLaplacian && (shadows != 0f || highlights != 0f || clarity != 0f)) {
        // Highest quality path: Local Laplacian filter
        val params = slidersToParams(shadows, highlights, clarity)
        newLuminance = fastLocalLaplacian(
            luminance, width, height,
            sigmaR = params.sigmaR,
            alpha = params.alpha,
            beta = (params.betaShadows + params.betaHighlights) / 2f // simplified
        )
    } else if (shadows != 0f || highlights != 0f) {
        // Good quality, faster: Guided filter decomposition
        val guidedFilter = GuidedFilter(width, height)
        val radius = (maxOf(width, height) * 0.02f).toInt().coerceIn(5, 200)
        val eps = 0.001f

        val base = guidedFilter.filter(luminance, radius, eps)
        val detail = FloatArray(luminance.size) { luminance[it] - base[it] }

        // Remap base
        val remappedBase = remapBaseTone(base, shadows, highlights, width, height)

        // Reconstruct luminance
        newLuminance = FloatArray(luminance.size) { remappedBase[it] + detail[it] }
    } else {
        newLuminance = luminance.copyOf()
    }

    // Step 5: Reconstruct RGB
    val processedRgb = reconstructRgb(linearRgb, luminance, newLuminance)

    // Step 6: Adaptive sharpening (on new luminance)
    if (sharpness > 0f) {
        val sharpenedLum = adaptiveUnsharpMask(
            newLuminance, width, height,
            amount = sharpness / 50f,     // [0, 2]
            radius = 2,
            threshold = 0.008f,
            shadowProtect = 0.8f
        )
        // Re-apply sharpened luminance
        val finalRgb = reconstructRgb(processedRgb, newLuminance, sharpenedLum)
        System.arraycopy(finalRgb, 0, processedRgb, 0, finalRgb.size)
    }

    // Step 7: Film grain (anti-banding)
    if (grainAmount > 0f) {
        addFilmGrain(
            processedRgb,
            newLuminance,
            grainAmount = grainAmount / 1000f  // [0, 0.1]
        )
    }

    // Step 8: Gamma encode back to sRGB
    for (i in processedRgb.indices) {
        processedRgb[i] = linearToSrgb(processedRgb[i].coerceIn(0f, 1f))
    }

    // Step 9: Convert back to Bitmap
    return floatToBitmap(processedRgb, width, height)
}

fun floatToBitmap(rgb: FloatArray, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    for (i in pixels.indices) {
        val r = (rgb[i * 3 + 0] * 255f + 0.5f).toInt().coerceIn(0, 255)
        val g = (rgb[i * 3 + 1] * 255f + 0.5f).toInt().coerceIn(0, 255)
        val b = (rgb[i * 3 + 2] * 255f + 0.5f).toInt().coerceIn(0, 255)
        pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
```

---

## JPEG-Specific Artifact Mitigation Checklist

| Artifact | Cause | Mitigation |
|----------|-------|------------|
| **Banding / posterization** | 8-bit quantization + shadow stretch | Film grain dithering (Module 5), 16-bit internal processing |
| **Block artifacts** | 8×8 DCT block boundaries amplified | Deblock filter before tonal adjustment; or bilateral filter at block boundaries |
| **Color noise in shadows** | Chroma subsampling + quantization | Denoise chroma channels (Cb/Cr) independently; sharpen luminance only |
| **Hue shifts** | Non-proportional RGB channel clipping | Process in luminance+chroma separation; avoid Lab for extreme adjustments |
| **Halos around edges** | Base layer smoothing across edges | Use guided filter (not Gaussian); keep eps small; use Local Laplacian for best results |
| **Flat "HDR look"** | Over-compression of tonal range | Preserve midtone contrast; keep shadows slider < +60; use clarity to restore local contrast |

---

## JPEG Quality-Based Parameter Limits

```kotlin
/**
 * Estimate safe adjustment limits based on JPEG quality.
 * Higher quality JPEG = more editing headroom.
 *
 * @param jpegQuality  Estimated JPEG quality [1-100]. If unknown, assume 85.
 */
fun getEditingLimits(jpegQuality: Int): EditingLimits {
    return when {
        jpegQuality >= 95 -> EditingLimits(
            maxShadowLift = 80f,    // ~1.5 stops
            maxHighlightRecovery = 60f,
            maxClarity = 80f,
            recommendGrain = false
        )
        jpegQuality >= 80 -> EditingLimits(
            maxShadowLift = 50f,    // ~1 stop
            maxHighlightRecovery = 40f,
            maxClarity = 60f,
            recommendGrain = true
        )
        else -> EditingLimits(
            maxShadowLift = 30f,    // ~0.5 stops
            maxHighlightRecovery = 25f,
            maxClarity = 40f,
            recommendGrain = true   // essential for low-quality JPEG
        )
    }
}

data class EditingLimits(
    val maxShadowLift: Float,
    val maxHighlightRecovery: Float,
    val maxClarity: Float,
    val recommendGrain: Boolean
)
```

---

## Dependencies & Build Configuration

```kotlin
// build.gradle.kts (app module)
dependencies {
    // Option A: OpenCV (recommended for guided filter + more)
    implementation("org.opencv:opencv-android:4.9.0")

    // Option B: GPUImage (lightweight, GPU shaders only)
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")

    // For JPEG quality estimation
    // Uses ExifInterface to read quality metadata
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}
```

---

## Performance Targets (Reference)

| Operation | 12MP image | 48MP image |
|-----------|-----------|-----------|
| JPEG decode + float conversion | ~80ms | ~300ms |
| Guided filter (r=100, OpenCV) | ~120ms | ~450ms |
| Tone remap (Kotlin CPU) | ~30ms | ~120ms |
| Unsharp mask (CPU) | ~50ms | ~200ms |
| Film grain (CPU) | ~15ms | ~60ms |
| sRGB encode + bitmap | ~40ms | ~160ms |
| **Total (guided filter path)** | **~335ms** | **~1.3s** |
| Fast Local Laplacian (15 samples, 6 levels) | ~800ms | ~3s |

GPU compute (Vulkan/OpenGL ES) typically achieves 3–5× speedup over CPU for the filter and remap stages. Real-time preview at 1/4 resolution is achievable at 30+ fps.

---

## References

- Paris, Hasinoff, Kautz. "Local Laplacian Filters" (SIGGRAPH 2011)
- Aubry, Paris, Hasinoff, Kautz, Durand. "Fast Local Laplacian Filters" (2014)
- He, Sun, Tang. "Guided Image Filtering" (ECCV 2010, TPAMI 2013)
- Farbman, Fattal, Lischinski, Szeliski. "Edge-Preserving Decompositions" (SIGGRAPH 2008)
- Durand, Dorsey. "Fast Bilateral Filtering for HDR Display" (SIGGRAPH 2002)
- Gastal, Oliveira. "Domain Transform for Edge-Aware Processing" (SIGGRAPH 2011)
- Mertens, Kautz, Van Reeth. "Exposure Fusion" (Pacific Graphics 2007)
- Reinhard et al. "Photographic Tone Reproduction" (SIGGRAPH 2002)
- darktable source: `src/common/locallaplacian.c`, `src/iop/shadhi.c`, `src/iop/toneequal.c`
- GPUImage Android: `github.com/cats-oss/android-gpuimage`
