package com.bcd.technotes.data.service

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.Half
import androidx.annotation.RequiresApi
import com.bcd.technotes.core.model.CollageLayout
import com.bcd.technotes.core.model.PhotoAdjustments
import com.bcd.technotes.core.processing.GuidedFilter
import com.bcd.technotes.core.processing.extractLuminance
import com.bcd.technotes.core.processing.srgbToLinear
import com.bcd.technotes.data.model.CollageResult
import com.bcd.technotes.data.model.ImageEditState
import com.bcd.technotes.data.model.PhotoTransform
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHighlightShadowFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.max

class BitmapService @Inject constructor(
    private val application: Application
) {

    suspend fun decodeBitmap(uri: Uri): Bitmap? {
        val displayHeight = application.resources.displayMetrics.heightPixels
        val targetMaxHeight = displayHeight * 2
        return withContext(Dispatchers.IO) {
            try {
                application.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, opts)
                    opts
                }?.let { opts ->
                    val sampleSize = calculateInSampleSize(opts.outHeight, targetMaxHeight)
                    application.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(
                            stream, null,
                            BitmapFactory.Options().apply { inSampleSize = sampleSize }
                        )
                    }
                }
            } catch (_: Exception) { null }
        }
    }

    fun cropBitmap(bitmap: Bitmap, normalizedRect: RectF): Bitmap {
        val x = (normalizedRect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (normalizedRect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val w = ((normalizedRect.right - normalizedRect.left) * bitmap.width).toInt()
            .coerceAtMost(bitmap.width - x).coerceAtLeast(1)
        val h = ((normalizedRect.bottom - normalizedRect.top) * bitmap.height).toInt()
            .coerceAtMost(bitmap.height - y).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }

    fun flipBitmap(bitmap: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix()
        if (horizontal) matrix.postScale(-1f, 1f) else matrix.postScale(1f, -1f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun applyTransform(bitmap: Bitmap, transform: PhotoTransform): Bitmap {
        if (!transform.flipH && !transform.flipV && transform.rotation == 0 && transform.cropRect == null) return bitmap

        var current = bitmap

        if (transform.flipH || transform.flipV || transform.rotation != 0) {
            val matrix = Matrix()
            if (transform.flipH) matrix.postScale(-1f, 1f)
            if (transform.flipV) matrix.postScale(1f, -1f)
            matrix.postRotate(transform.rotation.toFloat())
            current = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
        }

        if (transform.cropRect != null) {
            val cropped = cropBitmap(current, transform.cropRect)
            if (current !== bitmap) current.recycle()
            current = cropped
        }

        return current
    }

    suspend fun stitchCollage(
        bitmaps: List<Bitmap>,
        transforms: List<PhotoTransform>,
        layout: CollageLayout,
        canvasWidth: Int
    ): CollageResult? = withContext(Dispatchers.IO) {
        if (bitmaps.size < 2) return@withContext null

        val transformed = bitmaps.mapIndexed { i, bmp ->
            applyTransform(bmp, transforms.getOrElse(i) { PhotoTransform() })
        }

        if (layout.aspectRatio == 0f) {
            stitchHorizontalRow(bitmaps, transformed)
        } else {
            stitchGrid(bitmaps, transformed, transforms, layout, canvasWidth)
        }
    }

    private fun stitchHorizontalRow(
        originals: List<Bitmap>,
        transformed: List<Bitmap>
    ): CollageResult {
        val uniformHeight = transformed.minOf { it.height }
        val scaledWidths = transformed.map { bmp ->
            (bmp.width * (uniformHeight.toFloat() / bmp.height)).toInt()
        }
        val totalWidth = scaledWidths.sum()

        val result = Bitmap.createBitmap(totalWidth, uniformHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        var x = 0f
        transformed.forEachIndexed { i, bmp ->
            val scale = uniformHeight.toFloat() / bmp.height
            val matrix = Matrix().apply {
                postScale(scale, scale)
                postTranslate(x, 0f)
            }
            canvas.drawBitmap(bmp, matrix, null)
            x += scaledWidths[i]
        }

        transformed.forEachIndexed { i, tbmp ->
            if (tbmp !== originals[i]) tbmp.recycle()
        }

        val bounds = mutableListOf<RectF>()
        var bx = 0f
        scaledWidths.forEach { w ->
            bounds.add(RectF(bx, 0f, bx + w, uniformHeight.toFloat()))
            bx += w
        }
        val fullVisible = bounds.map { RectF(0f, 0f, 1f, 1f) }
        return CollageResult(result, bounds, fullVisible)
    }

    private fun stitchGrid(
        originals: List<Bitmap>,
        transformed: List<Bitmap>,
        transforms: List<PhotoTransform>,
        layout: CollageLayout,
        canvasWidth: Int
    ): CollageResult {
        val cw = canvasWidth
        val ch = (cw / layout.aspectRatio).toInt()
        val result = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val bounds = mutableListOf<RectF>()
        val visibleRects = mutableListOf<RectF>()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        layout.cells.forEachIndexed { i, cell ->
            val bmp = transformed.getOrNull(i) ?: return@forEachIndexed
            val transform = transforms.getOrElse(i) { PhotoTransform() }

            val cellLeft = (cell.left * cw).toInt()
            val cellTop = (cell.top * ch).toInt()
            val cellRight = (cell.right * cw).toInt()
            val cellBottom = (cell.bottom * ch).toInt()
            val cellW = cellRight - cellLeft
            val cellH = cellBottom - cellTop

            val scale = max(cellW.toFloat() / bmp.width, cellH.toFloat() / bmp.height)
            val scaledW = (bmp.width * scale).toInt()
            val scaledH = (bmp.height * scale).toInt()
            val overflowX = scaledW - cellW
            val overflowY = scaledH - cellH
            val offsetX = (transform.panX * overflowX).toInt()
            val offsetY = (transform.panY * overflowY).toInt()

            val srcLeft = (offsetX / scale).toInt().coerceIn(0, bmp.width - 1)
            val srcTop = (offsetY / scale).toInt().coerceIn(0, bmp.height - 1)
            val srcRight = (srcLeft + cellW / scale).toInt().coerceAtMost(bmp.width)
            val srcBottom = (srcTop + cellH / scale).toInt().coerceAtMost(bmp.height)

            canvas.drawBitmap(
                bmp,
                Rect(srcLeft, srcTop, srcRight, srcBottom),
                Rect(cellLeft, cellTop, cellRight, cellBottom),
                paint
            )
            bounds.add(RectF(cellLeft.toFloat(), cellTop.toFloat(), cellRight.toFloat(), cellBottom.toFloat()))

            if (overflowX == 0 && overflowY == 0) {
                visibleRects.add(RectF(0f, 0f, 1f, 1f))
            } else {
                visibleRects.add(RectF(
                    offsetX.toFloat() / scaledW,
                    offsetY.toFloat() / scaledH,
                    (offsetX + cellW).toFloat() / scaledW,
                    (offsetY + cellH).toFloat() / scaledH
                ))
            }
        }

        transformed.forEachIndexed { i, tbmp ->
            if (tbmp !== originals[i]) tbmp.recycle()
        }

        return CollageResult(result, bounds, visibleRects)
    }

    fun applyEditState(source: Bitmap, state: ImageEditState): Bitmap {
        var current = source

        if (state.cropRect != null) {
            current = cropBitmap(current, state.cropRect)
        }

        if (state.flipH || state.flipV || state.rotation != 0) {
            val matrix = Matrix()
            if (state.flipH) matrix.postScale(1f, -1f, current.width / 2f, current.height / 2f)
            if (state.flipV) matrix.postScale(-1f, 1f, current.width / 2f, current.height / 2f)
            if (state.rotation != 0) matrix.postRotate(state.rotation.toFloat(), current.width / 2f, current.height / 2f)
            current = Bitmap.createBitmap(current, 0, 0, current.width, current.height, matrix, true)
        }

        return current
    }

    private fun calculateInSampleSize(actualHeight: Int, targetHeight: Int): Int {
        var sampleSize = 1
        while (actualHeight / (sampleSize * 2) >= targetHeight) {
            sampleSize *= 2
        }
        return sampleSize
    }

    // --- Adjustment engine ---

    private val gpuImage: GPUImage by lazy { GPUImage(application) }

    private val dummyBaseDetailBitmap: Bitmap by lazy {
        Bitmap.createBitmap(1, 1, Bitmap.Config.RGBA_F16)
    }

    fun computeBaseDetailTexture(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val n = w * h

        val pixels = IntArray(n)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val luminance = FloatArray(n)
        for (i in 0 until n) {
            val pixel = pixels[i]
            val r = srgbToLinear(((pixel shr 16) and 0xFF) / 255f)
            val g = srgbToLinear(((pixel shr 8) and 0xFF) / 255f)
            val b = srgbToLinear((pixel and 0xFF) / 255f)
            luminance[i] = extractLuminance(r, g, b)
        }

        val radius = (maxOf(w, h) * 0.02f).toInt().coerceIn(5, 200)
        val base = GuidedFilter(w, h).filter(luminance, radius, 0.001f)

        val buffer = ByteBuffer.allocateDirect(n * 8).order(ByteOrder.nativeOrder())
        val shortBuffer = buffer.asShortBuffer()
        for (i in 0 until n) {
            val detail = luminance[i] - base[i]
            shortBuffer.put(Half.toHalf(base[i]))
            shortBuffer.put(Half.toHalf(detail))
            shortBuffer.put(Half.toHalf(luminance[i]))
            shortBuffer.put(Half.toHalf(1.0f))
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.RGBA_F16)
        buffer.rewind()
        result.copyPixelsFromBuffer(buffer)
        return result
    }

    fun applyAdjustments(bitmap: Bitmap, adj: PhotoAdjustments, baseDetail: Bitmap?): Bitmap {
        return if (Build.VERSION.SDK_INT >= 33) {
            applyAgslAdjustments(bitmap, adj, baseDetail)
        } else {
            applyGpuFallback(bitmap, adj)
        }
    }

    @RequiresApi(33)
    private fun applyAgslAdjustments(bitmap: Bitmap, adj: PhotoAdjustments, baseDetail: Bitmap?): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val shader = RuntimeShader(AGSL_ADJUSTMENT_SHADER)

        shader.setInputShader("inputImage", BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        shader.setFloatUniform("brightness", adj.brightness / 100f)
        shader.setFloatUniform("contrast", adj.contrast / 100f)
        shader.setFloatUniform("saturation", adj.saturation / 100f)
        shader.setFloatUniform("warmth", adj.warmth / 100f)
        shader.setFloatUniform("tint", adj.tint / 100f)
        shader.setFloatUniform("highlights", adj.highlights / 100f)
        shader.setFloatUniform("shadows", adj.shadows / 100f)
        shader.setFloatUniform("sharpness", adj.sharpness / 100f)

        val bdBitmap = baseDetail ?: dummyBaseDetailBitmap
        shader.setInputShader("baseDetailTex", BitmapShader(bdBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        shader.setFloatUniform("useBaseDetail", if (baseDetail != null) 1.0f else 0.0f)

        val paint = Paint().apply { this.shader = shader }
        val picture = android.graphics.Picture()
        val canvas = picture.beginRecording(w, h)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        picture.endRecording()

        return Bitmap.createBitmap(picture, w, h, Bitmap.Config.HARDWARE)
            .copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun applyGpuFallback(bitmap: Bitmap, adj: PhotoAdjustments): Bitmap {
        val group = GPUImageFilterGroup()

        if (adj.brightness != 0f) {
            group.addFilter(GPUImageBrightnessFilter(adj.brightness / 100f))
        }
        if (adj.contrast != 0f) {
            val c = 1f + adj.contrast / 100f * 1.5f
            group.addFilter(GPUImageContrastFilter(c.coerceIn(0.25f, 4f)))
        }
        if (adj.saturation != 0f) {
            val s = 1f + adj.saturation / 100f
            group.addFilter(GPUImageSaturationFilter(s.coerceAtLeast(0f)))
        }
        if (adj.warmth != 0f) {
            val temp = 5000f + adj.warmth / 100f * 3000f
            group.addFilter(GPUImageWhiteBalanceFilter(temp, 0f))
        }
        if (adj.tint != 0f) {
            group.addFilter(GPUImageWhiteBalanceFilter(5000f, adj.tint))
        }
        if (adj.highlights != 0f || adj.shadows != 0f) {
            val h = 1f - (adj.highlights / 100f * 0.5f)
            val s = (adj.shadows / 100f + 1f) * 0.5f
            group.addFilter(GPUImageHighlightShadowFilter(s.coerceIn(0f, 1f), h.coerceIn(0f, 1f)))
        }
        if (adj.sharpness != 0f) {
            group.addFilter(GPUImageSharpenFilter(adj.sharpness / 100f * 2f))
        }
        val filters = group.filters
        if (filters.isNullOrEmpty()) return bitmap
        val filter = if (filters.size == 1) filters[0] else group
        gpuImage.setFilter(filter)
        return gpuImage.getBitmapWithFilterApplied(bitmap, false)
    }

    companion object {
        const val AGSL_ADJUSTMENT_SHADER = """
            uniform shader inputImage;
            uniform shader baseDetailTex;
            uniform float useBaseDetail;
            uniform float brightness;
            uniform float contrast;
            uniform float saturation;
            uniform float warmth;
            uniform float tint;
            uniform float highlights;
            uniform float shadows;
            uniform float sharpness;

            const half3 LUM = half3(0.2126, 0.7152, 0.0722);
            const half HCV_EPS = 1e-10;

            half srgbToLin(half v) {
                return v <= 0.04045 ? v / 12.92 : pow((v + 0.055) / 1.055, 2.4);
            }
            half linToSrgb(half v) {
                return v <= 0.0031308 ? v * 12.92 : 1.055 * pow(v, 1.0/2.4) - 0.055;
            }
            half3 toLinear(half3 s) { return half3(srgbToLin(s.r), srgbToLin(s.g), srgbToLin(s.b)); }
            half3 toSrgb(half3 l)  { return half3(linToSrgb(l.r), linToSrgb(l.g), linToSrgb(l.b)); }

            const half LC_A = 5.555556;
            const half LC_B = 0.047996;
            const half LC_C = 0.244161;
            const half LC_D = 0.386036;
            const half LC_LOG10 = 0.4342944819;
            const half LC_MID = 0.391;

            half toLogC(half v) { return LC_C * log(LC_A * max(v, 1e-6) + LC_B) * LC_LOG10 + LC_D; }
            half fromLogC(half v) { return (pow(10.0, (v - LC_D) / LC_C) - LC_B) / LC_A; }

            half3 hue_to_rgb(half hue) {
                half R = abs(hue * 6.0 - 3.0) - 1.0;
                half G = 2.0 - abs(hue * 6.0 - 2.0);
                half B = 2.0 - abs(hue * 6.0 - 4.0);
                return clamp(half3(R, G, B), half3(0.0), half3(1.0));
            }
            half3 rgb_to_hcv(half3 rgb) {
                half4 P = (rgb.g < rgb.b) ? half4(rgb.bg, -1.0, 2.0/3.0) : half4(rgb.gb, 0.0, -1.0/3.0);
                half4 Q = (rgb.r < P.x) ? half4(P.xyw, rgb.r) : half4(rgb.r, P.yzx);
                half C = Q.x - min(Q.w, Q.y);
                half H = abs((Q.w - Q.y) / (6.0 * C + HCV_EPS) + Q.z);
                return half3(H, C, Q.x);
            }
            half3 rgb_to_hsl(half3 rgb) {
                half3 HCV = rgb_to_hcv(rgb);
                half L = HCV.z - HCV.y * 0.5;
                half S = HCV.y / (1.0 - abs(L * 2.0 - 1.0) + HCV_EPS);
                return half3(HCV.x, S, L);
            }
            half3 hsl_to_rgb(half3 hsl) {
                half3 rgb = hue_to_rgb(hsl.x);
                half C = (1.0 - abs(2.0 * hsl.z - 1.0)) * hsl.y;
                return (rgb - 0.5) * C + hsl.z;
            }

            half4 main(float2 fragCoord) {
                half4 pixel = inputImage.eval(fragCoord);
                if (pixel.a < 0.01) return pixel;

                half3 color = pixel.rgb;
                color = toLinear(color);

                if (highlights != 0.0 || shadows != 0.0) {
                    if (useBaseDetail > 0.5) {
                        half4 bd = baseDetailTex.eval(fragCoord);
                        half base = bd.r;
                        half detail = bd.g;
                        half origLum = bd.b;

                        half remappedBase = base;
                        if (shadows != 0.0) {
                            half shadowMask = 1.0 - smoothstep(0.15, 0.45, base);
                            half gamma = 1.0 - shadows * 1.0;
                            half shifted = pow(max(base, 1e-6), gamma) - base;
                            remappedBase += shifted * shadowMask;
                        }

                        if (highlights != 0.0) {
                            half highlightMask = smoothstep(0.3, 0.8, base);
                            if (highlights < 0.0) {
                                half strength = -highlights * 2.5;
                                half darkened = pow(max(base, 1e-6), 1.0 + strength);
                                remappedBase -= (base - darkened) * highlightMask;
                            } else {
                                half strength = highlights * 2.0;
                                half brightened = pow(max(base, 1e-6), 1.0 / (1.0 + strength));
                                remappedBase += (brightened - base) * highlightMask;
                            }
                        }

                        half newLum = max(remappedBase + detail, 0.0);
                        if (origLum > 1e-5) {
                            color *= newLum / origLum;
                        }
                    } else {
                        half lum = dot(color, LUM);
                        if (lum > 1e-5) {
                            half newLum = lum;
                            if (shadows != 0.0) {
                                half mask = 1.0 - smoothstep(0.15, 0.45, lum);
                                half s = abs(shadows);
                                half target = (shadows > 0.0)
                                    ? 1.0 - pow(1.0 - lum, 1.0 + s * 4.0)
                                    : pow(lum, 1.0 + s * 1.5);
                                newLum += (target - lum) * mask;
                            }
                            if (highlights != 0.0) {
                                half mask = smoothstep(0.15, 0.6, lum);
                                half h = abs(highlights);
                                half target = (highlights > 0.0)
                                    ? 1.0 - pow(1.0 - lum, 1.0 + h * 5.0)
                                    : pow(lum, 1.0 + h * 3.0);
                                newLum += (target - lum) * mask;
                            }
                            color *= max(newLum, 0.0) / lum;
                        }
                    }
                }

                if (brightness != 0.0) {
                    half ev = brightness * abs(brightness) * 2.0;
                    color *= half3(pow(2.0, ev));
                }

                if (warmth != 0.0 || tint != 0.0) {
                    half d65x = 0.31271;
                    half d65y = 0.32902;
                    half x = d65x - warmth * (warmth < 0.0 ? 0.0214 : 0.066);
                    half y = 2.87 * x - 3.0 * x * x - 0.275 + tint * 0.066;
                    half X = x / max(y, 1e-5);
                    half Z = (1.0 - x - y) / max(y, 1e-5);
                    half3 gain = half3(0.95047 / X, 1.0, 1.08883 / Z);
                    half3 xyz = half3(
                        0.4124564 * color.r + 0.3575761 * color.g + 0.1804375 * color.b,
                        0.2126729 * color.r + 0.7151522 * color.g + 0.0721750 * color.b,
                        0.0193339 * color.r + 0.1191920 * color.g + 0.9503041 * color.b
                    );
                    xyz *= gain;
                    color = half3(
                         3.2404542 * xyz.r - 1.5371385 * xyz.g - 0.4985314 * xyz.b,
                        -0.9692660 * xyz.r + 1.8760108 * xyz.g + 0.0415560 * xyz.b,
                         0.0556434 * xyz.r - 0.2040259 * xyz.g + 1.0572252 * xyz.b
                    );
                    color = max(color, half3(0.0));
                }

                if (contrast != 0.0) {
                    half3 logC = half3(toLogC(color.r), toLogC(color.g), toLogC(color.b));
                    half c = 1.0 + contrast * 0.5;
                    logC = half3(LC_MID) + c * (logC - half3(LC_MID));
                    color = half3(fromLogC(logC.r), fromLogC(logC.g), fromLogC(logC.b));
                    color = max(color, half3(0.0));
                }

                if (saturation != 0.0) {
                    half3 srgbCol = toSrgb(color);
                    half3 hsl = rgb_to_hsl(srgbCol);
                    hsl.y *= (1.0 + saturation);
                    hsl.y = clamp(hsl.y, 0.0, 1.0);
                    srgbCol = hsl_to_rgb(hsl);
                    color = toLinear(srgbCol);
                }

                color = toSrgb(color);

                if (sharpness > 0.0) {
                    half lumaC  = dot(inputImage.eval(fragCoord).rgb, LUM);
                    half lumaL  = dot(inputImage.eval(fragCoord + float2(-1,  0)).rgb, LUM);
                    half lumaR  = dot(inputImage.eval(fragCoord + float2( 1,  0)).rgb, LUM);
                    half lumaU  = dot(inputImage.eval(fragCoord + float2( 0, -1)).rgb, LUM);
                    half lumaD  = dot(inputImage.eval(fragCoord + float2( 0,  1)).rgb, LUM);
                    half lumaTL = dot(inputImage.eval(fragCoord + float2(-1, -1)).rgb, LUM);
                    half lumaTR = dot(inputImage.eval(fragCoord + float2( 1, -1)).rgb, LUM);
                    half lumaBL = dot(inputImage.eval(fragCoord + float2(-1,  1)).rgb, LUM);
                    half lumaBR = dot(inputImage.eval(fragCoord + float2( 1,  1)).rgb, LUM);

                    half lumaBlur = (lumaTL + 2.0*lumaU + lumaTR +
                                     2.0*lumaL + 4.0*lumaC + 2.0*lumaR +
                                     lumaBL + 2.0*lumaD + lumaBR) / 16.0;

                    half diff = lumaC - lumaBlur;
                    half absDiff = abs(diff);

                    half gate = smoothstep(0.002, 0.008, absDiff);
                    half shadowMask = smoothstep(0.0, 0.08, lumaC);
                    half highlightMask = 1.0 - smoothstep(0.9, 1.0, lumaC);

                    half sharp = diff * sharpness * 12.0 * gate * shadowMask * highlightMask;
                    sharp = clamp(sharp, -0.25, 0.25);
                    color += half3(sharp);
                }

                {
                    half lum = dot(color, LUM);
                    half grainMask = 1.0 - smoothstep(0.05, 0.25, lum);
                    if (grainMask > 0.0) {
                        float2 seed = fragCoord * 0.1 + float2(12.9898, 78.233);
                        half noise1 = fract(sin(dot(seed, float2(127.1, 311.7))) * 43758.5453) - 0.5;
                        half noise2 = fract(sin(dot(seed, float2(269.5, 183.3))) * 43758.5453) - 0.5;
                        half grain = (noise1 + noise2) * 0.012 * grainMask;
                        color += half3(grain);
                    }
                }

                return half4(clamp(color, half3(0.0), half3(1.0)), pixel.a);
            }
        """
    }
}
