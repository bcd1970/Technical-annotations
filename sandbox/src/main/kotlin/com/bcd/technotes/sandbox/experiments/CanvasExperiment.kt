package com.bcd.technotes.sandbox.experiments

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CanvasExperiment() {
    val context = LocalContext.current

    var backgroundImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                backgroundImage = bitmap?.asImageBitmap()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Pick a Photo")
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.1f, 10f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    }
            ) {
                withTransform({
                    translate(left = offsetX, top = offsetY)
                    scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                }) {
                    // Draw background image
                    backgroundImage?.let { image ->
                        drawImage(
                            image = image,
                            dstSize = IntSize(image.width, image.height)
                        )
                    }

                    // Draw hardcoded test annotations
                    drawTestAnnotations()
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTestAnnotations() {
    // Line with arrow
    val lineStart = Offset(50f, 50f)
    val lineEnd = Offset(300f, 150f)
    drawLine(
        color = Color.Red,
        start = lineStart,
        end = lineEnd,
        strokeWidth = 3f
    )
    drawArrowHead(lineStart, lineEnd, Color.Red)

    // Rectangle
    drawRect(
        color = Color.Blue,
        topLeft = Offset(350f, 50f),
        size = Size(200f, 120f),
        style = Stroke(width = 3f)
    )

    // Circle
    drawCircle(
        color = Color(0xFF4CAF50),
        center = Offset(200f, 350f),
        radius = 80f,
        style = Stroke(width = 3f)
    )

    // Filled semi-transparent rectangle
    drawRect(
        color = Color(0x40FF9800),
        topLeft = Offset(400f, 250f),
        size = Size(150f, 100f)
    )
    drawRect(
        color = Color(0xFFFF9800),
        topLeft = Offset(400f, 250f),
        size = Size(150f, 100f),
        style = Stroke(width = 2f)
    )

    // Polygon (triangle)
    val trianglePath = Path().apply {
        moveTo(100f, 500f)
        lineTo(250f, 500f)
        lineTo(175f, 400f)
        close()
    }
    drawPath(
        path = trianglePath,
        color = Color(0xFF9C27B0),
        style = Stroke(width = 3f)
    )

    // Dashed line (dimension line simulation)
    val dashPath = Path().apply {
        moveTo(50f, 600f)
        lineTo(500f, 600f)
    }
    drawPath(
        path = dashPath,
        color = Color(0xFF607D8B),
        style = Stroke(
            width = 2f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(15f, 8f),
                0f
            )
        )
    )
    // Extension lines
    drawLine(Color(0xFF607D8B), Offset(50f, 580f), Offset(50f, 620f), strokeWidth = 1.5f)
    drawLine(Color(0xFF607D8B), Offset(500f, 580f), Offset(500f, 620f), strokeWidth = 1.5f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(
    from: Offset,
    to: Offset,
    color: Color,
    arrowLength: Float = 20f,
    arrowAngle: Float = 25f
) {
    val angle = kotlin.math.atan2(
        (to.y - from.y).toDouble(),
        (to.x - from.x).toDouble()
    )
    val rad = arrowAngle * PI / 180.0

    val x1 = to.x - arrowLength * cos(angle - rad).toFloat()
    val y1 = to.y - arrowLength * sin(angle - rad).toFloat()
    val x2 = to.x - arrowLength * cos(angle + rad).toFloat()
    val y2 = to.y - arrowLength * sin(angle + rad).toFloat()

    val arrowPath = Path().apply {
        moveTo(to.x, to.y)
        lineTo(x1, y1)
        lineTo(x2, y2)
        close()
    }
    drawPath(arrowPath, color)
}
