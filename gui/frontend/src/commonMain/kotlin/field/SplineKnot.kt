package field

import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.times
import io.nacular.doodle.core.Behavior
import io.nacular.doodle.core.behavior
import io.nacular.doodle.core.renderProperty
import io.nacular.doodle.drawing.*
import io.nacular.doodle.geometry.*
import io.nacular.doodle.system.SystemPointerEvent
import io.nacular.measured.units.Angle
import io.nacular.measured.units.Measure
import kotlin.math.roundToInt

class SplineKnot internal constructor() : FieldEntity() {
    init {
        bounds = Rectangle(4.0, 4.0)
        center = origin
    }

    internal var stretchFactor = 0.3
    var behavior by behavior<SplineKnot, SplineKnotBehavior>()
    init {
        behavior = DefaultSplineKnotBehavior()
    }

    private val point: Shape get() = behavior?.point!!
    private val startKnot: Shape get() = behavior?.startKnot!!
    private val endKnot: Shape get() = behavior?.endKnot!!
    private val headingArrow: Shape get() = behavior?.headingArrow!!

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
    var headingVisible by renderProperty(true)

    var isSpecial = false

    var tangent by renderProperty(0.deg)
    var startTangentMag by renderProperty(-1.0)
    var endTangentMag by renderProperty(-1.0)
    var defaultTangentMag by renderProperty(40.0)

    var heading by renderProperty(0.deg)

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
                    headingVisible && pos in headingArrow -> 3
                    else -> 0
                }
            }
            mouseDragged = { pos, delta ->
                if (isMove) {
                    when (knotSelected) {
                        0 -> {
                            val extreme = Point(Field.fieldSize, Field.fieldSize) * 0.5
                            position = (position + delta).coerceIn(-extreme, extreme).roundToNearest(0.1)
                            onChange.forEach { it(this@SplineKnot) }
                        }
                        3 -> {
                            heading = (pos).toVector2d().angle().degrees.roundToInt().deg
                        }
                        else -> {
                            val vec = (pos).toVector2d() * if (knotSelected == 1) -1.0 else 1.0
                            val newMag = vec.norm() / stretchFactor
                            when (this@SplineKnot.tangentMode) {
                                TangentMode.FREE, TangentMode.FIXED -> {
                                    val newAngle = vec.angle().degrees.roundToInt().deg
                                    tangent = newAngle
                                }
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
    }

    override fun intersects(point: Point): Boolean {
        val newPoint = point - position
        return if (!hasFocus) newPoint in this.point
        else {
            (startVisible && newPoint in startKnot) ||
                    (endVisible && newPoint in endKnot) ||
                    (headingVisible && newPoint in headingArrow) ||
                    newPoint in this.point
        }
    }

    override fun render(canvas: Canvas) {
        behavior?.render(this, canvas)
        onChange.forEach { it(this@SplineKnot) }
    }
}

interface SplineKnotBehavior : Behavior<SplineKnot> {
    val point: Shape
    val startKnot: Shape
    val endKnot: Shape
    val headingArrow: Shape

    override fun render(view: SplineKnot, canvas: Canvas)
}

open class DefaultSplineKnotBehavior : SplineKnotBehavior {
    override var point: Circle = Circle(Point.Origin, 1.75)
    override var startKnot: Circle = Circle(1.0)
    override var endKnot: Circle = Circle(1.0)
    val thickness = 2.0
    val arrowLength = 8.0
    val headLength = 2.5
    private lateinit var baseArrow: Rectangle
    override lateinit var headingArrow: ConvexPolygon

    override fun install(view: SplineKnot) {
        point = Circle(view.origin, 1.75)
        baseArrow = Rectangle(view.origin.x, view.origin.y + -thickness/2, arrowLength, thickness)
    }

    override fun render(view: SplineKnot, canvas: Canvas) {
        with(view) {
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
            headingArrow = AffineTransform2D().rotate(origin, Measure(heading.degrees, Angle.degrees)).invoke(baseArrow)

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
            if (headingVisible)  {
                    canvas.poly(headingArrow, Color.Red.opacity(0.5f).paint)
                    val end = arrowLength * heading.vec().toPoint() + origin
                    val angle = 45.deg
                    canvas.path(
                        path(origin)
                            .lineTo(end)
                            .lineTo(end - headLength * (angle * 0.5 + heading).vec())
                            .moveTo(end)
                            .lineTo(end - headLength * (-angle * 0.5 + heading).vec())
                            .finish(),
                        Stroke(Color.Cyan.paint, thickness,
                            lineJoint = Stroke.LineJoint.Round,
                            lineCap = Stroke.LineCap.Round
                        )
                    )
            }

            canvas.circle(point.withRadius(point.radius + 1.0), Color(66u, 135u, 245u, 0.8f))
            renderBase()

            if (startVisible) canvas.circle(startKnot, Stroke(Color.Black, 0.2), Color.Blue)
            if (endVisible) canvas.circle(endKnot, Stroke(Color.Black, 0.2), Color.Blue)
        }
    }

}