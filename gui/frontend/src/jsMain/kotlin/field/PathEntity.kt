package field

import com.amarcolini.joos.path.LineSegment
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.QuinticSpline
import com.amarcolini.joos.util.DoubleProgression
import io.nacular.doodle.core.renderProperty
import io.nacular.doodle.drawing.*
import io.nacular.doodle.geometry.Circle
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.path
import kotlinx.browser.window

class PathEntity(path: Path, stroke: Stroke) : FieldEntity() {
    var path by renderProperty(path)
    var stroke by renderProperty(stroke)

    init {
        bounds = Rectangle(10, 10)
        clipCanvasToBounds = false
        window.asDynamic()["derivMult"] = 0.0
    }

    override fun render(canvas: Canvas) {
        val start = path.internalGet(0, 0.0).vec()
        val sampledPath = path(start.toPoint())
        path.segments.forEach { segment ->
            when (val curve = segment.curve) {
                is QuinticSpline -> {
//                    val estimatedLength =
//                        curve.x.controlPoints.zip(curve.y.controlPoints)
//                            .zipWithNext { (firstX, firstY), (secondX, secondY) ->
//                                Vector2d(firstX, firstY) to Vector2d(secondX, secondY)
//                            }.sumOf { it.first.distTo(it.second) }
//                    val numSamples = max(5.0, estimatedLength * 0.03).roundToInt()
                    val numSamples = 10
                    val samples =
                        DoubleProgression.fromClosedInterval(0.0, 1.0, numSamples)
                    var lastPoint = segment[0.0, 0.0].vec()
                    var lastDeriv = segment.internalDeriv(0.0, 0.0).vec()
                    samples.drop(1).forEach {
                        val currentPoint = segment[0.0, it].vec()
                        val currentDeriv = segment.internalDeriv(0.0, it).vec()
                        sampledPath.cubicTo(
                            currentPoint.toPoint(),
                            (lastPoint + lastDeriv * (samples.step / 3.0)).toPoint(),
                            (currentPoint - currentDeriv * (samples.step / 3.0)).toPoint()
                        )
                        lastDeriv = currentDeriv
                        lastPoint = currentPoint
                    }
                }
                is LineSegment -> sampledPath.lineTo(curve[0.0, 1.0].toPoint())
            }
        }
        canvas.path(
            sampledPath.finish(),
            stroke
        )
        for (i in 1..path.segments.lastIndex) {
            canvas.circle(Circle(path.internalGet(i, 0.0).toPoint(), 1.0), Color.Black.paint)
        }
        canvas.circle(Circle(start.toPoint(), 1.3), Color.Green.darker().paint)
        canvas.circle(Circle(path.segments.last()[0.0, 1.0].toPoint(), 1.3), Color.Red.darker().paint)
    }
}