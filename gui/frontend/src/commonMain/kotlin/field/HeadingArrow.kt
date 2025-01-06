package field

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.times
import io.nacular.doodle.core.Behavior
import io.nacular.doodle.core.behavior
import io.nacular.doodle.core.renderProperty
import io.nacular.doodle.drawing.*
import io.nacular.doodle.geometry.*
import io.nacular.doodle.system.SystemPointerEvent
import io.nacular.measured.units.Measure
import kotlin.math.roundToInt

class HeadingArrow internal constructor() : FieldEntity() {
    init {
        bounds = Rectangle(4.0, 4.0)
        center = origin
    }

    class DefaultBehavior(
        val thickness: Double = 1.0,
        val length: Double = 5.0,
        val arrowLength: Double = 1.0,
        val arrowAngle: Angle = 45.deg
    ) : HeadingArrowBehavior {
        override val shape: Rectangle = Rectangle(0.0, -thickness / 2, thickness, length)

        override fun render(view: HeadingArrow, canvas: Canvas) {
            canvas.rect(shape, Color.Red.opacity(0.5f))
            val end = Point(length, 0.0)
            canvas.path(
                path(Point.Origin)
                    .lineTo(end)
                    .lineTo(end - arrowLength * (arrowAngle * 0.5).vec())
                    .moveTo(end)
                    .lineTo(end - arrowLength * (-arrowAngle * 0.5).vec())
                    .finish(),
                Stroke(Color.Cyan, thickness)
            )
        }
    }

    var behavior by behavior<HeadingArrow, HeadingArrowBehavior>()
    private val shape: Shape get() = behavior?.shape ?: Circle(1.0)

    var heading by renderProperty(0.deg)

    init {
        behavior = DefaultBehavior()
        clipCanvasToBounds = false
        focusChanged += { _, _, _ ->
            rerender()
        }

        Dragger(this).apply {
            var isMove = false
            mouseReleased = { _, _ ->
                isMove = false
            }
            mousePressed = { _, buttons ->
                isMove = buttons.contains(SystemPointerEvent.Button.Button1)
            }
            mouseDragged = { pos, _ ->
                if (isMove) heading = (pos).toVector2d().angle().degrees.roundToInt().deg
            }
        }
    }

    override fun intersects(point: Point): Boolean {
        val newPoint = point - position
        return newPoint in shape
    }

    override fun render(canvas: Canvas) {
        canvas.rotate(Measure(heading.degrees, io.nacular.measured.units.Angle.degrees)) {
            behavior?.render(this@HeadingArrow, this)
        }
    }
}

interface HeadingArrowBehavior : Behavior<HeadingArrow> {
    val shape: Shape
    override fun render(view: HeadingArrow, canvas: Canvas)
}