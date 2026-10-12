# Technical Annotation App — Complete Implementation Spec

> **THE single source of truth for Claude Code.**  
> Contains: 43 drawing primitives, 20 arrowhead styles, gesture flows, text rules, data classes, render code, smooth curve closure system.

---

## TABLE OF CONTENTS

1. [Global Architecture](#1-global-architecture)
2. [Text Rule](#2-text-rule)
3. [Arrowhead System (20 styles)](#3-arrowhead-system)
4. [Group A: Two-Point Primitives (10)](#4-group-a)
5. [Group B: Tap-to-Place Primitives (20)](#5-group-b)
6. [Group C: Tap-on-Line Primitives (5)](#6-group-c)
7. [Group D: Drag-Area Primitives (4)](#7-group-d)
8. [Group E: Tap+Drag Primitives (2)](#8-group-e)
9. [Group F: Wizard Primitives (4)](#9-group-f)
10. [Implementation Slices](#10-implementation-slices)
11. [Smooth Curve Closure System](#11-smooth-curve-closure)

---

## 1. GLOBAL ARCHITECTURE

### Input Modes

```kotlin
// Detect input type from MotionEvent
when (event.getToolType(0)) {
    TOOL_TYPE_FINGER -> {
        hitTargetSize = 48.dp    // larger touch targets
        snapAssist = AGGRESSIVE  // snap to grid/angle/endpoint
        showMagnifier = true     // 2.5× loupe offset 60dp above finger
    }
    TOOL_TYPE_STYLUS -> {
        hitTargetSize = 12.dp    // precise
        snapAssist = SUBTLE
        showMagnifier = false
        usePressure = true       // map event.pressure to strokeWidth for freehand
    }
}
```

### Snap Engine

```kotlin
object SnapEngine {
    val GRID_SNAP = 5.dp
    val ANGLE_SNAP_DEGREES = listOf(0f, 15f, 30f, 45f, 60f, 75f, 90f) // + multiples
    val ENDPOINT_SNAP = 12.dp
    val MIDPOINT_SNAP = 12.dp
    val INTERSECTION_SNAP = 12.dp
    val PERPENDICULAR_SNAP = 8.dp
    val PARALLEL_SNAP = 8.dp
}
// Haptic: HapticFeedbackConstants.CLOCK_TICK on each snap event
```

### Gesture Vocabulary

| Gesture | Detection | Action |
|---|---|---|
| Single tap | DOWN→UP <200ms, move <10dp | Place point / select |
| Long press | DOWN held >400ms, move <10dp | Context menu / variant picker |
| Drag | DOWN→MOVE→UP, move >10dp | Draw / move / pan |
| Two-finger pinch | 2 pointers, distance changing | Zoom |
| Two-finger drag | 2 pointers, parallel | Pan |
| Double tap | 2 taps <300ms, dist <15dp | Edit text / open properties |

### Tool Palette (Bottom Bar)

```
┌──────────────────────────────────────────────────┐
│  ✏ Line  │  📏 Dim  │  ○ Shape  │  ╱ Mark  │  ⋮ More │
└──────────────────────────────────────────────────┘
Each opens radial sub-menu for variants.
Long-press any sub-tool → variant picker sheet.
```

### Undo/Redo

```kotlin
val undoStack = ArrayDeque<DrawingElement>()
val redoStack = ArrayDeque<DrawingElement>()
// Every completed element → push to undoStack
// Undo = pop undo → push redo
// Any new action clears redoStack
// Gesture shortcut: two-finger double-tap = undo
```

---

## 2. TEXT RULE

**A single graphic element carries EITHER computed text OR manual text. Never both.**

```
COMPUTED (C):  Value auto-calculated from geometry (distance × scale, angle, radius).
               If user taps the value to edit → element SWITCHES to MANUAL permanently.

MANUAL (M):    User types all text. Auto-increment suggestions are just pre-fills 
               the user can accept or change — still counts as MANUAL.

NONE (N):      Element has no text fields. Any annotation is a SEPARATE element.
```

Only 3 elements are COMPUTED: #05 Linear Dimension, #06 Angular Dimension, #07 Radius/Diameter.  
All other text-bearing elements (18 of them) are 100% MANUAL.  
22 elements carry NO TEXT at all.

### Keyboard Modes

```
NUMERIC:     Number pad + decimal + ± + engineering symbols row
             Triggered by: dimension values, tolerances, angles, sizes
             
ALPHA-CODE:  Full keyboard + auto-complete suggestions
             Triggered by: datum letters, tag codes, instrument IDs
             
FREE-TEXT:   Full keyboard + engineering symbols bar
             Triggered by: notes, labels, descriptions
```

### Engineering Symbols Bar (above system keyboard)

```
┌──────────────────────────────────────────────────────┐
│ Ø  R  SR  SØ  ⌴  ⌵  ↧  □  ±  °  ×  ′  ″  ²  ³    │
│ µ  Ω  #  @  ≤  ≥  ∥  ⊥  ∠  ≈  Δ  ∑  ←  →  ↑  ↓   │
└──────────────────────────────────────────────────────┘
Scrollable single row. Context-dependent: shows relevant symbols first.
```

---

## 3. ARROWHEAD SYSTEM

### 20 Arrowhead Styles — Complete Enum

```kotlin
enum class ArrowStyle {
    // ── Triangular (8) ──
    CLOSED_FILLED,      // ① solid triangle (ASME default)
    CLOSED_BLANK,       // ② outlined triangle (hollow)
    OPEN,               // ③ two wings, no base (ISO default)
    OPEN_30,            // ④ narrow 30° open
    OPEN_90,            // ⑤ wide 90° open
    CLOSED_HALF,        // ⑥ top half filled, bottom outlined
    DATUM_FILLED,       // ⑦ wide filled (equilateral-like)
    DATUM_BLANK,        // ⑧ wide outlined

    // ── Geometric shapes (7) ──
    DOT_FILLED,         // ⑨ filled circle
    DOT_SMALL,          // ⑩ smaller filled circle
    DOT_BLANK,          // ⑪ outlined circle (hollow)
    BOX_FILLED,         // ⑫ filled square, rotates with line
    BOX_BLANK,          // ⑬ outlined square
    DIAMOND_FILLED,     // ⑭ filled diamond (rotated square)
    DIAMOND_BLANK,      // ⑮ outlined diamond

    // ── Line-based (4) ──
    OBLIQUE,            // ⑯ 45° slash/tick (architectural)
    INTEGRAL,           // ⑰ perpendicular line across endpoint
    RIGHT_ANGLE,        // ⑱ L-shape (ordinate origin)
    CROSS,              // ⑲ + mark through endpoint

    // ── Special (4) ──
    ORIGIN_INDICATOR,   // ⑳ circle + short radius line
    DOUBLE_BARB,        // ㉑ two nested open arrow wings
    CONCAVE,            // ㉒ barbed/scooped base triangle
    NONE                // ㉓ no terminator
}
```

### Core Geometry — Triangle Vertex Computation

```
Given line from S (start) to E (end):

1. angle = atan2(E.y - S.y, E.x - S.x)          // ALWAYS use atan2, never atan
2. tip = E                                         // arrowhead tip IS the endpoint
3. Base center (behind tip along line):
     baseCenterX = tip.x - headLength * cos(angle)
     baseCenterY = tip.y - headLength * sin(angle)
4. Left & right base corners (perpendicular offset):
     left.x  = baseCenter.x + halfWidth * sin(angle)
     left.y  = baseCenter.y - halfWidth * cos(angle)
     right.x = baseCenter.x - halfWidth * sin(angle)
     right.y = baseCenter.y + halfWidth * cos(angle)
5. Draw: tip → left → right → close
6. INSET POINT = baseCenter (line must stop HERE, not at tip)
```

### Proportions

```
STANDARD (①②③):   headLength = max(strokeWidth * 3.0, 10dp)
                    halfWidth  = max(strokeWidth * 1.0, 3.5dp)
                    
NARROW (④):        halfWidth  = headLength * tan(15°) ≈ headLength * 0.27

WIDE (⑤⑦⑧):       halfWidth  = headLength * 1.0 (equilateral-like)

DIMENSION ARROWS:   headLength = 8dp (FIXED, not stroke-dependent)
                    halfWidth  = 3dp (FIXED)

DOT (⑨⑩⑪):        radius = max(strokeWidth * 1.5, 4dp)
                    DOT_SMALL: radius = max(strokeWidth * 0.8, 2.5dp)

BOX (⑫⑬):         halfSide = max(strokeWidth * 1.2, 4dp)

DIAMOND (⑭⑮):     forwardLen = max(strokeWidth * 2.0, 6dp)
                    sideLen = max(strokeWidth * 1.2, 4dp)

SLASH (⑯):         slashLen = max(strokeWidth * 3.0, 8dp)

INTEGRAL/CROSS (⑰⑲): armLen = max(strokeWidth * 2.0, 6dp)
```

### Complete Kotlin ArrowheadRenderer

```kotlin
import android.graphics.*
import kotlin.math.*

object ArrowheadRenderer {

    /**
     * Draws arrowhead at [tip] pointing in direction [angleRad].
     * Returns the INSET POINT where the parent line should stop.
     */
    fun draw(
        canvas: Canvas,
        tip: PointF,
        angleRad: Float,
        style: ArrowStyle,
        strokeWidth: Float,
        color: Int
    ): PointF {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        
        return when (style) {
            // ── TRIANGULAR ──
            ArrowStyle.CLOSED_FILLED -> drawTriangle(canvas, tip, angleRad, strokeWidth, paint, fill = true, ratio = 3.0f to 1.0f)
            ArrowStyle.CLOSED_BLANK  -> drawTriangle(canvas, tip, angleRad, strokeWidth, paint, fill = false, ratio = 3.0f to 1.0f)
            ArrowStyle.OPEN          -> drawOpenWings(canvas, tip, angleRad, strokeWidth, paint, ratio = 3.5f to 1.2f)
            ArrowStyle.OPEN_30       -> drawOpenWings(canvas, tip, angleRad, strokeWidth, paint, ratio = 4.0f to 0.27f)
            ArrowStyle.OPEN_90       -> drawOpenWings(canvas, tip, angleRad, strokeWidth, paint, ratio = 3.0f to 3.0f)
            ArrowStyle.CLOSED_HALF   -> drawHalfFilled(canvas, tip, angleRad, strokeWidth, paint)
            ArrowStyle.DATUM_FILLED  -> drawTriangle(canvas, tip, angleRad, strokeWidth, paint, fill = true, ratio = 2.5f to 1.5f)
            ArrowStyle.DATUM_BLANK   -> drawTriangle(canvas, tip, angleRad, strokeWidth, paint, fill = false, ratio = 2.5f to 1.5f)
            
            // ── GEOMETRIC ──
            ArrowStyle.DOT_FILLED    -> drawDot(canvas, tip, angleRad, strokeWidth, paint, fill = true, sizeMultiplier = 1.5f)
            ArrowStyle.DOT_SMALL     -> drawDot(canvas, tip, angleRad, strokeWidth, paint, fill = true, sizeMultiplier = 0.8f)
            ArrowStyle.DOT_BLANK     -> drawDot(canvas, tip, angleRad, strokeWidth, paint, fill = false, sizeMultiplier = 1.5f)
            ArrowStyle.BOX_FILLED    -> drawBox(canvas, tip, angleRad, strokeWidth, paint, fill = true)
            ArrowStyle.BOX_BLANK     -> drawBox(canvas, tip, angleRad, strokeWidth, paint, fill = false)
            ArrowStyle.DIAMOND_FILLED -> drawDiamond(canvas, tip, angleRad, strokeWidth, paint, fill = true)
            ArrowStyle.DIAMOND_BLANK  -> drawDiamond(canvas, tip, angleRad, strokeWidth, paint, fill = false)
            
            // ── LINE-BASED ──
            ArrowStyle.OBLIQUE       -> drawSlash(canvas, tip, angleRad, strokeWidth, paint, offsetAngle = PI.toFloat() / 4f)
            ArrowStyle.INTEGRAL      -> drawSlash(canvas, tip, angleRad, strokeWidth, paint, offsetAngle = PI.toFloat() / 2f)
            ArrowStyle.RIGHT_ANGLE   -> drawRightAngle(canvas, tip, angleRad, strokeWidth, paint)
            ArrowStyle.CROSS         -> drawCross(canvas, tip, angleRad, strokeWidth, paint)
            
            // ── SPECIAL ──
            ArrowStyle.ORIGIN_INDICATOR -> drawOriginIndicator(canvas, tip, angleRad, strokeWidth, paint)
            ArrowStyle.DOUBLE_BARB      -> drawDoubleBarb(canvas, tip, angleRad, strokeWidth, paint)
            ArrowStyle.CONCAVE          -> drawConcave(canvas, tip, angleRad, strokeWidth, paint)
            ArrowStyle.NONE             -> tip
        }
    }

    // ── TRIANGLE (covers ①②⑦⑧) ──
    private fun drawTriangle(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float,
        paint: Paint, fill: Boolean, ratio: Pair<Float, Float>
    ): PointF {
        val headLen = maxOf(sw * ratio.first, 10f)
        val halfW = maxOf(sw * ratio.second, 3.5f)
        val cosA = cos(angle); val sinA = sin(angle)
        
        val bcX = tip.x - headLen * cosA
        val bcY = tip.y - headLen * sinA
        val left = PointF(bcX + halfW * sinA, bcY - halfW * cosA)
        val right = PointF(bcX - halfW * sinA, bcY + halfW * cosA)
        
        val path = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        }
        paint.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
        paint.strokeWidth = maxOf(sw * 0.6f, 1.5f)
        paint.strokeJoin = Paint.Join.MITER
        paint.strokeMiter = 10f
        canvas.drawPath(path, paint)
        return PointF(bcX, bcY) // INSET — line stops here
    }

    // ── OPEN WINGS (covers ③④⑤) ──
    private fun drawOpenWings(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float,
        paint: Paint, ratio: Pair<Float, Float>
    ): PointF {
        val headLen = maxOf(sw * ratio.first, 12f)
        val halfW = maxOf(sw * ratio.second, 4f)
        val cosA = cos(angle); val sinA = sin(angle)
        
        val bcX = tip.x - headLen * cosA
        val bcY = tip.y - headLen * sinA
        val left = PointF(bcX + halfW * sinA, bcY - halfW * cosA)
        val right = PointF(bcX - halfW * sinA, bcY + halfW * cosA)
        
        val path = Path().apply {
            moveTo(left.x, left.y)
            lineTo(tip.x, tip.y)    // NO close() — just two lines meeting
            lineTo(right.x, right.y)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(sw * 0.6f, 1.5f)
        paint.strokeJoin = Paint.Join.MITER
        paint.strokeMiter = 10f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, paint)
        return PointF(bcX, bcY)
    }

    // ── HALF-FILLED (⑥) ──
    private fun drawHalfFilled(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float, paint: Paint
    ): PointF {
        val headLen = maxOf(sw * 3f, 10f)
        val halfW = maxOf(sw * 1f, 3.5f)
        val cosA = cos(angle); val sinA = sin(angle)
        
        val bcX = tip.x - headLen * cosA
        val bcY = tip.y - headLen * sinA
        val left = PointF(bcX + halfW * sinA, bcY - halfW * cosA)
        val right = PointF(bcX - halfW * sinA, bcY + halfW * cosA)
        val base = PointF(bcX, bcY)
        
        // Fill upper half
        val upperPath = Path().apply {
            moveTo(tip.x, tip.y); lineTo(left.x, left.y); lineTo(base.x, base.y); close()
        }
        paint.style = Paint.Style.FILL
        canvas.drawPath(upperPath, paint)
        
        // Stroke lower half
        val lowerPath = Path().apply {
            moveTo(base.x, base.y); lineTo(right.x, right.y); lineTo(tip.x, tip.y)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(sw * 0.6f, 1.5f)
        canvas.drawPath(lowerPath, paint)
        return base
    }

    // ── DOT (covers ⑨⑩⑪) ──
    private fun drawDot(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float,
        paint: Paint, fill: Boolean, sizeMultiplier: Float
    ): PointF {
        val r = maxOf(sw * sizeMultiplier, if (sizeMultiplier < 1f) 2.5f else 4f)
        paint.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
        paint.strokeWidth = maxOf(sw * 0.6f, 1.5f)
        canvas.drawCircle(tip.x, tip.y, r, paint)
        // Inset: line ends at circle edge
        return PointF(tip.x - r * cos(angle), tip.y - r * sin(angle))
    }

    // ── BOX (covers ⑫⑬) ──
    private fun drawBox(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float,
        paint: Paint, fill: Boolean
    ): PointF {
        val s = maxOf(sw * 1.2f, 4f)
        paint.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
        paint.strokeWidth = maxOf(sw * 0.6f, 1.5f)
        
        canvas.save()
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat(), tip.x, tip.y)
        canvas.drawRect(tip.x - s, tip.y - s, tip.x + s, tip.y + s, paint)
        canvas.restore()
        
        return PointF(tip.x - s * 2 * cos(angle), tip.y - s * 2 * sin(angle))
    }

    // ── DIAMOND (covers ⑭⑮) ──
    private fun drawDiamond(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float,
        paint: Paint, fill: Boolean
    ): PointF {
        val fwd = maxOf(sw * 2f, 6f)
        val side = maxOf(sw * 1.2f, 4f)
        val cosA = cos(angle); val sinA = sin(angle)
        
        val back = PointF(tip.x - fwd * 2 * cosA, tip.y - fwd * 2 * sinA)
        val mid = PointF(tip.x - fwd * cosA, tip.y - fwd * sinA)
        val top = PointF(mid.x + side * sinA, mid.y - side * cosA)
        val bot = PointF(mid.x - side * sinA, mid.y + side * cosA)
        
        val path = Path().apply {
            moveTo(tip.x, tip.y); lineTo(top.x, top.y)
            lineTo(back.x, back.y); lineTo(bot.x, bot.y); close()
        }
        paint.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
        paint.strokeWidth = maxOf(sw * 0.6f, 1.5f)
        canvas.drawPath(path, paint)
        return back
    }

    // ── SLASH / INTEGRAL (covers ⑯⑰) ──
    private fun drawSlash(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float,
        paint: Paint, offsetAngle: Float
    ): PointF {
        val len = maxOf(sw * 3f, 8f)
        val slashAngle = angle + offsetAngle
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(sw * 0.8f, 2f)
        paint.strokeCap = Paint.Cap.ROUND
        
        canvas.drawLine(
            tip.x - len * cos(slashAngle), tip.y - len * sin(slashAngle),
            tip.x + len * cos(slashAngle), tip.y + len * sin(slashAngle),
            paint
        )
        return tip // line goes through slash (no inset)
    }

    // ── RIGHT ANGLE (⑱) ──
    private fun drawRightAngle(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float, paint: Paint
    ): PointF {
        val arm = maxOf(sw * 2.5f, 7f)
        val perpAngle = angle + PI.toFloat() / 2f
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(sw * 0.7f, 1.5f)
        paint.strokeCap = Paint.Cap.SQUARE
        
        canvas.drawLine(
            tip.x, tip.y,
            tip.x + arm * cos(perpAngle), tip.y + arm * sin(perpAngle),
            paint
        )
        return tip
    }

    // ── CROSS (⑲) ──
    private fun drawCross(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float, paint: Paint
    ): PointF {
        val arm = maxOf(sw * 2f, 6f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(sw * 0.7f, 1.5f)
        
        // Line along arrow direction
        canvas.drawLine(
            tip.x - arm * cos(angle), tip.y - arm * sin(angle),
            tip.x + arm * cos(angle), tip.y + arm * sin(angle), paint
        )
        // Perpendicular line
        val pAngle = angle + PI.toFloat() / 2f
        canvas.drawLine(
            tip.x - arm * cos(pAngle), tip.y - arm * sin(pAngle),
            tip.x + arm * cos(pAngle), tip.y + arm * sin(pAngle), paint
        )
        return tip
    }

    // ── ORIGIN INDICATOR (⑳) ──
    private fun drawOriginIndicator(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float, paint: Paint
    ): PointF {
        val r = maxOf(sw * 1.5f, 5f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(sw * 0.6f, 1.5f)
        canvas.drawCircle(tip.x, tip.y, r, paint)
        // Short radius line pointing perpendicular
        val pAngle = angle + PI.toFloat() / 2f
        canvas.drawLine(tip.x, tip.y, tip.x + r * cos(pAngle), tip.y + r * sin(pAngle), paint)
        return PointF(tip.x - r * cos(angle), tip.y - r * sin(angle))
    }

    // ── DOUBLE BARB (㉑) ──
    private fun drawDoubleBarb(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float, paint: Paint
    ): PointF {
        // Outer barb
        val inset1 = drawOpenWings(canvas, tip, angle, sw, paint, ratio = 3.5f to 1.2f)
        // Inner barb (offset back, smaller)
        val innerTip = PointF(
            tip.x - sw * 1.5f * cos(angle),
            tip.y - sw * 1.5f * sin(angle)
        )
        paint.alpha = 140 // slightly transparent
        drawOpenWings(canvas, innerTip, angle, sw * 0.8f, paint, ratio = 3.0f to 1.0f)
        paint.alpha = 255
        return inset1
    }

    // ── CONCAVE / BARBED (㉒) ──
    private fun drawConcave(
        canvas: Canvas, tip: PointF, angle: Float, sw: Float, paint: Paint
    ): PointF {
        val headLen = maxOf(sw * 3f, 10f)
        val halfW = maxOf(sw * 1f, 3.5f)
        val inset = headLen * 0.4f // how deep the scoop goes
        val cosA = cos(angle); val sinA = sin(angle)
        
        val bcX = tip.x - headLen * cosA
        val bcY = tip.y - headLen * sinA
        val left = PointF(bcX + halfW * sinA, bcY - halfW * cosA)
        val right = PointF(bcX - halfW * sinA, bcY + halfW * cosA)
        // Scoop control point (pushed forward from base center)
        val scoopX = bcX + inset * cosA
        val scoopY = bcY + inset * sinA
        
        val path = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(left.x, left.y)
            quadTo(scoopX, scoopY, right.x, right.y) // curved base
            close()
        }
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
        return PointF(bcX, bcY)
    }
}
```

### Line Integration Pattern

```kotlin
fun drawLineWithArrows(
    canvas: Canvas, start: PointF, end: PointF,
    strokeWidth: Float, color: Int,
    startArrow: ArrowStyle = ArrowStyle.NONE,
    endArrow: ArrowStyle = ArrowStyle.NONE,
    dashPattern: FloatArray? = null
) {
    val angle = atan2(end.y - start.y, end.x - start.x)
    
    // 1. Draw arrowheads FIRST (they return inset points)
    var lineStart = start
    var lineEnd = end
    
    if (endArrow != ArrowStyle.NONE) {
        lineEnd = ArrowheadRenderer.draw(canvas, end, angle, endArrow, strokeWidth, color)
    }
    if (startArrow != ArrowStyle.NONE) {
        lineStart = ArrowheadRenderer.draw(canvas, start, angle + PI.toFloat(), startArrow, strokeWidth, color)
    }
    
    // 2. Draw line BETWEEN inset points (never through the arrowheads)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = Paint.Cap.BUTT
        if (dashPattern != null) pathEffect = DashPathEffect(dashPattern, 0f)
    }
    canvas.drawLine(lineStart.x, lineStart.y, lineEnd.x, lineEnd.y, paint)
}
```

### Dimension Arrow Flip (tight space)

```kotlin
fun drawDimensionArrows(canvas: Canvas, leftTip: PointF, rightTip: PointF, style: ArrowStyle, color: Int) {
    val distance = abs(rightTip.x - leftTip.x)
    val headLength = 8f // fixed for dimensions
    
    if (distance > headLength * 4) {
        // NORMAL: arrows point INWARD toward each other
        ArrowheadRenderer.draw(canvas, leftTip, 0f, style, 1.5f, color)           // →
        ArrowheadRenderer.draw(canvas, rightTip, PI.toFloat(), style, 1.5f, color) // ←
    } else {
        // TIGHT: arrows flip OUTWARD, text moves outside
        ArrowheadRenderer.draw(canvas, leftTip, PI.toFloat(), style, 1.5f, color)  // ← (outward)
        ArrowheadRenderer.draw(canvas, rightTip, 0f, style, 1.5f, color)           // → (outward)
    }
}
```

### Critical Pitfalls

```
PITFALL 1: Line pokes through arrowhead
  FIX: Draw line from start to INSET point (returned by ArrowheadRenderer.draw), not to tip.

PITFALL 2: Arrow flips wrong at certain angles
  FIX: Always use atan2(dy, dx). Argument order is (Y, X). Never use atan(dy/dx).

PITFALL 3: Blurry/rounded arrow tip
  FIX: Paint.Join.MITER + strokeMiter = 10f. Use FILL not STROKE for filled arrows.

PITFALL 4: Double-headed arrows asymmetric
  FIX: For start arrow, use angle + PI (add 180°) to reverse direction.

PITFALL 5: Arrow too small / too large
  FIX: Use max(strokeWidth * multiplier, minimumDp). Never use fixed pixels.
```

---

## 4. GROUP A: TWO-POINT PRIMITIVES

Gesture: **Tap point A → Tap point B** (or drag). Ghost extends from B for chaining.

### #01 — Straight Line

```kotlin
data class StraightLine(
    val start: PointF, val end: PointF,
    val dashType: DashType,     // SOLID, DASHED, CHAIN, PHANTOM, DOTTED, CHAIN_DOUBLE, STITCH_*
    val strokeWidth: Float,
    val startArrow: ArrowStyle, // any of 20 styles
    val endArrow: ArrowStyle,
    val color: Int, val layer: String
)
// DashType renders via Paint.setPathEffect(DashPathEffect(intervals, 0f))
// 7 dash variants + 7 stitch variants = 14 total line appearances from ONE component
```
- **Text:** NONE
- **Arrows:** Optional both ends (all 20 styles)
- **Uses:** Visible edges, hidden edges, center lines, pipe lines, boundaries, stitch lines

### #02 — Break Line Zigzag

```kotlin
data class BreakLineZigzag(
    val start: PointF, val end: PointF,
    val breakPosition: Float // 0.0..1.0, default 0.5 (centered)
)
// Render: line A→breakStart, zigzag peaks, line breakEnd→B
// Zigzag: 5-7 triangular peaks, amplitude = strokeWidth * 4
```
- **Text:** NONE | **Arrows:** No

### #03 — Break Line Wave

```kotlin
data class BreakLineWave(
    val start: PointF, val end: PointF,
    val breakPosition: Float
)
// Render: line, then bezier curve with 2-3 control points, then line
```
- **Text:** NONE | **Arrows:** No

### #04 — Stitch Line (Footwear)

```kotlin
data class StitchLine(
    val start: PointF, val end: PointF,
    val stitchType: StitchType // SINGLE_TOP, DOUBLE_TOP, ZIGZAG, CHAIN, BLIND, OVERLOCK, BACKSTITCH
)
// Each type has unique DashPathEffect or custom Path pattern
// DOUBLE_TOP: draw two parallel lines offset 1-2dp apart
// CHAIN: series of small ellipses along path
// OVERLOCK: looping wave pattern along path
```
- **Text:** NONE | **Arrows:** No | **Tags:** 👟 Footwear, garment, textile

### #05 — Linear Dimension

```kotlin
data class LinearDimension(
    val pointA: PointF, val pointB: PointF,
    val offset: Float,           // distance of dim line from feature
    val value: String,           // auto-computed OR manual override
    val isComputed: Boolean,     // starts true, switches to false if user edits
    val arrowStyle: ArrowStyle,  // typically CLOSED_FILLED
    val textPosition: TextPos    // ABOVE, BELOW, CENTERED, BROKEN
)
// Render: 2 extension lines + dimension line + 2 arrowheads + text
// Text position: centered on dim line, 2dp above, breaks the line underneath
// If tight space: arrows flip outward, text moves outside
```
- **Text:** COMPUTED (auto from distance×scale). Tap value → switches to MANUAL permanently.
- **Arrows:** YES — 2× facing inward (any of 20 styles)
- **Gesture:** Tap A → Tap B → Drag offset outward

### #06 — Angular Dimension

```kotlin
data class AngularDimension(
    val line1Start: PointF, val line1End: PointF,
    val line2Start: PointF, val line2End: PointF,
    val arcPosition: PointF,
    val value: String,
    val isComputed: Boolean
)
// Text position: midpoint of arc, tangent-aligned, offset outward
// Auto-appends ° suffix
```
- **Text:** COMPUTED | **Arrows:** YES — 2× on arc ends
- **Gesture:** Tap line1 → Tap line2 → Drag arc position

### #07 — Radius / Diameter

```kotlin
data class RadiusDiameter(
    val center: PointF, val arcPoint: PointF,
    val leaderAngle: Float,
    val prefix: String,        // "R", "Ø", "SR", "SØ" — toggle picker
    val value: String,
    val isComputed: Boolean,
    val insideMode: Boolean    // true if text fits inside circle
)
// Inside: text centered on diameter line, breaks the line
// Outside: leader extends outward, text horizontal at landing
```
- **Text:** COMPUTED | **Arrows:** YES — 1× on leader pointing to arc
- **Gesture:** Tap arc → Drag leader angle

### #08 — Grainline Arrow (Footwear)

```kotlin
data class GrainlineArrow(
    val start: PointF, val end: PointF,
    val label: String // "GRAIN", "STRETCH", "BIAS 45°", "WARP"
)
// Always double-headed. Label centered above arrow line.
// Snaps to 0°/45°/90° angles
```
- **Text:** MANUAL (pre-fills "GRAIN", editable) | **Arrows:** YES — 2× double-headed
- **Tags:** 👟 Footwear, garment, leather, woodworking

### #09 — Zigzag Inline (Resistor)

```kotlin
data class ZigzagInline(
    val start: PointF, val end: PointF,
    val peakCount: Int // auto from distance, typically 5-7
)
```
- **Text:** NONE | **Arrows:** No | **Uses:** Electronics resistor (ANSI), spring

### #10 — Coil / Bumps (Inductor)

```kotlin
data class CoilBumps(
    val start: PointF, val end: PointF,
    val bumpCount: Int,
    val coreType: CoreType // AIR, IRON (adds parallel lines), VARIABLE (adds arrow)
)
```
- **Text:** NONE | **Arrows:** No | **Uses:** Inductor, transformer (paired), HVAC coil, spring

---

## 5. GROUP B: TAP-TO-PLACE PRIMITIVES

Gesture: **Single tap** drops element at touch location. Resize via handles.

### #11 — Wave / Sine

```kotlin
data class WaveSine(val start: PointF, val end: PointF, val amplitude: Float, val frequency: Float)
```
- **Text:** NONE | **Uses:** AC source (inside circle), fluid hatch, break line

### #12 — Parallel Plates (Capacitor/Battery)

```kotlin
data class ParallelPlates(
    val start: PointF, val end: PointF,
    val plateType: PlateType // CAPACITOR (equal), POLARIZED (one curved), BATTERY (long+short alternating)
)
```
- **Text:** NONE | **Uses:** Capacitor, battery, GD&T parallelism

### #13 — Circle (Empty)

```kotlin
data class CircleElement(val center: PointF, val radius: Float)
// Also supports gesture recognition: rough circular drag → auto-snaps to perfect circle
```
- **Text:** NONE | **Uses:** GD&T circularity, spot weld, ceiling light, equipment base

### #14 — Circle + Text

```kotlin
data class CircleText(
    val center: PointF, val radius: Float,
    val text: String,        // upper text (function code: M, A, TT, SD...)
    val text2: String? = null // lower text (tag number: 101, 201...)
)
// Circle auto-grows if text wider than diameter - 8dp padding
// Font scales down: max 14sp, min 7sp
```
- **Text:** MANUAL (auto-increment tag#) | **IMMEDIATE** keyboard
- **Uses:** Motor, ammeter, instruments, detectors, thermostat, water heater

### #15 — Circle + Divider Line

```kotlin
data class CircleDivided(
    val center: PointF, val radius: Float,
    val upperText: String, val lowerText: String
)
// Horizontal line at vertical center. Two text zones: upper half, lower half.
```
- **Text:** MANUAL | **Uses:** P&ID panel instrument, electrical outlet variant

### #16 — Circle + Crosshairs

```kotlin
data class CircleCross(val center: PointF, val radius: Float, val labelBelow: String? = null)
// Cross lines extend exactly to circle boundary
```
- **Text:** NONE (optional label below, DEFERRED) | **Uses:** GD&T position, sprinkler, survey pin

### #17 — Concentric Circles

```kotlin
data class ConcentricCircles(val center: PointF, val outerR: Float, val innerR: Float)
```
- **Text:** NONE | **Uses:** GD&T concentricity, recessed light, bearing

### #18 — Pump/Motor (Circle + Internal Triangle)

```kotlin
data class PumpMotor(
    val center: PointF, val radius: Float,
    val direction: Float, // angle triangle points
    val filled: Boolean   // true=hydraulic, false=pneumatic
)
```
- **Text:** NONE | **Uses:** Hydraulic pump/motor, pneumatic compressor

### #19 — Split Circle (Callout)

```kotlin
data class SplitCircle(
    val center: PointF, val radius: Float,
    val topNum: String,      // section/detail number (auto-increment)
    val bottomRef: String,   // sheet reference
    val cutLine: Line? = null, // for section callouts
    val arrowDirection: Float? = null
)
```
- **Text:** MANUAL (auto-increment top#, sheet picker for bottom) | **IMMEDIATE**
- **Uses:** Section callout, detail callout, elevation marker

### #20 — Triangle (Standalone)

```kotlin
data class TriangleStandalone(
    val center: PointF,
    val orientation: Orientation, // UP, DOWN, LEFT, RIGHT
    val variant: TriangleVariant, // FLAG, DATUM, BENCHMARK, LEVEL, REVISION_DELTA
    val text: String,             // letter, number, or elevation value
    val attachedRect: Boolean     // datum has rectangle box for letter
)
// Datum: auto-increment A→B→C. App tracks used letters.
// Level: prefix picker [SSL] [TOS] [FFL] + numeric elevation value
// Benchmark: "BM" fixed label + elevation below
```
- **Text:** MANUAL (auto-increment letters/numbers) | **IMMEDIATE**

### #21 — Rectangle (Empty)

```kotlin
data class RectangleElement(val topLeft: PointF, val width: Float, val height: Float)
// Near 1:1 aspect → haptic + snap to square
```
- **Text:** NONE | **Gesture:** Corner-to-corner drag
- **Uses:** GD&T flatness, resistor IEC, IC, duct, equipment, panel

### #22 — Rectangle + Internal Lines

```kotlin
data class RectanglePattern(
    val topLeft: PointF, val width: Float, val height: Float,
    val pattern: InternalPattern // X_CROSS, SINGLE_DIAGONAL, PARALLEL, GRID
)
```
- **Text:** NONE | **Uses:** HVAC supply(×)/return(/), circuit breaker(×), elevator(×)

### #23 — Labeled Rectangle

```kotlin
data class LabeledRectangle(
    val topLeft: PointF, val width: Float, val height: Float,
    val text: String,
    val autoSize: Boolean // true = rect grows with text
)
```
- **Text:** MANUAL | **IMMEDIATE** keyboard | Double-tap to edit
- **Uses:** Basic dimension, structural level, room tag, steel callout W14×90

### #24 — Diamond

```kotlin
data class DiamondElement(val center: PointF, val size: Float, val text: String?)
```
- **Text:** MANUAL (number or tag) | **Uses:** Window tag, P&ID filter/computer function

### #25 — V-Shape (Surface Finish)

```kotlin
data class VShape(
    val position: PointF, val surfaceAngle: Float,
    val type: SurfaceFinishType, // BASIC, MACHINING_REQUIRED, NO_MACHINING
    val raValue: String?, val rzValue: String?,
    val layDirection: LayDir?, // PARALLEL, PERPENDICULAR, CROSSED, MULTI, CIRCULAR, RADIAL
    val process: String?, val removalAllowance: String?
)
// All text fields populated via mini-builder panel
```
- **Text:** MANUAL (builder) | **Uses:** Surface finish, V-groove weld, countersink

### #26 — Cylinder / Piston

```kotlin
data class CylinderPiston(
    val basePoint: PointF, val rodEnd: PointF,
    val actingType: ActingType // SINGLE, DOUBLE, CUSHIONED, TELESCOPIC
)
```
- **Text:** NONE | **Uses:** Hydraulic/pneumatic actuators

### #27 — Op-Amp Triangle

```kotlin
data class OpAmpTriangle(val center: PointF, val size: Float)
// Pin labels (+, −) are FIXED graphical elements, not editable
// REF designator (U3) is a SEPARATE #14 Circle+Text or #40 Leader
```
- **Text:** NONE | **Uses:** Op-amp, comparator

### #28 — Logic Gate

```kotlin
data class LogicGate(
    val center: PointF,
    val gateType: GateType, // AND, OR, NOT, NAND, NOR, XOR, XNOR, BUFFER
    val inputCount: Int,    // 2, 3, or 4
    val inverted: Boolean   // adds bubble on output
)
```
- **Text:** NONE | **Uses:** Digital electronics

### #29 — Hardware Mark (Footwear)

```kotlin
data class HardwareMark(
    val position: PointF,
    val hardwareType: HardwareType, // EYELET, GROMMET, RIVET, HOOK, D_RING, BUCKLE, SNAP
    val size: Float                 // mm diameter
)
```
- **Text:** NONE | **Tags:** 👟 Footwear, leather goods, bags

### #30 — Seam Cross-Section (Footwear)

```kotlin
data class SeamCrossSection(
    val position: PointF,
    val seamType: SeamType, // PLAIN, FRENCH, FELLED, TURNED, OVERLOCK, BOUND, PIPED, WELTED
    val label: String?      // auto from type: "French seam"
)
// Rendered as a small fixed-size icon (~20×15dp) from pre-drawn SVG paths
```
- **Text:** MANUAL (type name auto-fills) | **Tags:** 👟 Footwear, garment

---

## 6. GROUP C: TAP-ON-LINE PRIMITIVES

Gesture: **Tap on existing line/pipe/wire** → symbol inserts inline, auto-orients.

### #31 — Inline Triangle + Bar (Diode/Check Valve)

```kotlin
data class InlineTriangleBar(
    val parentLine: UUID, val positionOnLine: Float, // 0..1
    val variant: InlineVariant, // PLAIN, LED(+arrows), ZENER(bent bar), BUFFER(no bar)
    val direction: Boolean      // true=forward, false=reversed. Tap to flip.
)
```
- **Text:** NONE | **Uses:** Diode, LED, check valve, buffer gate

### #32 — Bowtie (Valve)

```kotlin
data class BowtieValve(
    val parentLine: UUID, val positionOnLine: Float,
    val valveType: ValveType, // GATE, GLOBE(+circle), BALL, BUTTERFLY, CHECK, CONTROL(+actuator)
    val actuatorType: ActuatorType, // NONE, MANUAL, PNEUMATIC, ELECTRIC, SOLENOID
    val tag: String?                // "GV-101"
)
```
- **Text:** MANUAL (tag code auto-increment) | **Uses:** All valve types across plumbing, P&ID, HVAC, fire, hydraulic

### #33 — Diamond Inline (Filter)

```kotlin
data class DiamondInline(val parentLine: UUID, val positionOnLine: Float)
```
- **Text:** NONE | **Uses:** Hydraulic filter, P&ID strainer

### #34 — Notch Mark (Footwear)

```kotlin
data class NotchMark(
    val parentLine: UUID, val positionOnLine: Float,
    val notchType: NotchType, // V_CUT, DOUBLE_V, STRAIGHT, DRILL_HOLE
    val label: String?        // "CF", "CB", "SS", "HN"
)
```
- **Text:** MANUAL (picker: CF/CB/SS/HN) | **Tags:** 👟 Footwear, pattern making

### #35 — Arrowhead (Line Modifier)

Not a standalone element — applied as a **property** to any line endpoint.

```kotlin
// Already defined in ArrowStyle enum (20 styles)
// Applied via: line.startArrow = ArrowStyle.CLOSED_FILLED
// Rendered via: ArrowheadRenderer.draw(...)
```

---

## 7. GROUP D: DRAG-AREA PRIMITIVES

Gesture: **Drag freehand loop** or **tap inside closed boundary**.

### #36 — Hatching Fill (8 tile patterns)

```kotlin
data class HatchFill(
    val boundaryPath: Path,
    val patternType: HatchPattern, // STEEL_45, CONCRETE_STIPPLE, EARTH_CROSSHATCH,
                                   // BRICK, WOOD_GRAIN, FLUID_WAVE, INSULATION_ZIG, GLASS_BLANK
    val angle: Float, val scale: Float, val spacing: Float
)
// Render: clip to boundary, fill with BitmapShader(tile, REPEAT, REPEAT) + rotation matrix
// Boundary detection: ray-cast from tap point, walk nearest lines to find closed loop
// Manual fallback: tap polygon points to define boundary
```
- **Text:** NONE

### #37 — Perforation Pattern (Footwear)

```kotlin
data class PerforationPattern(
    val boundaryPath: Path,
    val patternType: PerfPattern, // GRID, OFFSET, CIRCULAR, ALONG_PATH
    val holeRadius: Float, val spacing: Float, val rotation: Float
)
```
- **Text:** NONE | **Tags:** 👟 Brogue perforations, decorative

### #38 — Component Zone Fill (Footwear)

```kotlin
data class ComponentZone(
    val boundaryPath: Path,
    val zoneName: String,  // "VAMP", "COUNTER", "TOE PUFF", etc.
    val fillColor: Int, val opacity: Float // 0.1-0.3
)
// Suggestions: VAMP, QUARTER, COUNTER, TOE_PUFF, TONGUE, COLLAR, EYESTAY, FOXING, MUDGUARD, HEEL_TAB
```
- **Text:** MANUAL (zone name centered inside, IMMEDIATE) | **Tags:** 👟 Footwear

### #39 — Revision Cloud

```kotlin
data class RevisionCloud(
    val pathPoints: List<PointF>, // user's freehand loop
    val revLetter: Char           // auto-increment A→B→C
)
// Cloud generation: sample path at ~20dp intervals, generate semicircular bump at each sample
// Close from last point to first with bumps
```
- **Text:** MANUAL (rev letter auto-increment, editable)

---

## 8. GROUP E: TAP+DRAG PRIMITIVES

Gesture: **Tap on feature → Drag outward** to position annotation.

### #40 — Leader + Text (Note)

```kotlin
data class LeaderText(
    val tipPoint: PointF,       // arrow tip on the feature
    val elbowPoint: PointF,     // angled elbow
    val landingPoint: PointF,   // horizontal landing start
    val text: String,           // multi-line OK
    val arrowStyle: ArrowStyle  // any of 20 styles
)
// Text: left-aligned from landing point, wraps at ~40 chars
// Keyboard opens IMMEDIATELY after drag release
// Angle snaps to 30°/45°/60° for the leader line
```
- **Text:** MANUAL (100% user-typed) | **IMMEDIATE** keyboard
- **Arrows:** YES — 1× at feature tip

### #41 — Tolerance / Limit Annotation

```kotlin
data class ToleranceAnnotation(
    val position: PointF,
    val value: String,                  // "25.00"
    val format: ToleranceFormat,        // PLUS_MINUS, LIMITS, BASIC_BOXED, REFERENCE
    val tolPlus: String?, val tolMinus: String?,  // "+0.05", "-0.02"
    val maxLimit: String?, val minLimit: String?   // "25.05", "24.98"
)
// This is a SEPARATE element from dimensions (#05/#06/#07).
// Never mixed with computed values on the same graphic.
```
- **Text:** MANUAL (all fields user-typed) | **IMMEDIATE** keyboard (NUMERIC)

---

## 9. GROUP F: WIZARD PRIMITIVES

Gesture: **Tap to anchor → Builder bottom-sheet opens** → configure all fields → Apply.

### #42 — Welding Symbol

```kotlin
data class WeldingSymbol(
    val jointPoint: PointF,          // arrow tip at joint
    val refLineEnd: PointF,          // end of reference line
    val arrowSideWeld: WeldConfig?,  // below reference line
    val otherSideWeld: WeldConfig?,  // above reference line
    val tail: TailConfig?,           // process, spec reference
    val fieldWeld: Boolean,
    val allAround: Boolean,
    val meltThrough: Boolean,
    val contour: Contour?            // FLUSH, CONVEX, CONCAVE
)

data class WeldConfig(
    val weldType: WeldType,    // FILLET, V_GROOVE, BEVEL, U_GROOVE, J_GROOVE, SQUARE, PLUG, SPOT, SEAM, EDGE, STUD, SURFACING
    val size: String?,         // weld size (left of symbol)
    val length: String?,       // weld length (right of symbol)
    val pitch: String?,        // intermittent pitch
    val grooveAngle: String?,
    val rootOpening: String?
)
```
- **Text:** MANUAL (all via builder panel) | **Arrows:** YES — 1× to joint

### #43 — GD&T Feature Control Frame

```kotlin
data class GdtFcf(
    val featurePoint: PointF,
    val fcfPosition: PointF,
    val symbol: GdtSymbol,     // 14 options: STRAIGHTNESS, FLATNESS, CIRCULARITY, CYLINDRICITY,
                               // PROFILE_LINE, PROFILE_SURFACE, ANGULARITY, PERPENDICULARITY,
                               // PARALLELISM, POSITION, CONCENTRICITY, SYMMETRY,
                               // CIRCULAR_RUNOUT, TOTAL_RUNOUT
    val tolerance: String,     // "0.05"
    val modifier: Modifier?,   // MMC, LMC, or null
    val datums: List<DatumRef> // up to 3: [{letter:"A", modifier:null}, ...]
)
```
- **Text:** MANUAL (all via builder: symbol grid + numeric tolerance + datum picker)
- **Arrows:** YES — 1× leader to feature

### #44 — Hydraulic Spool (DCV)

```kotlin
data class HydraulicSpool(
    val center: PointF,
    val ports: Int,           // 2, 3, 4, or 5
    val positions: Int,       // 2 or 3
    val centerType: CenterType, // OPEN, CLOSED, TANDEM, FLOAT
    val leftActuation: Actuation, // SPRING, SOLENOID, PILOT, MANUAL, DETENT
    val rightActuation: Actuation,
    val portLabels: List<String>  // ["P", "T", "A", "B"]
)
```
- **Text:** MANUAL (port labels auto-fill P/T/A/B, editable)

### #45 — EU Material Label Grid (Footwear)

```kotlin
data class EuMaterialGrid(
    val position: PointF,
    val upperMaterial: MaterialType,  // LEATHER, COATED_LEATHER, TEXTILE, OTHER
    val liningMaterial: MaterialType,
    val soleMaterial: MaterialType,
    val upperSpec: String?,           // "Nappa 0.9mm"
    val liningSpec: String?,
    val soleSpec: String?
)
// 3 rows × 2 columns: part pictogram + material pictogram
// Part pictograms: standardized shoe silhouettes per EU Directive 94/11/EC
// Material pictograms: hide shape (leather), weave grid (textile), diamond (other)
```
- **Text:** MANUAL (material type picker + optional spec text) | **Tags:** 👟 EU compliance

---

## 10. IMPLEMENTATION SLICES

```
SLICE 1: Canvas + Touch Engine
  - Canvas View with pan/zoom (two-finger)
  - Touch event handling (finger vs stylus detection)
  - Snap engine (grid, angle, endpoint, midpoint, perpendicular, parallel)
  - Undo/redo stack
  - Layer system (visibility toggle, lock, active layer)

SLICE 2: ArrowheadRenderer
  - All 20 arrowhead styles (enum + renderer)
  - Triangle vertex math with atan2
  - Line inset calculation
  - Unit tests: draw arrows at 0°, 45°, 90°, 135°, 180°, 225°, 270°, 315°
  - Dimension flip logic (tight space)

SLICE 3: Lines (#01) + Arrowheads (#35)
  - Straight line with 14 dash pattern variants
  - Arrowhead style as endpoint property
  - Two-point gesture (tap-tap or drag)
  - Chain-continue mode (ghost extends from endpoint)
  - Integration with ArrowheadRenderer (inset points)

SLICE 4: Dimensions (#05, #06, #07) + Tolerance (#41)
  - Linear dimension: extension lines + dim line + arrows + text
  - Angular dimension: arc + arrows + angle text
  - Radius/diameter: leader + prefix toggle
  - Computed text mode (auto from geometry)
  - Tap-to-override → switches to MANUAL permanently
  - Tight-space arrow flip
  - Tolerance annotation as separate element

SLICE 5: Leaders (#40) + Callouts (#19, #20, #39)
  - Leader line + elbow + horizontal landing + text
  - Keyboard integration (IMMEDIATE open)
  - Smart suggestions bar above keyboard
  - Split circle callout (section/detail)
  - Triangle variants (datum/flag/BM/level)
  - Revision cloud (freehand → bump generation)

SLICE 6: Shapes (#13-17, #21-24, #25)
  - Circle (empty, +text, +divider, +crosshairs, concentric)
  - Circle gesture recognition (rough draw → snap to perfect)
  - Rectangle (empty, +pattern, labeled)
  - Diamond, V-shape (surface finish builder)
  - Double-tap to add text
  - Auto-sizing (shape grows with text content)

SLICE 7: Inline Symbols (#31-34, #32)
  - Tap-on-line insertion system
  - Auto-orient symbol along line direction
  - Inline triangle + bar (diode/check)
  - Bowtie valve with type picker + actuator
  - Diamond inline (filter)
  - Tap to flip direction

SLICE 8: Electronics & Hydraulics (#09-12, #18, #26-28, #44)
  - Zigzag, coil, wave, parallel plates (two-point generate)
  - Pump/motor (circle + internal triangle)
  - Cylinder/piston (drag base to rod)
  - Op-amp, logic gates (tap-place from palette)
  - Hydraulic spool DCV wizard

SLICE 9: Area Fills (#36-39)
  - Boundary detection (ray-cast from tap point)
  - Manual boundary (polygon tap mode)
  - 8 hatch pattern tiles (BitmapShader with rotation/scale)
  - Perforation pattern (dot grid in boundary)
  - Component zone fill (semi-transparent tint + label)
  - Revision cloud (in Group D but same area concept)

SLICE 10: Footwear Pack (#04, #08, #29, #30, #34, #37, #38, #45)
  - Stitch lines (7 variants)
  - Grainline arrow (double-headed + label)
  - Hardware marks (7 types from sub-palette)
  - Seam cross-section symbols (9 types, pre-drawn icons)
  - Notch marks (4 types, tap-on-edge)
  - Perforation patterns
  - Component zones
  - EU material label grid wizard

SLICE 11: Welding (#42) + GD&T (#43)
  - Welding symbol framework (arrow + ref line + tail)
  - Weld type shapes (12 types on reference line)
  - Welding builder bottom-sheet panel
  - GD&T FCF builder (14 symbols grid + tolerance + datums)
  - Leader integration for both
  - These are the most complex — save for last

SLICE 12: Polish & Export
  - Properties panel for all elements (bottom sheet)
  - Clipboard (copy/paste/group/align)
  - Export: SVG, DXF, PDF, PNG
  - File format (JSON serialization of all DrawingElements)
  - Drawing metadata (title, scale, units, revision history)
```

---

## ELEMENT SUMMARY TABLE

| # | Name | Gesture | Text | Arrows | 
|---|---|---|---|---|
| 01 | Straight Line | A: tap-tap | N | Optional (20 styles) |
| 02 | Break Zigzag | A: tap-tap | N | No |
| 03 | Break Wave | A: tap-tap | N | No |
| 04 | Stitch Line 👟 | A: tap-tap | N | No |
| 05 | Linear Dimension | A: tap-tap-drag | C | Yes (2×) |
| 06 | Angular Dimension | A: tap-tap-drag | C | Yes (2×) |
| 07 | Radius/Diameter | A: tap-drag | C | Yes (1×) |
| 08 | Grainline Arrow 👟 | A: tap-tap | M | Yes (2× double) |
| 09 | Zigzag Inline | A: tap-tap | N | No |
| 10 | Coil/Bumps | A: tap-tap | N | No |
| 11 | Wave/Sine | B: tap-place | N | No |
| 12 | Parallel Plates | B: tap-place | N | No |
| 13 | Circle (empty) | B: tap-place | N | No |
| 14 | Circle + Text | B: tap-place | M | No |
| 15 | Circle + Divider | B: tap-place | M | No |
| 16 | Circle + Crosshairs | B: tap-place | N | No |
| 17 | Concentric Circles | B: tap-place | N | No |
| 18 | Pump/Motor | B: tap-place | N | No |
| 19 | Split Circle | B: tap-place | M | No |
| 20 | Triangle | B: tap-place | M | No |
| 21 | Rectangle (empty) | B: drag | N | No |
| 22 | Rect + Internal Lines | B: tap-place | N | No |
| 23 | Labeled Rectangle | B: tap-place | M | No |
| 24 | Diamond | B: tap-place | M | No |
| 25 | V-Shape | B: tap-place | M | No |
| 26 | Cylinder/Piston | B: drag | N | No |
| 27 | Op-Amp Triangle | B: tap-place | N | No |
| 28 | Logic Gate | B: tap-place | N | No |
| 29 | Hardware Mark 👟 | B: tap-place | N | No |
| 30 | Seam Cross-Section 👟 | B: tap-place | M | No |
| 31 | Inline Triangle+Bar | C: tap-on-line | N | No |
| 32 | Bowtie (Valve) | C: tap-on-line | M | No |
| 33 | Diamond Inline | C: tap-on-line | N | No |
| 34 | Notch Mark 👟 | C: tap-on-line | M | No |
| 35 | Arrowhead (modifier) | C: property | N | IS the arrow |
| 36 | Hatching Fill | D: tap-fill | N | No |
| 37 | Perforation 👟 | D: drag-area | N | No |
| 38 | Component Zone 👟 | D: drag-area | M | No |
| 39 | Revision Cloud | D: drag-loop | M | No |
| 40 | Leader + Text | E: tap-drag | M | Yes (1×) |
| 41 | Tolerance Annotation | E: tap-place | M | No |
| 42 | Welding Symbol | F: wizard | M | Yes (1×) |
| 43 | GD&T FCF | F: wizard | M | Yes (1×) |
| 44 | Hydraulic Spool | F: wizard | M | No |
| 45 | EU Material Grid 👟 | F: wizard | M | No |

**Totals:** 45 entries (43 unique implementations + 2 dual-mode overlaps for diamond #24/#33 and arrowhead #35)

**Text breakdown:** 3 Computed, 18 Manual, 22 None  
**Arrow-bearing elements:** 12 (all use ArrowheadRenderer with 20 style options)  
**Footwear-specific:** 8 elements marked with 👟

---

## 11. SMOOTH CURVE CLOSURE SYSTEM

When drawing a freehand curve or spline and the endpoint approaches the start point, the app snaps to it and closes the path smoothly — no visible corner, just a seamless continuous loop with C1 tangent continuity at the join.

### Algorithm: Centripetal Catmull-Rom Spline (α=0.5)

Catmull-Rom splines pass THROUGH every control point (unlike Bezier where control points are off-curve). The user's touch samples ARE the control points — what they draw is what they get. The centripetal variant (α=0.5) is mathematically proven to never produce cusps or self-intersections within curve segments (Yuksel et al., 2011).

**How closure works — array wrapping:**

For an open spline each segment needs 4 points [Pi-1, Pi, Pi+1, Pi+2]. The first and last segments are missing neighbors. For a CLOSED spline, wrap the array — the missing neighbors come from the other end of the loop:

```
OPEN:   [P0, P1, P2, P3, P4, P5]
  Segment 0: [???, P0, P1, P2]     ← missing P-1
  Segment 4: [P3, P4, P5, ???]     ← missing P6

CLOSED: [P0, P1, P2, P3, P4, P5]  (P5 snapped ≈ P0, then removed)
  Segment 0: [P4, P0, P1, P2]     ← P4 wraps from end
  Segment 3: [P2, P3, P4, P0]     ← P0 wraps from start
  Segment 4: [P3, P4, P0, P1]     ← closing segment

  The tangent at P0 is computed from neighbors P4 and P1.
  Result: perfect C1 continuity. No special-case math. The wrap handles everything.
```

### Complete Kotlin Implementation

```kotlin
import android.graphics.Path
import android.graphics.PointF
import kotlin.math.*

/**
 * Centripetal Catmull-Rom spline with closed-loop support.
 *
 * Converts sampled touch points into a smooth Path for Canvas drawing.
 * When closed=true, the path forms a smooth loop with C1 continuity at the join.
 *
 * Usage:
 *   val smoother = CatmullRomSmoother()
 *   val path = smoother.createPath(points, closed = true)
 *   canvas.drawPath(path, paint)
 */
class CatmullRomSmoother(
    private val alpha: Float = 0.5f,    // 0.5 = centripetal (no cusps, guaranteed)
    private val tension: Float = 0f      // 0 = standard smoothness
) {

    fun createPath(points: List<PointF>, closed: Boolean = false): Path {
        if (points.size < 2) return Path()
        if (points.size == 2) return Path().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[1].x, points[1].y)
            if (closed) close()
        }

        val path = Path()

        // For closed: remove duplicate endpoint if it's the same as start (from snap)
        val workingPoints = if (closed && points.last().distTo(points.first()) < 1f) {
            points.dropLast(1)
        } else {
            points
        }

        val n = workingPoints.size
        if (n < 3) return Path().apply {
            moveTo(workingPoints[0].x, workingPoints[0].y)
            for (i in 1 until n) lineTo(workingPoints[i].x, workingPoints[i].y)
            if (closed) close()
        }

        path.moveTo(workingPoints[0].x, workingPoints[0].y)
        val segmentCount = if (closed) n else n - 1

        for (i in 0 until segmentCount) {
            val p1 = workingPoints[wrapIndex(i, n, closed)]
            val p2 = workingPoints[wrapIndex(i + 1, n, closed)]

            val p0 = if (closed) {
                workingPoints[wrapIndex(i - 1, n, true)]
            } else if (i == 0) {
                PointF(2 * p1.x - p2.x, 2 * p1.y - p2.y) // reflect p2 through p1
            } else {
                workingPoints[i - 1]
            }

            val p3 = if (closed) {
                workingPoints[wrapIndex(i + 2, n, true)]
            } else if (i == segmentCount - 1) {
                PointF(2 * p2.x - p1.x, 2 * p2.y - p1.y) // reflect p1 through p2
            } else {
                workingPoints[i + 2]
            }

            val (cp1, cp2) = catmullRomToBezier(p0, p1, p2, p3)
            path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
        }

        if (closed) path.close() // safe — last cubicTo already arrives at P0

        return path
    }

    /**
     * Converts a Catmull-Rom segment (4 points) into cubic Bezier control points.
     * Returns the two INNER control points (cp1, cp2) for Path.cubicTo().
     */
    private fun catmullRomToBezier(
        p0: PointF, p1: PointF, p2: PointF, p3: PointF
    ): Pair<PointF, PointF> {
        val d01 = p0.distTo(p1).pow(alpha).coerceAtLeast(0.0001f)
        val d12 = p1.distTo(p2).pow(alpha).coerceAtLeast(0.0001f)
        val d23 = p2.distTo(p3).pow(alpha).coerceAtLeast(0.0001f)

        val t1x = (1f - tension) * d12 * (
            (p1.x - p0.x) / d01 - (p2.x - p0.x) / (d01 + d12) + (p2.x - p1.x) / d12
        )
        val t1y = (1f - tension) * d12 * (
            (p1.y - p0.y) / d01 - (p2.y - p0.y) / (d01 + d12) + (p2.y - p1.y) / d12
        )
        val t2x = (1f - tension) * d12 * (
            (p2.x - p1.x) / d12 - (p3.x - p1.x) / (d12 + d23) + (p3.x - p2.x) / d23
        )
        val t2y = (1f - tension) * d12 * (
            (p2.y - p1.y) / d12 - (p3.y - p1.y) / (d12 + d23) + (p3.y - p2.y) / d23
        )

        return Pair(
            PointF(p1.x + t1x / 3f, p1.y + t1y / 3f),  // cp1
            PointF(p2.x - t2x / 3f, p2.y - t2y / 3f)   // cp2
        )
    }

    private fun wrapIndex(i: Int, n: Int, closed: Boolean): Int {
        return if (closed) ((i % n) + n) % n else i.coerceIn(0, n - 1)
    }

    private fun PointF.distTo(other: PointF): Float {
        val dx = x - other.x; val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun Float.pow(exp: Float): Float = this.toDouble().pow(exp.toDouble()).toFloat()
}
```

### Snap-to-Close Manager

```kotlin
/**
 * Manages snap-to-close detection during freehand drawing.
 *
 * FLOW:
 * 1. User draws freehand (collecting touch points)
 * 2. On each ACTION_MOVE: check if current point is within SNAP_RADIUS of start
 * 3. States: DRAWING → APPROACHING (preview zone) → SNAPPED (locked)
 * 4. On ACTION_UP in SNAPPED state: close with CatmullRomSmoother(closed=true)
 */
class SnapToCloseManager(
    private val snapRadiusDp: Float = 20f,
    private val previewRadiusDp: Float = 30f
) {
    private var startPoint: PointF? = null
    private var isSnapped = false

    fun onDrawStart(point: PointF) {
        startPoint = point
        isSnapped = false
    }

    fun onDrawMove(currentPoint: PointF, density: Float): SnapState {
        val start = startPoint ?: return SnapState.DRAWING
        val distance = start.distTo(currentPoint)
        val snapPx = snapRadiusDp * density
        val previewPx = previewRadiusDp * density

        return when {
            distance < snapPx -> {
                isSnapped = true
                SnapState.SNAPPED
            }
            distance < previewPx -> {
                isSnapped = false
                val progress = 1f - (distance - snapPx) / (previewPx - snapPx)
                SnapState.APPROACHING(progress.coerceIn(0f, 1f))
            }
            else -> {
                isSnapped = false
                SnapState.DRAWING
            }
        }
    }

    fun onDrawEnd(): Boolean = isSnapped

    sealed class SnapState {
        object DRAWING : SnapState()
        data class APPROACHING(val progress: Float) : SnapState() // 0..1
        object SNAPPED : SnapState()
    }

    private fun PointF.distTo(other: PointF): Float {
        val dx = x - other.x; val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}
```

### Visual Feedback Rendering

```kotlin
fun drawSnapFeedback(
    canvas: Canvas,
    startPoint: PointF,
    snapState: SnapToCloseManager.SnapState,
    density: Float
) {
    when (snapState) {
        is SnapToCloseManager.SnapState.APPROACHING -> {
            // Pulsing ring growing as user approaches
            val maxRadius = 20f * density
            val currentRadius = maxRadius * (0.5f + 0.5f * snapState.progress)
            val alpha = (80 * snapState.progress).toInt()

            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(alpha, 88, 166, 255)
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
            }
            canvas.drawCircle(startPoint.x, startPoint.y, currentRadius, ringPaint)

            // Dashed snap zone circle
            val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(40, 88, 166, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1f * density
                pathEffect = DashPathEffect(floatArrayOf(4f * density, 4f * density), 0f)
            }
            canvas.drawCircle(startPoint.x, startPoint.y, maxRadius, dashPaint)
        }

        is SnapToCloseManager.SnapState.SNAPPED -> {
            val radius = 14f * density

            // Green glow
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(60, 63, 185, 80)
                style = Paint.Style.FILL
                maskFilter = BlurMaskFilter(8f * density, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawCircle(startPoint.x, startPoint.y, radius * 1.5f, glowPaint)

            // Solid green ring
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(200, 63, 185, 80)
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * density
            }
            canvas.drawCircle(startPoint.x, startPoint.y, radius, ringPaint)

            // Center dot
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(255, 63, 185, 80)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(startPoint.x, startPoint.y, 3f * density, dotPaint)

            // Trigger haptic once on entering SNAPPED state:
            // view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }

        else -> { /* DRAWING = no visual feedback */ }
    }
}
```

### Point Reduction (Ramer-Douglas-Peucker)

Touch events produce hundreds of points per second. Reduce before splining:

```kotlin
/**
 * Ramer-Douglas-Peucker: reduces point count while preserving shape.
 * @param epsilon  Tolerance in dp. Recommended: 2.0 for finger, 1.0 for stylus.
 */
fun reducePoints(points: List<PointF>, epsilon: Float): List<PointF> {
    if (points.size < 3) return points

    val first = points.first()
    val last = points.last()
    var maxDist = 0f
    var maxIndex = 0

    for (i in 1 until points.size - 1) {
        val d = perpendicularDistance(points[i], first, last)
        if (d > maxDist) { maxDist = d; maxIndex = i }
    }

    return if (maxDist > epsilon) {
        val left = reducePoints(points.subList(0, maxIndex + 1), epsilon)
        val right = reducePoints(points.subList(maxIndex, points.size), epsilon)
        left.dropLast(1) + right
    } else {
        listOf(first, last)
    }
}

private fun perpendicularDistance(point: PointF, lineStart: PointF, lineEnd: PointF): Float {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y
    val lenSq = dx * dx + dy * dy
    if (lenSq < 0.0001f) return point.distTo(lineStart)
    val t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / lenSq
    val proj = PointF(lineStart.x + t * dx, lineStart.y + t * dy)
    return point.distTo(proj)
}
```

### Complete Freehand Drawing Flow

```kotlin
class FreehandCurveDrawer(
    private val smoother: CatmullRomSmoother = CatmullRomSmoother(),
    private val snapManager: SnapToCloseManager = SnapToCloseManager()
) {
    private val rawPoints = mutableListOf<PointF>()
    private var currentPath: Path? = null

    fun onTouchDown(x: Float, y: Float) {
        rawPoints.clear()
        rawPoints.add(PointF(x, y))
        snapManager.onDrawStart(PointF(x, y))
    }

    fun onTouchMove(x: Float, y: Float, density: Float): SnapToCloseManager.SnapState {
        rawPoints.add(PointF(x, y))
        val snapState = snapManager.onDrawMove(PointF(x, y), density)

        // Live preview (reduced + smoothed)
        val reduced = reducePoints(rawPoints, 2f * density)
        val previewClosed = snapState is SnapToCloseManager.SnapState.SNAPPED
        currentPath = smoother.createPath(reduced, closed = previewClosed)

        return snapState
    }

    fun onTouchUp(): FreehandCurveResult {
        val isClosed = snapManager.onDrawEnd()
        val finalPoints = reducePoints(rawPoints, 2f)
        val finalPath = smoother.createPath(finalPoints, closed = isClosed)

        return FreehandCurveResult(
            path = finalPath,
            controlPoints = finalPoints,
            isClosed = isClosed
        )
    }

    fun getPreviewPath(): Path? = currentPath
}

data class FreehandCurveResult(
    val path: Path,
    val controlPoints: List<PointF>,
    val isClosed: Boolean
)
```

### Which Primitives Use This System

| Primitive | Usage |
|---|---|
| #36 Hatching Fill | Freehand boundary option → snap close → fill with pattern |
| #37 Perforation Zone | Freehand boundary → snap close → fill with dots |
| #38 Component Zone | Freehand boundary → snap close → fill with tint + label |
| #39 Revision Cloud | Freehand loop → snap close → convert outline to bumps |
| Freehand Curve tool | Standalone drawing tool for custom shapes and annotations |

The `CatmullRomSmoother` is also reusable for smoothing ANY polyline path (even open), such as stitch lines or decorative curves.
