package particle

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.geometry.Rectangle
import particle.SensingPlayground.center
import kotlin.math.abs
import kotlin.math.min

class Simple : SensingPlayground.SensorAlgorithm() {
    override val render: (Canvas) -> Unit = {
        it.rect(rectangle, Stroke(Color.Green))
        drawPoseEstimate(it, estimate, Color.Green)
    }
    private var rectangle: Rectangle = Rectangle.Empty
    private var estimate: Pose2d = Pose2d()

    override fun update(distances: List<Double>, headingEstimate: Angle) {
        val sensorVectors = SensingPlayground.sensorAngles.zip(distances).map {
            (Vector2d.polar(it.second, it.first.heading) + it.first.vec()).rotated(headingEstimate)
        }

        val x1 = sensorVectors.maxOf { -SensingPlayground.half - it.x }
        val x2 = sensorVectors.minOf { SensingPlayground.half - it.x }
        val y1 = sensorVectors.maxOf { -SensingPlayground.half - it.y }
        val y2 = sensorVectors.minOf { SensingPlayground.half - it.y }

        rectangle = Rectangle(min(x1, x2) + center.x, min(y1, y2) + center.y, abs(x1 - x2), abs(y1 - y2))
        estimate = Pose2d(Vector2d(x1 + x2, y1 + y2) * 0.5, headingEstimate)
    }
}