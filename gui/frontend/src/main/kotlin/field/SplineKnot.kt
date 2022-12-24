package field

import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.deg
import io.nacular.doodle.core.renderProperty
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.drawing.circle
import io.nacular.doodle.geometry.Circle
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle

class SplineKnot : FieldEntity() {
    private val stretchFactor = 0.2
    private val point: Circle = Circle(2.0)
    private var startKnot: Circle = Circle(1.0)
    private var endKnot: Circle = Circle(1.0)

    enum class Mode {
        MATCH_LENGTH, FIXED_LENGTH, FREE_LENGTH
    }

    var mode by renderProperty(Mode.FIXED_LENGTH)
    var startVisible by renderProperty(true)
    var endVisible by renderProperty(true)

    var tangent by renderProperty(0.deg)
    var startTangentMag by renderProperty(-1.0)
    var endTangentMag by renderProperty(-1.0)
    var defaultTangentMag by renderProperty(70.0)

    val onChange = ArrayList<(SplineKnot) -> Unit>()

    init {
        bounds = Rectangle(1.0, 1.0)
        clipCanvasToBounds = false
        focusable = true
        focusChanged += { _, _, _ ->
            rerender()
        }

        Dragger(this).apply {
            var knotSelected = 0
            mouseDown = {
                knotSelected = when {
                    !hasFocus -> 0
                    startVisible && it in startKnot -> 1
                    endVisible && it in endKnot -> 2
                    else -> 0
                }
            }
            mouseDragged = { pos, delta ->
                if (knotSelected == 0) {
                    val extreme = Point(Field.fieldSize, Field.fieldSize) * 0.5
                    position = (position + delta).coerceIn(-extreme, extreme)
                    onChange.forEach { it(this@SplineKnot) }
                } else {
                    val vec = (pos).toVector2d() * if (knotSelected == 1) -1.0 else 1.0
                    val newAngle = vec.angle()
                    val newMag = vec.norm() / stretchFactor
                    tangent = newAngle
                    when (this@SplineKnot.mode) {
                        Mode.FIXED_LENGTH -> {
                            startTangentMag = -1.0
                            endTangentMag = -1.0
                        }
                        Mode.MATCH_LENGTH -> {
                            startTangentMag = newMag
                            endTangentMag = newMag
                        }
                        Mode.FREE_LENGTH -> {
                            if (knotSelected == 1) startTangentMag = newMag
                            else endTangentMag = newMag
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
        startKnot = Circle(
            Vector2d.polar(
                (if (startTangentMag >= 0) startTangentMag else defaultTangentMag) * -stretchFactor,
                tangent
            ).toPoint(), 1.0
        )
        endKnot = Circle(
            Vector2d.polar(
                (if (endTangentMag >= 0) endTangentMag else defaultTangentMag) * stretchFactor,
                tangent
            ).toPoint(), 1.0
        )

        if (!hasFocus) {
            canvas.circle(point, Color.Blue)
            return
        }

        if (startVisible) canvas.line(point.center, startKnot.center, Stroke(Color.Black, 0.2))
        if (endVisible) canvas.line(point.center, endKnot.center, Stroke(Color.Black, 0.2))

        canvas.circle(point, Color.Blue)

        if (startVisible) canvas.circle(startKnot, Stroke(Color.Black, 0.2), Color.Blue)
        if (endVisible) canvas.circle(endKnot, Stroke(Color.Black, 0.2), Color.Blue)
    }
}