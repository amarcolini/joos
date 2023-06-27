package field

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PathBuilder
import com.amarcolini.joos.path.heading.*
import com.amarcolini.joos.serialization.*
import com.amarcolini.joos.util.deg
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.LinearGradientPaint
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.path

object DraggableTrajectory : EntityGroup() {
    private val knots: MutableList<SplineKnot> = mutableListOf()

    enum class Mode {
        PATH,
        HEADIMG
    }

    private var mode: Mode = Mode.HEADIMG

    private var trajectory: SerializableTrajectory = SerializableTrajectory(
        TrajectoryStart(Pose2d()),
        mutableListOf(
            LinePiece(Vector2d(30.0, 30.0), SplineHeading(45.deg)),
            SplinePiece(Vector2d(40.0, 0.0), 30.deg)
        )
    )

    private val pathEntity: PathEntity = PathEntity(trajectory.createPath().path, Stroke(Color.Green))

    val currentPath: Path get() = pathEntity.path

    private fun update() {
        pathEntity.path = trajectory.createPath().path
    }

    fun disableEditing() {
        children.removeAll(knots)
        knots.clear()
    }

    fun initializePathEditing() {
        disableEditing()
        knots += PathKnot().apply {
            position = trajectory.start.pose.vec().toPoint()
            tangent = trajectory.start.tangent
            startVisible = false
//            endVisible = trajectory.pieces[0] is SplinePiece
            endVisible = true
            onChange += {
                trajectory.start.pose = Pose2d(it.position.toVector2d())
                trajectory.start.tangent = it.tangent
                update()
            }
        }
        trajectory.pieces.forEachIndexed { i, piece ->
            when (piece) {
                is SplinePiece -> knots += PathKnot().apply {
                    position = piece.end.toPoint()
                    tangent = piece.tangent
                    endTangentMag = piece.endTangentMag
                    startVisible = true
                    val nextSpline = trajectory.pieces.getOrNull(i + 1) as? SplinePiece
                    endVisible = nextSpline != null
                    onChange += {
                        piece.end = it.position.toVector2d()
                        piece.tangent = it.tangent
                        piece.endTangentMag = it.endTangentMag
                        nextSpline?.startTangentMag = it.endTangentMag
                        update()
                    }
                }
                is LinePiece -> knots += PathKnot().apply {
                    position = piece.end.toPoint()
                    tangentMode = SplineKnot.TangentMode.FIXED
                    startVisible = false
                    val nextSpline = trajectory.pieces.getOrNull(i + 1) as? SplinePiece
                    endVisible = lengthMode != SplineKnot.LengthMode.FIXED_LENGTH && nextSpline != null
                    onChange += {
                        piece.end = it.position.toVector2d()
                        nextSpline?.startTangentMag = it.endTangentMag
                        update()
                    }
                }
                else -> {}
            }
        }
        children += knots
        recomputeTransforms()
    }

    fun initializeHeadingEditing() {
        disableEditing()
        knots += HeadingKnot().apply {
            position = trajectory.start.pose.vec().toPoint()
            startVisible = false
            endVisible = true
            onChange += {
                trajectory.start.pose = Pose2d(it.position.toVector2d(), it.tangent)
                update()
            }
        }
        trajectory.pieces.forEachIndexed { i, piece ->
            if (piece !is MovableTrajectoryPiece) return@forEachIndexed
            val heading = piece.heading as? ValueHeading

            knots += HeadingKnot().apply {
                position = piece.end.toPoint()
                if (heading != null) tangent = heading.heading
                else endVisible = false
                onChange += {
                    piece.end = it.position.toVector2d()
                    heading?.heading = it.tangent
                    update()
                }
            }
        }
        children += knots
        recomputeTransforms()
    }

    init {
        children += pathEntity
        initializePathEditing()
    }
}