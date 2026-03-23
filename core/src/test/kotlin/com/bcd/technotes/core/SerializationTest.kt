package com.bcd.technotes.core

import com.bcd.technotes.core.model.Annotation as AnnotationModel
import com.bcd.technotes.core.model.*
import com.bcd.technotes.core.serialization.toJson
import com.bcd.technotes.core.serialization.toProject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerializationTest {

    private fun createTestProject(): Project {
        val layerId = "layer-1"
        return Project(
            id = "proj-1",
            name = "Test Project",
            createdAt = 1711234567890L,
            modifiedAt = 1711234567890L,
            canvasWidth = 1920,
            canvasHeight = 1080,
            backgroundPhotos = listOf(
                PhotoReference(
                    id = "photo-1",
                    uri = "photos/photo-1.jpg",
                    rect = RectF(0f, 0f, 1920f, 1080f)
                )
            ),
            layers = listOf(
                Layer(id = layerId, name = "Annotations", order = 0, type = LayerType.ANNOTATION),
                Layer(id = "layer-2", name = "Measurements", order = 1, type = LayerType.MEASUREMENT)
            ),
            annotations = listOf(
                LineAnnotation(
                    id = "ann-1",
                    layerId = layerId,
                    start = PointF(10f, 20f),
                    end = PointF(100f, 200f),
                    hasArrowEnd = true
                ),
                RectangleAnnotation(
                    id = "ann-2",
                    layerId = layerId,
                    topLeft = PointF(50f, 50f),
                    size = SizeF(200f, 100f),
                    cornerRadius = 8f
                ),
                CircleAnnotation(
                    id = "ann-3",
                    layerId = layerId,
                    center = PointF(400f, 300f),
                    radius = 75f
                ),
                TextAnnotation(
                    id = "ann-4",
                    layerId = layerId,
                    position = PointF(500f, 100f),
                    content = "Test label",
                    fontSize = 16f
                ),
                DimensionLineAnnotation(
                    id = "ann-5",
                    layerId = "layer-2",
                    start = PointF(100f, 500f),
                    end = PointF(400f, 500f),
                    displayValue = "3.00 m",
                    unit = MeasurementUnit.M
                )
            ),
            scaleCalibration = ScaleCalibration(
                pixelDistance = 300f,
                realDistance = 3f,
                unit = MeasurementUnit.M
            )
        )
    }

    @Test
    fun `project round-trip serialization preserves all data`() {
        val original = createTestProject()
        val json = original.toJson()
        val restored = json.toProject()
        assertEquals(original, restored)
    }

    @Test
    fun `serialized json contains type discriminator for annotations`() {
        val project = createTestProject()
        val json = project.toJson()
        assertTrue(json.contains("\"type\": \"line\""), "Should contain line type discriminator")
        assertTrue(json.contains("\"type\": \"rectangle\""), "Should contain rectangle type discriminator")
        assertTrue(json.contains("\"type\": \"circle\""), "Should contain circle type discriminator")
        assertTrue(json.contains("\"type\": \"text\""), "Should contain text type discriminator")
        assertTrue(json.contains("\"type\": \"dimension_line\""), "Should contain dimension_line type discriminator")
    }

    @Test
    fun `all annotation types serialize and deserialize correctly`() {
        val layerId = "layer-1"
        val allAnnotations: List<AnnotationModel> = listOf(
            LineAnnotation(id = "1", layerId = layerId, start = PointF(0f, 0f), end = PointF(1f, 1f)),
            RectangleAnnotation(id = "2", layerId = layerId, topLeft = PointF(0f, 0f), size = SizeF(10f, 10f)),
            CircleAnnotation(id = "3", layerId = layerId, center = PointF(5f, 5f), radius = 5f),
            EllipseAnnotation(id = "4", layerId = layerId, center = PointF(5f, 5f), radiusX = 10f, radiusY = 5f),
            PolygonAnnotation(id = "5", layerId = layerId, points = listOf(PointF(0f, 0f), PointF(10f, 0f), PointF(5f, 10f))),
            FreehandAnnotation(id = "6", layerId = layerId, points = listOf(PointF(0f, 0f), PointF(1f, 1f), PointF(2f, 0f))),
            TextAnnotation(id = "7", layerId = layerId, position = PointF(0f, 0f), content = "Hello"),
            CalloutAnnotation(
                id = "8", layerId = layerId,
                anchorPoint = PointF(10f, 10f),
                textBoxRect = RectF(50f, 50f, 200f, 100f),
                content = "Note"
            ),
            NumberedMarkerAnnotation(id = "9", layerId = layerId, position = PointF(0f, 0f), number = 1),
            DimensionLineAnnotation(id = "10", layerId = layerId, start = PointF(0f, 0f), end = PointF(100f, 0f)),
            AngleAnnotation(
                id = "11", layerId = layerId,
                vertex = PointF(50f, 50f),
                armA = PointF(100f, 50f),
                armB = PointF(50f, 0f)
            ),
            AreaAnnotation(
                id = "12", layerId = layerId,
                points = listOf(PointF(0f, 0f), PointF(100f, 0f), PointF(100f, 100f), PointF(0f, 100f))
            ),
            SymbolAnnotation(
                id = "13", layerId = layerId,
                position = PointF(200f, 200f),
                symbolId = "electrical-outlet",
                category = SymbolCategory.ELECTRICAL
            ),
            ImageOverlayAnnotation(
                id = "14", layerId = layerId,
                imageUri = "photos/overlay.jpg",
                rect = RectF(0f, 0f, 500f, 500f),
                overlayOpacity = 0.5f
            )
        )

        val project = Project(
            id = "test",
            name = "All Types Test",
            createdAt = 0L,
            modifiedAt = 0L,
            canvasWidth = 1000,
            canvasHeight = 1000,
            layers = listOf(Layer(id = layerId, name = "Test", order = 0)),
            annotations = allAnnotations
        )

        val json = project.toJson()
        val restored = json.toProject()

        assertEquals(allAnnotations.size, restored.annotations.size)
        allAnnotations.forEachIndexed { index, annotation ->
            assertEquals(annotation, restored.annotations[index], "Annotation at index $index should match")
        }
    }

    @Test
    fun `style with custom values round-trips correctly`() {
        val style = AnnotationStyle(
            strokeColor = 0xFFFF0000,
            fillColor = 0x8000FF00,
            strokeWidth = 3.5f,
            strokeCap = StrokeCap.SQUARE,
            strokeJoin = StrokeJoin.MITER,
            dashPattern = listOf(10f, 5f, 2f, 5f),
            opacity = 0.75f
        )

        val annotation = LineAnnotation(
            id = "styled",
            layerId = "layer-1",
            style = style,
            start = PointF(0f, 0f),
            end = PointF(100f, 100f)
        )

        val project = Project(
            id = "test",
            name = "Style Test",
            createdAt = 0L,
            modifiedAt = 0L,
            canvasWidth = 100,
            canvasHeight = 100,
            layers = listOf(Layer(id = "layer-1", name = "Test", order = 0)),
            annotations = listOf(annotation)
        )

        val restored = project.toJson().toProject()
        val restoredAnnotation = restored.annotations.first() as LineAnnotation
        assertEquals(style, restoredAnnotation.style)
    }

    @Test
    fun `transform round-trips correctly`() {
        val transform = AnnotationTransform(
            rotation = 45f,
            scaleX = 1.5f,
            scaleY = 0.8f,
            translateX = 10f,
            translateY = -20f
        )

        val annotation = RectangleAnnotation(
            id = "transformed",
            layerId = "layer-1",
            transform = transform,
            topLeft = PointF(0f, 0f),
            size = SizeF(100f, 50f)
        )

        val project = Project(
            id = "test",
            name = "Transform Test",
            createdAt = 0L,
            modifiedAt = 0L,
            canvasWidth = 100,
            canvasHeight = 100,
            layers = listOf(Layer(id = "layer-1", name = "Test", order = 0)),
            annotations = listOf(annotation)
        )

        val restored = project.toJson().toProject()
        val restoredAnnotation = restored.annotations.first() as RectangleAnnotation
        assertEquals(transform, restoredAnnotation.transform)
    }

    @Test
    fun `scale calibration round-trips and computes pixelsPerUnit`() {
        val calibration = ScaleCalibration(
            pixelDistance = 300f,
            realDistance = 3f,
            unit = MeasurementUnit.M
        )

        assertEquals(100f, calibration.pixelsPerUnit)

        val project = Project(
            id = "test",
            name = "Calibration Test",
            createdAt = 0L,
            modifiedAt = 0L,
            canvasWidth = 1000,
            canvasHeight = 1000,
            scaleCalibration = calibration
        )

        val restored = project.toJson().toProject()
        assertEquals(calibration, restored.scaleCalibration)
        assertEquals(100f, restored.scaleCalibration!!.pixelsPerUnit)
    }

    @Test
    fun `unknown keys in json are ignored for forward compatibility`() {
        val json = """
        {
            "version": 1,
            "id": "test",
            "name": "Forward Compat",
            "createdAt": 0,
            "modifiedAt": 0,
            "canvasWidth": 100,
            "canvasHeight": 100,
            "backgroundPhotos": [],
            "layers": [],
            "annotations": [],
            "scaleCalibration": null,
            "futureField": "some value",
            "anotherNewField": 42
        }
        """.trimIndent()

        val project = json.toProject()
        assertEquals("test", project.id)
        assertEquals("Forward Compat", project.name)
    }
}
