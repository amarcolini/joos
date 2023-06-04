package field

import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.rad
import com.amarcolini.joos.util.times
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.ColorPaint
import io.nacular.doodle.geometry.Circle
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle
import kotlin.math.PI
import kotlin.math.min

class WaypointPopup : FieldEntity() {
    init {
        bounds = Rectangle(30.0, 30.0)
    }

    open class Item : View() {
        init {
            bounds = Rectangle(5.0, 5.0)
        }

        override fun render(canvas: Canvas) {
            canvas.circle(
                Circle(origin, min(bounds.width, bounds.height) * 0.5), ColorPaint(Color.Black)
            )
        }
    }

    init {
        children += listOf(Item(), Item(), Item())
        position = Point(0.0, 10.0)
        children.forEachIndexed { i, view ->
            view.cPos = Vector2d.polar(8.0, 2.0 * PI.rad * (i.toDouble() / children.size))
                .toPoint()
        }
    }

}