# The algorithms behind highlight and shadow editing in photo apps

**Local Laplacian filters power the shadow and highlight sliders in Adobe Lightroom, Photoshop CS6, and darktable's local contrast module** — a technique co-developed by Sylvain Paris at Adobe Research and published at SIGGRAPH 2011. This single algorithm family, combined with edge-aware filtering and multi-scale decomposition, forms the backbone of virtually all professional highlight/shadow editing. For an Android developer working with 8-bit JPEG data, understanding these algorithms is essential because JPEG's limited dynamic range (roughly 6–8 usable stops versus 14–15 from RAW) makes every implementation decision — from filter choice to color space — directly visible in the output quality.

This report covers the complete technical stack: from the mathematical formulations of core tone-mapping algorithms, through edge-preserving filters that prevent halos, to the specific JPEG artifacts you'll encounter when pushing shadows on 8-bit data. Each section includes implementation-relevant details, open-source references, and the academic papers behind them.

---

## How professional apps actually implement their shadow/highlight sliders

The critical architectural insight is that professional shadow/highlight tools are **not** simple tone curves. A tone curve maps each input value to exactly one output value regardless of context. Professional tools instead use **spatially-varying, edge-aware local adjustments** — pixels of the same brightness can receive different adjustments depending on their neighborhood. This is what prevents the flat, washed-out look of naive approaches.

**Adobe Lightroom (Process Version 2012+)** uses Local Laplacian Filters, confirmed by Samuel Hasinoff's research page: *"The controls for shadows, highlights, and clarity introduced in Lightroom 4 and Photoshop CS6 are inspired by our work on local Laplacian filters."* The algorithm builds a luminance-based mask limiting each slider's effect to specific tonal ranges. The Shadows slider has maximum effect on the darkest tones and tapers toward highlights. For slider values beyond ±50%, a "halo mask" considers whether a pixel sits in a generally light or dark region — not just its individual value — preventing the halos typical in older HDR processing.

**Photoshop's Shadow/Highlight command** uses a different, older approach: a local-neighborhood adaptive contrast algorithm. Each pixel is adjusted based on surrounding pixels within a user-defined Radius. The Amount controls intensity, Tonal Width controls how far into midtones the effect reaches, and Radius defines the local neighborhood. Because it references surrounding pixels (not just individual values), it cannot be replicated with a lookup table. A simplified global approximation of the per-pixel math:

```
luminance = sqrt(0.299·R² + 0.587·G² + 0.114·B²)
shadow_adj = shadows × 0.05 × (pow(8.0, 1.0 - luminance) - 1.0)
highlight_adj = highlights × 0.05 × (pow(8.0, luminance) - 1.0)
output = source + shadow_adj + highlight_adj
```

The actual Photoshop implementation adds spatial awareness through its Radius parameter, making it behave like a bilateral-filter decomposition.

**Darktable** offers three distinct modules. The legacy Shadows and Highlights module operates in Lab color space using bilateral or Gaussian blur for base-detail decomposition. The modern **Tone Equalizer** (by Aurélien Pierre) works in linear RGB with a guided-filter mask, dividing the image into tonal zones like a graphic equalizer for brightness and applying per-zone exposure compensation. The **Local Contrast** module implements Local Laplacian filters directly, with the Paris et al. 2011 algorithm. The darktable manual explicitly recommends the Tone Equalizer over the legacy module because Lab-space processing can cause hue shifts toward blue in shadows.

**RawTherapee** (since version 5.5) uses a **fast guided filter** for its shadow/highlight module, operating in RGB space to preserve saturation. It also implements Fattal's gradient-domain compression and Mantiuk's algorithm for dynamic range compression.

**Google Snapseed** likely uses single-image exposure fusion — synthesizing virtual multi-exposure frames from one photo via gamma adjustments, then fusing them with Mertens et al.'s Laplacian pyramid blending approach.

---

## Local Laplacian filters: the core algorithm explained

The Local Laplacian Filter (Paris, Hasinoff & Kautz, SIGGRAPH 2011) transforms standard Laplacian pyramids into edge-aware tools through a deceptively simple modification: applying point-wise nonlinear **remapping functions** before pyramid construction.

The standard Laplacian pyramid decomposes an image into frequency bands. The problem: edges create correlated large-amplitude coefficients across multiple pyramid levels. Naively modifying these coefficients (to compress highlights or boost shadows) disrupts edge structure and produces halos. Local Laplacian filters solve this by distinguishing edges from details at each pixel.

**The algorithm works as follows.** For each output pyramid coefficient at position (x₀, y₀) and scale ℓ:

1. Read the reference intensity g₀ = G_ℓ(x₀, y₀) from the Gaussian pyramid
2. Apply a remapping function r(i, g₀) to the image, centered on g₀:
   - For **details** (|i − g₀| < σ_r): apply detail manipulation (parameter α controls enhancement vs. smoothing)
   - For **edges** (|i − g₀| ≥ σ_r): apply edge compression or preservation (parameter β)
3. Build the Laplacian pyramid of the remapped image
4. Extract the single coefficient at (x₀, y₀) at scale ℓ for the output

The remapping function in its simplest form:

```
r(i, g₀) = g₀ + sign(i − g₀) · σ_r · f(|i − g₀| / σ_r)
```

Where f controls behavior: **α < 1** amplifies details (the "Clarity" slider), **α > 1** smooths them, and **β < 1** compresses edge contrast (shadow/highlight recovery). Darktable's open-source implementation uses an S-shaped contrast curve (modeled as the derivative of a Gaussian) in the center region, with linear slopes on the tails controlling shadow and highlight contrast separately.

**Why it avoids halos**: Unlike bilateral filter decomposition, where a blurred base layer creates halo-producing artifacts in the detail residual, Local Laplacian filters process each coefficient independently based on its local reference value. Edge structure is maintained across scales because edges and details are treated through fundamentally different branches of the remapping function.

The original algorithm is O(N²) — prohibitively slow. The **Fast Local Laplacian** (Aubry et al., 2014) achieves **50× speedup** to O(N log N) by sampling the remapping function at discrete intensity levels and interpolating, making it practical for real-time editing.

Two other tone-mapping algorithms remain relevant. **Reinhard tone mapping** (2002) uses a global operator L_out = L·(1 + L/L²_white) / (1 + L) applied to luminance after normalizing by the scene's log-average luminance L̄ = exp((1/N) Σ log(δ + L)). Its local variant simulates photographic dodging and burning. **Exposure Fusion** (Mertens et al., 2007) directly blends bracketed exposures without creating an intermediate HDR image, using per-pixel quality metrics (contrast, saturation, well-exposedness) blended through Laplacian pyramid fusion.

---

## Edge-aware filters compared: bilateral, guided, WLS, and domain transform

All professional shadow/highlight tools rely on edge-aware filters to decompose images into base (large-scale tone) and detail (texture) layers. The base layer gets compressed or expanded; the detail layer is preserved. The quality of this decomposition determines whether results look natural or exhibit halos.

The **bilateral filter** (Tomasi & Manduchi, 1998) weights each pixel's neighborhood by both spatial distance and intensity similarity: BLF(p) = (1/k) Σ G_σs(‖p−q‖) · G_σr(|I_p − I_q|) · I_q. Durand & Dorsey (2002) used it for HDR tone mapping by decomposing log-luminance into base + detail layers. Its fundamental limitation, demonstrated by Farbman et al. (2008), is that it **cannot achieve progressive coarsening** — increasing spatial support (σs) alone causes convergence to a range filter, while increasing range support (σr) blurs edges. It also produces **gradient reversal artifacts** at isolated edge pixels where few similar-valued neighbors exist.

The **guided filter** (He, Sun & Tang, 2010/2013) fits a local linear model q_i = a_k · I_i + b_k within each window, ensuring ∇q = a·∇I — output gradients exist only where the guidance image has gradients. Coefficients are computed in closed form: a_k = cov(I, p) / (var(I) + ε), b_k = p̄ − a_k · μ_k. Key advantages: **O(N) exact algorithm** independent of kernel size (using integral images), **no gradient reversal artifacts**, and the ability to use a separate guidance image. RawTherapee and darktable's Tone Equalizer both use guided filters. Darktable additionally implements an **Exposure-Independent Guided Filter (EIGF)** to address the standard guided filter's tendency to blur shadows more than highlights.

The **Weighted Least Squares filter** (Farbman et al., 2008) minimizes Σ[(u_p − g_p)² + λ(a_x(∂u/∂x)² + a_y(∂u/∂y)²)] where smoothness weights a_x = (|∂ℓ/∂x|^α + ε)^{−1} are inversely proportional to log-luminance gradients. This reduces to the sparse linear system (I + λL_g)u = g. Its chief advantage is **excellent progressive coarsening** via increasing λ, producing clean multi-scale decompositions. The drawback is computational cost (~3 seconds per megapixel), though the Fast Global Smoother reduces this to ~47ms.

The **domain transform filter** (Gastal & Oliveira, 2011) maps pixel positions to a new domain preserving geodesic distances on the image manifold, then applies simple 1D filters in the transformed space. It achieves **~7ms per megapixel on GPU** — 500× faster than WLS — with quality between bilateral and guided filters. Runtime is independent of filter parameters. Best suited for real-time video and interactive applications.

| Filter | Speed | Halo quality | Multi-scale | Best use case |
|--------|-------|-------------|-------------|---------------|
| Bilateral | Medium | Good (gradient reversals possible) | Limited | Simple two-layer decomposition |
| Guided | Fast (O(N), ~40ms/MP) | Very good | Two-scale; iterable | Real-time apps, darktable/RawTherapee |
| WLS | Slow (~3s/MP; fast ~47ms) | Excellent | Excellent progressive | Highest-quality multi-scale editing |
| Domain transform | Very fast (~7ms/MP GPU) | Good | Multi-scale via iteration | Real-time video, mobile apps |
| Local Laplacian | Medium (O(N log N)) | Excellent, halo-free by design | Inherently multi-scale | Lightroom, darktable local contrast |

---

## Multi-scale decomposition and frequency separation

Gaussian and Laplacian pyramids form the mathematical backbone of multi-scale editing. The Gaussian pyramid is constructed by repeated low-pass filtering and 2× downsampling: G_{ℓ+1} = downsample(blur(G_ℓ)). The Laplacian pyramid captures band-pass detail: L_ℓ = G_ℓ − upsample(G_{ℓ+1}), with perfect reconstruction via G_ℓ = L_ℓ + upsample(G_{ℓ+1}).

**WLS-based multi-scale decomposition** (Farbman et al., 2008) avoids downsampling entirely. Progressively smoother versions u₁, u₂, ..., u_k are computed with increasing λ, all at full resolution. Detail layers d_i = u_{i−1} − u_i capture features at specific spatial scales. This overcomplete representation allows independent manipulation of fine, medium, and coarse detail layers — for example, boosting fine texture (d₁) while compressing coarse tonal variation (u_k) to lift shadows without destroying surface detail.

**Exposure Fusion** (Mertens et al., 2007) blends multiple exposures directly through Laplacian pyramid fusion. Weight maps are computed from three quality metrics per pixel in each exposure: contrast (|∇²I|), saturation (σ_RGB), and well-exposedness (∏ exp(−(I_c − 0.5)² / 2σ²)). The final output is assembled as L{output}_ℓ = Σ_k G{w_k}_ℓ · L{image_k}_ℓ — Gaussian-blurred weights multiplied by Laplacian image coefficients at each scale, then collapsed. For single-image HDR (as in Snapseed), virtual multi-exposure is synthesized by applying different gamma curves to the same image before fusion.

The critical distinction between **linear decomposition** (standard pyramids) and **edge-preserving decomposition** (bilateral/WLS/guided) is halo behavior. Linear decomposition blurs edges across scales, so modifying coefficients creates ringing artifacts in the reconstructed image. Edge-preserving decomposition maintains sharp edges in the base layer, producing clean detail residuals that can be aggressively boosted without halos. Local Laplacian filters bridge both paradigms — they use standard pyramid structure but apply content-dependent nonlinear remapping, combining the simplicity of linear pyramids with edge-aware quality.

---

## JPEG's 8-bit ceiling and how to work within it

Working with JPEG fundamentally constrains what's possible. A **14-bit RAW file has 16,384 levels per channel** versus JPEG's 256. In linear encoding, the brightest stop alone consumes ~8,192 of those 16,384 levels (50% of all data). Even with JPEG's gamma encoding redistributing levels more uniformly, the total cap of 256 means shadow regions contain very few discrete values. Practical recovery from JPEG is limited to roughly **1–1.5 stops** in either direction before severe quality degradation, versus 4–5 stops of shadow recovery from RAW.

**Quantization artifacts** intensify when pushing shadows. JPEG's DCT quantization introduces errors bounded by ±Q/2 for each coefficient (where Q is the quantization step). Lifting shadows by N stops amplifies these errors by 2^N. At +3 stops, quantization noise becomes **8× larger** — easily visible. The 8×8 block structure becomes apparent because adjacent blocks quantize independently, creating discontinuities at boundaries. These blocking artifacts are most visible in smooth, dark regions (skies, gradients) where texture doesn't mask them.

**Banding** (posterization) occurs because stretching a narrow range of input values across a wider output range exposes the gaps between discrete levels. In 8-bit gamma-corrected space, a pixel value of 5 jumping to 6 represents a **20% relative luminance change** — far above the ~1% just-noticeable difference threshold predicted by Weber's law. Banding in lifted shadows is mathematically inevitable without mitigation.

**Color channel clipping** in highlights is permanent in JPEG. When any RGB channel hits 255, that data is gone. If only red clips while green and blue continue increasing, the pixel shifts toward cyan. RAW files may retain usable data in unclipped channels for partial reconstruction; JPEG cannot.

Five practical mitigation techniques for Android implementation:

- **Deblocking filters**: Apply adaptive bilateral or wavelet-domain filtering at block boundaries before tonal adjustments. Google's Knusperli decoder treats quantized coefficients as intervals [c·Q − Q/2, c·Q + Q/2] and selects values minimizing inter-block discontinuities — a technique you can adapt post-decode.
- **16-bit internal processing**: Immediately convert decoded 8-bit JPEG to 16-bit (or floating-point) internally. This prevents further quantization during multi-step processing. All intermediate operations — filtering, decomposition, remapping — should use at least 16-bit precision.
- **Adaptive noise reduction before shadow lifting**: Treat quantization noise like sensor noise. Non-local means filtering in wavelet domain can distinguish significant (edge-related) coefficients from noise, applying heavier smoothing to the latter. Filter parameters should scale with the JPEG quality factor.
- **Dithering to mask banding**: After tonal adjustment, inject subtle noise (1–2% amplitude) into shadow regions. Multi-scale probabilistic dithering detects banding presence and perturbs only affected regions. Natural photographs contain sensor noise that acts as accidental dithering; JPEG compression partially removes this, so re-adding controlled noise restores smooth gradation.
- **Conservative adjustment ranges**: Limit shadow lifting to roughly +1.5 stops and highlight recovery to −1 stop for JPEG inputs. Beyond this, artifacts dominate regardless of mitigation.

---

## Sharpness perception, luminosity masking, and adaptive sharpening

**Sharpness perception differs fundamentally between highlights and shadows** due to the human visual system's contrast sensitivity characteristics. Weber's law states that the just-noticeable difference in luminance is proportional to background luminance: ΔL_JND / L ≈ 0.01. At low luminance (shadows), even tiny absolute changes are detectable — including noise and artifacts. At high luminance (highlights), larger absolute changes pass unnoticed. However, at very low luminance, sensitivity decreases again due to retinal noise, following a power law correction.

The practical consequence: **noise in lifted shadows destroys perceived sharpness** because shadow regions lack the high-frequency texture that would mask noise through visual masking. Highlights naturally contain specular reflections and high-contrast transitions that the visual system uses to assess focus. Lifting shadows by +4 EV amplifies noise by 16×, and in JPEG, this "noise" includes both baked-in sensor noise and structured quantization artifacts — the latter being especially objectionable because of their blocky, non-random pattern.

**Luminosity masking** provides the solution for selective, tone-targeted processing. The technique, formalized by Tony Kuyper (2006), computes masks directly from pixel luminance:

```
Lights: L(x,y) = 0.2126·R + 0.7152·G + 0.0722·B  (normalized to [0,255])
Darks:  D(x,y) = 255 − L(x,y)
Refined: L2 = L × L / 255;  L3 = L2 × L / 255  (progressively narrower)
Midtones: M = 255 − L − D
```

These masks are **completely self-feathering** — transitions in the mask exactly match tonal transitions in the image, producing seamless adjustments with no visible boundaries. Adobe integrated luminosity-based range masking into Lightroom/Camera Raw starting in 2017. Darktable's Tone Equalizer implements this as a graphic-equalizer interface with 9 EV bands, each filtered by a Gaussian window.

**Adaptive sharpening based on local tone** is the key to maintaining perceived sharpness during tonal adjustments. The implementation strategy:

1. **Sharpen in luminance only**: Convert to Lab (or YCbCr) and apply unsharp masking exclusively to the L (or Y) channel. This eliminates color fringing at high-contrast edges. The formula: sharpened = original + Amount × (original − blur(original)). For L-channel sharpening, use lower threshold values (~0–3) than RGB sharpening because color noise isn't a factor.

2. **Use luminosity masks to vary sharpening by tonal region**: Apply moderate sharpening to midtones, gentler sharpening to highlights (avoiding clipping), and either stronger sharpening or no sharpening to shadows depending on noise levels. CLAHE (Contrast Limited Adaptive Histogram Equalization) works well for shadows — it applies local histogram equalization in tiles with a clip limit preventing noise amplification.

3. **Sharpen before major tonal adjustments when possible**: Sharpening a correctly-exposed image produces cleaner results than sharpening lifted shadows. If tonal adjustment must come first, increase the unsharp mask Threshold parameter to ~10–15 for shadow-heavy regions to prevent noise sharpening.

4. **Apply noise reduction and sharpening as complementary operations**: Denoise the luminance channel in shadow regions (using luminosity masks to restrict the effect), then sharpen midtones and highlights. The two operations should target complementary tonal ranges.

---

## Practical Android implementation paths

For an Android developer, the implementation choice spans a spectrum from simple per-pixel shaders to full multi-scale decomposition.

**GPUImage for Android** (github.com/cats-oss/android-gpuimage) includes a `GPUImageHighlightShadowFilter` with this GLSL shader:

```glsl
float luminance = dot(source.rgb, vec3(0.3, 0.3, 0.3));
float shadow = clamp((pow(luminance, 1.0/(shadows+1.0)) +
    (-0.76)*pow(luminance, 2.0/(shadows+1.0))) - luminance, 0.0, 1.0);
float highlight = clamp((1.0 - (pow(1.0-luminance, 1.0/(2.0-highlights)) +
    (-0.8)*pow(1.0-luminance, 2.0/(2.0-highlights)))) - luminance, -1.0, 0.0);
vec3 result = ((luminance + shadow + highlight) / luminance) * source.rgb;
```

This is a **per-pixel approach with no edge awareness** — it will produce acceptable results for gentle adjustments but lacks the spatial intelligence of professional tools. Users on GitHub have noted it doesn't match Photoshop quality.

**For professional-quality results**, implement a guided-filter-based decomposition pipeline:

1. Decode JPEG to floating-point RGB (or 16-bit integer)
2. Compute luminance: Y = 0.2126R + 0.7152G + 0.0722B
3. Apply guided filter to Y with radius ~2% of image diagonal and ε tuning edge sensitivity → base layer
4. Compute detail = Y − base
5. Apply tone curve to base layer (shadow lifting, highlight compression) using luminosity-weighted remapping
6. Reconstruct: Y_new = modified_base + detail
7. Scale RGB: output = input × (Y_new / Y)

OpenCV on Android provides `cv::ximgproc::guidedFilter` for the filtering step. For GPU acceleration, the domain transform filter's recursive formulation maps efficiently to compute shaders and runs at ~7ms per megapixel.

**MetalPetal's HighlightShadow** (github.com/MetalPetal/HighlightShadow) closely follows the GEGL/darktable implementation with edge-aware blur — a good reference for porting to Android's Vulkan compute or OpenGL ES compute shaders.

For the highest quality, implement **Local Laplacian filters** using the fast approximation from Aubry et al. (2014). Reference implementations exist in C++ (github.com/psalvaggio/local_laplacian_filters) and darktable's source (src/common/locallaplacian.c). The algorithm's structure — independent per-pixel remapping followed by standard pyramid operations — parallelizes well on GPU.

---

## What makes edits look unnatural and how algorithms prevent it

Four artifacts make shadow/highlight editing look artificial, each with specific algorithmic causes and solutions.

**Halos** appear around high-contrast edges when the base-detail decomposition bleeds across edges. If a Gaussian blur is used for decomposition, the blurred base layer contains edge transitions smeared across many pixels. The detail residual then contains "ringing" — overshoot and undershoot signals that produce bright halos around dark objects when detail is recombined with a modified base. Solution: use edge-preserving filters (guided filter eliminates gradient reversals; WLS provides global optimization maintaining edge positions; Local Laplacian filters separate edges from details by construction). Keep Photoshop's Radius between 100–200px and Tonal Width conservative.

**Flat tonality** (the "HDR look") results from compressing the luminance hierarchy that conveys depth and three-dimensionality. When highlights are pulled down and shadows pushed up symmetrically, midtone contrast collapses. Every detail becomes equally visible at all tone levels — the opposite of how natural scenes appear, where shadows contain less visible detail than midtones. Solution: preserve midtone contrast by using S-curve remapping in the Local Laplacian framework (the α parameter) simultaneously with tonal compression. Darktable's Tone Equalizer allows independent control of each tonal band, enabling shadow lifting without midtone flattening.

**Color shifts** occur because RGB tone curves affect each channel differently depending on image content. Lifting shadows in sRGB space often desaturates colors and can shift warm shadows toward magenta. Processing in **non-perceptually-uniform spaces** (linear RGB, sRGB) produces different shifts than perceptually uniform spaces (Lab, OKLAB). Solution: separate luminance from chrominance before tonal adjustment. Work in Lab space (adjust L only) or apply RGB adjustments with Luminosity blend mode. Compensate saturation shifts with per-zone saturation controls, as implemented in Photoshop's Shadow/Highlight dialog.

**Noise amplification** in lifted shadows is the most challenging JPEG-specific problem. JPEG quantization noise has structured, blocky patterns unlike natural sensor noise's random grain. When amplified by shadow lifting, these patterns are immediately recognizable as artifacts. Solution: the pipeline should be deblock → denoise (luminance only, using luminosity mask to restrict to shadows) → tone adjust → add subtle film grain (to mask residual banding) → sharpen (midtones/highlights only, luminance channel).

---

## Essential academic papers and their practical relevance

The field evolved through four generations, each solving limitations of the previous:

**Generation 1 — Global tone mapping** (2002): Reinhard et al.'s "Photographic Tone Reproduction" introduced L_d = L(1 + L/L²_white)/(1 + L), normalizing by log-average luminance. Simple but limited to global adjustments.

**Generation 2 — Base-detail decomposition** (2002–2006): Durand & Dorsey's bilateral filter decomposition of log-luminance into base + detail layers, compressing only the base. Fattal et al.'s gradient-domain compression. These introduced spatial awareness but struggled with halos at extreme settings.

**Generation 3 — Multi-scale edge-aware decomposition** (2008–2011): Farbman et al.'s WLS filter enabled clean progressive coarsening impossible with bilateral filters. He et al.'s guided filter provided O(N) exact computation with no gradient reversals. Gastal & Oliveira's domain transform achieved real-time performance. Paris et al.'s Local Laplacian filters unified pyramid-based processing with edge awareness — the breakthrough that **directly powered Lightroom 4's shadows/highlights/clarity controls** (confirmed by Hasinoff and Adobe's blog post "Magic or Local Laplacian Filters?" from February 2012).

**Generation 4 — Learning-based approaches** (2016–present): Hasinoff et al.'s HDR+ burst pipeline (SIGGRAPH Asia 2016) demonstrated how to acquire clean, high-bit-depth data from mobile cameras. Gharbi et al.'s Deep Bilateral Learning (HDRNet, SIGGRAPH 2017) uses neural networks to learn bilateral grid representations for real-time enhancement. Google's HDR photo editing with ML predicts gain maps for HDR images.

The fundamental principle connecting all generations: **decompose an image into smooth base and textured detail layers using edge-preserving methods, modify the base to adjust shadows and highlights, and recombine — preserving texture detail and avoiding halos.** Every professional tool implements a variation of this pipeline. The choice of edge-preserving filter (bilateral vs. guided vs. WLS vs. Local Laplacian) determines the quality/speed tradeoff, while the remapping function applied to the base layer determines the character of the tonal adjustment.

---

## Conclusion

For an Android developer implementing highlight/shadow editing on JPEG data, the optimal strategy depends on your quality/performance requirements. A **guided filter decomposition** offers the best balance: O(N) complexity, no gradient reversals, available in OpenCV for Android, and the same approach used by darktable and RawTherapee in production. For maximum quality, implement **Local Laplacian filters** using Aubry et al.'s fast approximation — the same algorithm powering Lightroom.

Three non-obvious technical decisions will most impact your results. First, **always convert to at least 16-bit floating-point immediately after JPEG decode** — processing in 8-bit accumulates quantization errors catastrophically. Second, **separate luminance from chrominance** before any tonal manipulation; work in Lab or apply adjustments via the luminance channel of YCbCr, then reconstruct. Third, **add controlled noise (film grain) to shadow regions after tonal adjustment** — this masks the banding that is mathematically inevitable when stretching 8-bit shadow data, exploiting the same perceptual principle as sensor noise acting as natural dithering. The difference between amateur and professional-looking shadow recovery often comes down to these implementation details rather than the choice of primary algorithm.