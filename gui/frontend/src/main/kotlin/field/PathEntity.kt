package field

import com.amarcolini.joos.path.Path
import com.amarcolini.joos.util.DoubleProgression
import io.nacular.doodle.core.renderProperty
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.ColorPaint
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.path
import kotlin.math.roundToInt

class PathEntity(path: Path) : FieldEntity() {
    var path by renderProperty(path)

    override var center: Point = Point()
        private set

    init {
        bounds = Rectangle(10, 10)
        clipCanvasToBounds = false
    }

    override fun render(canvas: Canvas) {
        val resolution = 4.0
        val derivLength = resolution * 0.3333333333333333
        val samples =
            DoubleProgression.fromClosedInterval(0.0, path.length(), (path.length() / resolution).roundToInt()).drop(1)
        val start = path.start().toPoint()
        center = start
        position = start
        val sampledPath = path(start)
        var lastDeriv = path.startDeriv().toPoint() * derivLength
        var lastPoint = start
        samples.forEach {
            val currentPoint = path[it].toPoint()
            val currentDeriv = path.deriv(it).toPoint() * derivLength
            sampledPath.cubicTo(currentPoint, lastPoint + lastDeriv, currentPoint - currentDeriv).finish()
            lastDeriv = currentDeriv
            lastPoint = currentPoint
        }
        canvas.path(
            sampledPath.finish(),
            Stroke(Color((0..255).random().toUByte(), (0..255).random().toUByte(), (0..255).random().toUByte()))
        )
    }
}