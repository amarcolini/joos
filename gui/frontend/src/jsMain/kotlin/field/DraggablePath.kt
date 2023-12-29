package field

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.path.PathBuilder
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.LinearGradientPaint
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.geometry.Point

object DraggablePath : EntityGroup() {
    private val start = PathKnot().apply {
        startVisible = false
        lengthMode = SplineKnot.LengthMode.FREE_LENGTH
    }

    private val end = PathKnot().apply {
        endVisible = false
        lengthMode = SplineKnot.LengthMode.FIXED_LENGTH
        position = Point(30.0, 30.0)
    }

    private val path = PathEntity(
        PathBuilder(Pose2d(start.position.toVector2d(), start.tangent))
            .addSpline(end.position.toVector2d(), end.tangent, start.endTangentMag, end.startTangentMag)
            .preBuild(),
        Stroke(LinearGradientPaint(Color.Red, Color.Green, start.position, end.position))
    )

    init {
        val update = {
            path.path = PathBuilder(Pose2d(start.position.toVector2d(), start.tangent))
                .addSpline(end.position.toVector2d(), end.tangent, start.endTangentMag, end.startTangentMag)
                .preBuild()
            path.stroke = Stroke(LinearGradientPaint(Color.Red, Color.Green, start.position, end.position))
        }
        start.onChange += { update() }
        end.onChange += { update() }
    }

//    override val children: ObservableList<View> = ObservableList(listOf(path, start, end))
    init {
        this.children += listOf(path, start, end)
    }
}