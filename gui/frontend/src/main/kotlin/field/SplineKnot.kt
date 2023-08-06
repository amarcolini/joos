package field

import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.deg
import io.nacular.doodle.core.Behavior
import io.nacular.doodle.core.behavior
import io.nacular.doodle.core.renderProperty
import io.nacular.doodle.drawing.*
import io.nacular.doodle.geometry.*
import io.nacular.doodle.system.SystemPointerEvent
import kotlin.math.roundToInt

abstract class SplineKnot : FieldEntity() {
    init {
        bounds = Rectangle(4.0, 4.0)
        center = origin
    }

    protected var stretchFactor = 0.3
    protected abstract val point: Shape
    protected abstract val startKnot: Shape
    protected abstract val endKnot: Shape

    enum class LengthMode {
        MATCH_LENGTH, FIXED_LENGTH, FREE_LENGTH
    }

    enum class TangentMode {
        FIXED, FREE
    }

    var lengthMode by renderProperty(LengthMode.FIXED_LENGTH)
    var tangentMode = TangentMode.FREE
    var startVisible by renderProperty(true)
    var endVisible by renderProperty(true)

    var tangent by renderProperty(0.deg)
    var startTangentMag by renderProperty(-1.0)
    var endTangentMag by renderProperty(-1.0)
    var defaultTangentMag by renderProperty(40.0)

    val onChange = ArrayList<(SplineKnot) -> Unit>()

    init {
        clipCanvasToBounds = false
        focusChanged += { _, _, _ ->
            rerender()
        }

        Dragger(this).apply {
            var knotSelected = 0
            var isMove = false
            mouseReleased = { _, _ ->
                isMove = false
            }
            mousePressed = { pos, buttons ->
                isMove = buttons.contains(SystemPointerEvent.Button.Button1)
                knotSelected = when {
                    !hasFocus -> 0
                    startVisible && pos in startKnot -> 1
                    endVisible && pos in endKnot -> 2
                    else -> 0
                }
            }
            mouseDragged = { pos, delta ->
                if (isMove) {
                    if (knotSelected == 0) {
                        val extreme = Point(Field.fieldSize, Field.fieldSize) * 0.5
                        position = (position + delta).coerceIn(-extreme, extreme).roundToNearest(0.1)
                        onChange.forEach { it(this@SplineKnot) }
                    } else {
                        val vec = (pos).toVector2d() * if (knotSelected == 1) -1.0 else 1.0
                        val newMag = vec.norm() / stretchFactor
                        when (this@SplineKnot.tangentMode) {
                            TangentMode.FREE -> {
                                val newAngle = vec.angle().degrees.roundToInt().deg
                                tangent = newAngle
                            }
                            TangentMode.FIXED -> {}
                        }
                        when (this@SplineKnot.lengthMode) {
                            LengthMode.FIXED_LENGTH -> {
                                startTangentMag = -1.0
                                endTangentMag = -1.0
                            }
                            LengthMode.MATCH_LENGTH -> {
                                startTangentMag = newMag
                                endTangentMag = newMag
                            }
                            LengthMode.FREE_LENGTH -> {
                                if (knotSelected == 1) startTangentMag = newMag
                                else endTangentMag = newMag
                            }
                        }
                    }
                }
            }
        }
    }

    override fun intersects(point: Point): Boolean {
        val newPoint = point - position
        return if (!hasFocus) newPoint in this.point
        else {
            (startVisible && newPoint in startKnot) ||
                    (endVisible && newPoint in endKnot) ||
                    newPoint in this.point
        }
    }

    override fun render(canvas: Canvas) {
        onChange.forEach { it(this@SplineKnot) }
    }
}

class PathKnot : SplineKnot() {
    override val point: Circle = Circle(origin, 1.75)
    override var startKnot: Circle = Circle(1.0)
    override var endKnot: Circle = Circle(1.0)

    var behavior: Behavior<PathKnot>? by behavior()

    var isSpecial = false

    override fun render(canvas: Canvas) {
        super.render(canvas)
        startKnot = Circle(
            Vector2d.polar(
                (if (startTangentMag >= 0) startTangentMag else defaultTangentMag) * -stretchFactor,
                tangent
            ).toPoint() + origin, 1.0
        )
        endKnot = Circle(
            Vector2d.polar(
                (if (endTangentMag >= 0) endTangentMag else defaultTangentMag) * stretchFactor,
                tangent
            ).toPoint() + origin, 1.0
        )

        val renderBase = {
            canvas.circle(
                point,
                Stroke(if (isSpecial) Color.Green.darker(0.1f) else Color.Transparent, 0.5),
                Color.Blue
            )
        }

        if (!hasFocus) {
            renderBase()
            return
        }

        if (startVisible) canvas.line(point.center, startKnot.center, Stroke(Color.Black, 0.2))
        if (endVisible) canvas.line(point.center, endKnot.center, Stroke(Color.Black, 0.2))

        canvas.circle(point.withRadius(point.radius + 1.0), Color(66u, 135u, 245u, 0.8f))
        renderBase()

        if (startVisible) canvas.circle(startKnot, Stroke(Color.Black, 0.2), Color.Blue)
        if (endVisible) canvas.circle(endKnot, Stroke(Color.Black, 0.2), Color.Blue)
    }
}

class HeadingKnot : SplineKnot() {
    override val point: Circle = Circle(origin, 1.75)
    override var startKnot: Circle = Circle(1.0)
    override var endKnot: Circle = Circle(1.0)

    var behavior: Behavior<HeadingKnot>? by behavior()

    init {
        lengthMode = LengthMode.FIXED_LENGTH
        tangentMode = TangentMode.FREE
        startVisible = false
    }

    override fun render(canvas: Canvas) {
        super.render(canvas)
        startKnot = Circle(
            Vector2d.polar(
                (if (startTangentMag >= 0) startTangentMag else defaultTangentMag) * -stretchFactor,
                tangent
            ).toPoint() + origin, 1.0
        )
        endKnot = Circle(
            Vector2d.polar(
                (if (endTangentMag >= 0) endTangentMag else defaultTangentMag) * stretchFactor,
                tangent
            ).toPoint() + origin, 1.0
        )

        if (!hasFocus) {
            canvas.circle(point, Color.Red)
            return
        }

        if (startVisible) canvas.line(point.center, startKnot.center, Stroke(Color.Black, 0.2))
        if (endVisible) canvas.line(point.center, endKnot.center, Stroke(Color.Black, 0.2))

        canvas.circle(point.withRadius(point.radius + 1.0), Color(235u, 64u, 52u, 0.8f))
        canvas.circle(point, Color.Red)

        if (startVisible) canvas.circle(startKnot, Stroke(Color.Black, 0.2), Color.Red)
        if (endVisible) canvas.circle(endKnot, Stroke(Color.Black, 0.2), Color.Red)
    }
}