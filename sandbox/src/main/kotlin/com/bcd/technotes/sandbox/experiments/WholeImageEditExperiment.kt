package com.bcd.technotes.sandbox.experiments

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.Half
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bcd.technotes.core.model.AdjustmentType
import com.bcd.technotes.core.model.PhotoAdjustments
import com.bcd.technotes.core.model.getValue
import com.bcd.technotes.core.model.withValue
import com.bcd.technotes.core.processing.GuidedFilter
import com.bcd.technotes.core.processing.extractLuminance
import com.bcd.technotes.core.processing.srgbToLinear
import com.bcd.technotes.sandbox.util.WholeImageEditView
import com.bcd.technotes.sandbox.util.updateDoubleTapScale
import jp.co.cyberagent.android.gpuimage.GPUImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHighlightShadowFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

data class ImageEditState(
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val rotation: Int = 0,
    val cropRect: RectF? = null,
    val adjustments: PhotoAdjustments = PhotoAdjustments()
)

enum class EditTool { NONE, CROP, ADJUST }

@Composable
fun WholeImageEditScreen(
    sourceBitmap: Bitmap,
    onDone: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val gpuImage = remember { GPUImage(context) }

    var currentSource by remember { mutableStateOf(sourceBitmap) }
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var baseDetailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editState by remember { mutableStateOf(ImageEditState()) }
    var activeTool by remember { mutableStateOf(EditTool.NONE) }
    var editView by remember { mutableStateOf<WholeImageEditView?>(null) }

    BackHandler {
        when (activeTool) {
            EditTool.CROP -> {
                activeTool = EditTool.NONE
                editView?.cropMode = false
            }
            EditTool.ADJUST -> {
                activeTool = EditTool.NONE
                editView?.setRenderEffect(null)
            }
            EditTool.NONE -> onCancel()
        }
    }

    LaunchedEffect(currentSource, editState.flipH, editState.flipV, editState.rotation, editState.cropRect) {
        displayBitmap = withContext(Dispatchers.Default) {
            applyGeometricEdits(currentSource, editState)
        }
    }

    LaunchedEffect(displayBitmap) {
        val bmp = displayBitmap ?: return@LaunchedEffect
        editView?.let { view ->
            view.setImageBitmap(bmp)
            view.post { view.updateDoubleTapScale() }
        }
        baseDetailBitmap = null // invalidate stale decomposition
        baseDetailBitmap = withContext(Dispatchers.Default) {
            computeBaseDetailTexture(bmp)
        }
    }

    LaunchedEffect(editView, displayBitmap) {
        val view = editView ?: return@LaunchedEffect
        val bmp = displayBitmap ?: return@LaunchedEffect

        snapshotFlow { Pair(editState.adjustments, baseDetailBitmap) }
            .conflate()
            .collect { (adj, bd) ->
                if (adj.isDefault) {
                    view.setRenderEffect(null)
                    view.setImageBitmap(bmp)
                } else if (Build.VERSION.SDK_INT >= 33) {
                    val adjusted = withContext(Dispatchers.Default) {
                        applyAgslAdjustments(bmp, adj, bd)
                    }
                    view.setRenderEffect(null)
                    view.setImageBitmap(adjusted)
                } else {
                    val adjusted = withContext(Dispatchers.Default) {
                        applyGpuFallback(gpuImage, bmp, adj)
                    }
                    view.setImageBitmap(adjusted)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (displayBitmap != null) {
            AndroidView(
                factory = { ctx ->
                    WholeImageEditView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        maxZoom = 10f
                        minZoom = 1f
                        setImageBitmap(displayBitmap)
                        editView = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            )
        }

        displayBitmap?.let { bmp ->
            Text(
                text = "${bmp.width} \u00D7 ${bmp.height}",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .systemBarsPadding()
                    .padding(end = 12.dp, top = 4.dp)
                    .background(Color(0x66000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            when (activeTool) {
                EditTool.CROP -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xCC000000))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = {
                            activeTool = EditTool.NONE
                            editView?.cropMode = false
                        }) {
                            Icon(Icons.Default.Close, "Cancel crop", tint = Color.White)
                        }
                        IconButton(onClick = {
                            val view = editView ?: return@IconButton
                            val bmp = displayBitmap ?: return@IconButton
                            val rect = view.cropRect
                            if (!rect.isEmpty) {
                                val normRect = RectF(
                                    rect.left / bmp.width,
                                    rect.top / bmp.height,
                                    rect.right / bmp.width,
                                    rect.bottom / bmp.height
                                )
                                editState = editState.copy(cropRect = normRect)
                                currentSource = applyGeometricEdits(currentSource, editState)
                                editState = ImageEditState(adjustments = editState.adjustments)
                            }
                            activeTool = EditTool.NONE
                            view.cropMode = false
                        }) {
                            Icon(Icons.Default.Check, "Confirm crop", tint = Color.Green)
                        }
                    }
                }

                EditTool.ADJUST -> {
                    AdjustmentPanel(
                        adjustments = editState.adjustments,
                        onAdjustmentChange = { newAdj ->
                            editState = editState.copy(adjustments = newAdj)
                        },
                        onReset = {
                            editState = editState.copy(adjustments = PhotoAdjustments())
                        },
                        onClose = {
                            activeTool = EditTool.NONE
                        }
                    )
                }

                EditTool.NONE -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xCC000000))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { editState = editState.copy(flipV = !editState.flipV) }) {
                            Icon(Icons.Default.Flip, "Flip vertical", tint = Color.White)
                        }
                        IconButton(onClick = { editState = editState.copy(flipH = !editState.flipH) }) {
                            Icon(
                                Icons.Default.Flip, "Flip horizontal", tint = Color.White,
                                modifier = Modifier.graphicsLayer(rotationZ = 90f)
                            )
                        }
                        IconButton(onClick = { editState = editState.copy(rotation = (editState.rotation + 90) % 360) }) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate 90\u00B0", tint = Color.White)
                        }
                        IconButton(onClick = {
                            activeTool = EditTool.CROP
                            editView?.let {
                                it.cropMode = true
                                it.initCropRect()
                            }
                        }) {
                            Icon(Icons.Default.Crop, "Crop", tint = Color.White)
                        }
                        IconButton(onClick = {
                            activeTool = EditTool.ADJUST
                        }) {
                            Icon(Icons.Default.Tune, "Adjust", tint = Color.White)
                        }
                        IconButton(onClick = {
                            editView?.setRenderEffect(null)
                            val bmp = displayBitmap ?: currentSource
                            if (!editState.adjustments.isDefault) {
                                onDone(applyAdjustments(gpuImage, bmp, editState.adjustments, baseDetailBitmap))
                            } else {
                                onDone(bmp)
                            }
                        }) {
                            Icon(Icons.Default.Check, "Done", tint = Color.Green)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentPanel(
    adjustments: PhotoAdjustments,
    onAdjustmentChange: (PhotoAdjustments) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    var selectedType by remember { mutableStateOf(AdjustmentType.BRIGHTNESS) }
    val currentValue = adjustments.getValue(selectedType)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC000000))
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close adjustments", tint = Color.White)
            }
            Text(
                text = "${selectedType.label}: ${currentValue.toInt()}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onReset) {
                Text("Reset", color = Color(0xFF4FC3F7))
            }
        }

        Slider(
            value = currentValue,
            onValueChange = { newVal ->
                onAdjustmentChange(adjustments.withValue(selectedType, newVal))
            },
            valueRange = selectedType.min..selectedType.max,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF4FC3F7),
                inactiveTrackColor = Color(0x66FFFFFF)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (type in AdjustmentType.entries.filter { it != AdjustmentType.VIGNETTE }) {
                val value = adjustments.getValue(type)
                FilterChip(
                    selected = type == selectedType,
                    onClick = { selectedType = type },
                    label = {
                        Text(
                            text = if (value != 0f) "${type.label} ${value.toInt()}" else type.label,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4FC3F7),
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0x33FFFFFF),
                        labelColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

// --- Guided filter preprocessing ---

private val dummyBaseDetailBitmap: Bitmap by lazy {
    Bitmap.createBitmap(1, 1, Bitmap.Config.RGBA_F16)
}

private fun computeBaseDetailTexture(bitmap: Bitmap): Bitmap {
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

// --- Adjustment engine: AGSL (API 33+) with GPUImage fallback ---

private fun applyAdjustments(gpuImage: GPUImage, bitmap: Bitmap, adj: PhotoAdjustments, baseDetail: Bitmap?): Bitmap {
    return if (Build.VERSION.SDK_INT >= 33) {
        applyAgslAdjustments(bitmap, adj, baseDetail)
    } else {
        applyGpuFallback(gpuImage, bitmap, adj)
    }
}

private const val AGSL_ADJUSTMENT_SHADER = """
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

    // ARRI LogC (EI 1000) encode/decode for contrast
    const half LC_A = 5.555556;
    const half LC_B = 0.047996;
    const half LC_C = 0.244161;
    const half LC_D = 0.386036;
    const half LC_LOG10 = 0.4342944819;
    const half LC_MID = 0.391;

    half toLogC(half v) { return LC_C * log(LC_A * max(v, 1e-6) + LC_B) * LC_LOG10 + LC_D; }
    half fromLogC(half v) { return (pow(10.0, (v - LC_D) / LC_C) - LC_B) / LC_A; }

    // HSL helpers
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

        // 1. Highlights / Shadows — guided filter decomposition or per-pixel fallback
        if (highlights != 0.0 || shadows != 0.0) {
            if (useBaseDetail > 0.5) {
                // Professional path: edge-aware base/detail decomposition
                half4 bd = baseDetailTex.eval(fragCoord);
                half base = bd.r;
                half detail = bd.g;
                half origLum = bd.b;

                // Remap base layer — shadows
                half remappedBase = base;
                if (shadows != 0.0) {
                    half shadowMask = 1.0 - smoothstep(0.15, 0.45, base);
                    half gamma = 1.0 - shadows * 1.0;
                    half shifted = pow(max(base, 1e-6), gamma) - base;
                    remappedBase += shifted * shadowMask;
                }

                // Remap base layer — highlights
                if (highlights != 0.0) {
                    // Wide mask: active from linear ~0.15 upward (sRGB ~0.42+)
                    half highlightMask = smoothstep(0.3, 0.8, base);
                    if (highlights < 0.0) {
                        // Recovery: darken highlights via gamma > 1
                        half strength = -highlights * 2.5;
                        half darkened = pow(max(base, 1e-6), 1.0 + strength);
                        remappedBase -= (base - darkened) * highlightMask;
                    } else {
                        // Boost: brighten highlights via gamma < 1
                        half strength = highlights * 2.0;
                        half brightened = pow(max(base, 1e-6), 1.0 / (1.0 + strength));
                        remappedBase += (brightened - base) * highlightMask;
                    }
                }

                // Reconstruct luminance and scale RGB (chrominance preservation)
                half newLum = max(remappedBase + detail, 0.0);
                if (origLum > 1e-5) {
                    color *= newLum / origLum;
                }
            } else {
                // Fallback: per-pixel power curves (while guided filter computes)
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

        // 2. Brightness — quadratic EV curve
        if (brightness != 0.0) {
            half ev = brightness * abs(brightness) * 2.0;
            color *= half3(pow(2.0, ev));
        }

        // 3. White balance — CIE chromaticity shift + XYZ adaptation
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

        // 4. Contrast — in ARRI LogC space around middle gray
        if (contrast != 0.0) {
            half3 logC = half3(toLogC(color.r), toLogC(color.g), toLogC(color.b));
            half c = 1.0 + contrast * 0.5;
            logC = half3(LC_MID) + c * (logC - half3(LC_MID));
            color = half3(fromLogC(logC.r), fromLogC(logC.g), fromLogC(logC.b));
            color = max(color, half3(0.0));
        }

        // 5. Saturation — HSL-based, no hue shift
        if (saturation != 0.0) {
            half3 srgbCol = toSrgb(color);
            half3 hsl = rgb_to_hsl(srgbCol);
            hsl.y *= (1.0 + saturation);
            hsl.y = clamp(hsl.y, 0.0, 1.0);
            srgbCol = hsl_to_rgb(hsl);
            color = toLinear(srgbCol);
        }

        color = toSrgb(color);

        // 6. Sharpness — 9-tap Gaussian unsharp mask, tone-masked
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

            // Noise gate: only suppress very small differences
            half gate = smoothstep(0.002, 0.008, absDiff);
            // Shadow protection: reduce sharpening only in very dark regions
            half shadowMask = smoothstep(0.0, 0.08, lumaC);
            // Highlight protection: reduce near clipping
            half highlightMask = 1.0 - smoothstep(0.9, 1.0, lumaC);

            half sharp = diff * sharpness * 12.0 * gate * shadowMask * highlightMask;
            sharp = clamp(sharp, -0.25, 0.25);
            color += half3(sharp);
        }

        // 7. Film grain — shadow-only anti-banding dither
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

// --- GPUImage fallback (API 29-32) ---

private fun applyGpuFallback(gpuImage: GPUImage, bitmap: Bitmap, adj: PhotoAdjustments): Bitmap {
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

// --- Geometric edit state application ---

private fun applyGeometricEdits(source: Bitmap, state: ImageEditState): Bitmap {
    var current = source

    if (state.cropRect != null) {
        val cr = state.cropRect
        val x = (cr.left * current.width).toInt().coerceIn(0, current.width - 1)
        val y = (cr.top * current.height).toInt().coerceIn(0, current.height - 1)
        val w = ((cr.right - cr.left) * current.width).toInt().coerceAtMost(current.width - x).coerceAtLeast(1)
        val h = ((cr.bottom - cr.top) * current.height).toInt().coerceAtMost(current.height - y).coerceAtLeast(1)
        current = Bitmap.createBitmap(current, x, y, w, h)
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
