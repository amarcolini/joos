package com.amarcolini.joos.serialization

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.*
import com.amarcolini.joos.path.heading.HeadingInterpolation
import com.amarcolini.joos.path.heading.LinearInterpolator
import com.amarcolini.joos.path.heading.TangentHeading
import com.amarcolini.joos.path.heading.TangentInterpolator
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.constraints.TrajectoryConstraints
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface TrajectoryPiece

sealed interface MovableTrajectoryPiece {
    var end: Vector2d
    var heading: HeadingInterpolation
}

@Serializable
@SerialName("line")
data class LinePiece(
    override var end: Vector2d, override var heading: HeadingInterpolation = TangentHeading
) : TrajectoryPiece, MovableTrajectoryPiece

@Serializable
@SerialName("spline")
data class SplinePiece(
    override var end: Vector2d,
    var tangent: Angle,
    var startTangentMag: Double = -1.0,
    var endTangentMag: Double = -1.0,
    override var heading: HeadingInterpolation = TangentHeading
) : TrajectoryPiece, MovableTrajectoryPiece

@Serializable
@SerialName("turn")
data class TurnPiece(
    var angle: Angle
) : TrajectoryPiece

@Serializable
@SerialName("wait")
data class WaitPiece(
    var duration: Double
) : TrajectoryPiece

@Serializable
data class StartPiece(
    var pose: Pose2d,
    var tangent: Angle = pose.heading,
)

@Serializable
data class SerializableTrajectory(
    var start: StartPiece, val pieces: MutableList<TrajectoryPiece>
) {
    data class TrajectoryResult(
        val trajectory: Trajectory?,
        val errors: List<Pair<Exception, TrajectoryPiece?>>
    )

    data class PathResult(
        val path: Path,
        val errors: List<Pair<Exception, TrajectoryPiece?>>
    )

    fun createTrajectory(constraints: TrajectoryConstraints, resolution: Double = 0.25): TrajectoryResult {
        val errors = ArrayList<Pair<Exception, TrajectoryPiece?>>()
        val builder = TrajectoryBuilder(start.pose, start.tangent, constraints, resolution)
        pieces.forEach {
            try {
                when (it) {
                    is LinePiece -> builder.addLine(it.end, it.heading)
                    is SplinePiece -> builder.addSpline(
                        it.end, it.tangent, it.startTangentMag, it.endTangentMag, it.heading
                    )
                    is TurnPiece -> builder.turn(it.angle)
                    is WaitPiece -> builder.wait(it.duration)
                }
            } catch (e: Exception) {
                errors += e to it
            }
        }
        val trajectory = try {
            builder.build()
        } catch (e: Exception) {
            errors += e to null
            null
        }
        return TrajectoryResult(trajectory, errors)
    }

    fun createPath(): PathResult {
        val errors = ArrayList<Pair<Exception, TrajectoryPiece?>>()
        var builder = PathBuilder(start.pose, start.tangent)
        var currentPath = Path(emptyList())

        fun addToPath(path: Path) {
            currentPath = Path(currentPath.segments + path.segments)
        }

        fun splitCurrentPath(newHeading: (Angle) -> Angle = { it }, newTangent: (Angle) -> Angle = { it }) {
            addToPath(builder.preBuild())
            val lastSegment = currentPath.segments.last()
            val endOfLast = when (lastSegment.interpolator) {
                is TangentInterpolator, is LinearInterpolator -> lastSegment[0.0, 1.0]
                else -> lastSegment.end()
            }
            val newStart = endOfLast.copy(heading = newHeading(endOfLast.heading))
            builder = PathBuilder(
                newStart,
                newTangent(newStart.heading)
            )
        }

        fun tryAddPiece(piece: TrajectoryPiece) {
            when (piece) {
                is LinePiece -> builder.addLine(piece.end, piece.heading)
                is SplinePiece -> builder.addSpline(
                    piece.end, piece.tangent, piece.startTangentMag, piece.endTangentMag, piece.heading
                )
                is TurnPiece -> splitCurrentPath(newHeading = { it + piece.angle })
                is WaitPiece -> splitCurrentPath()
            }
        }

        pieces.forEach {
            try {
                tryAddPiece(it)
            } catch (e: PathBuilderException) {
                if (e is PathContinuityViolationException) {
                    splitCurrentPath()
                    try {
                        tryAddPiece(it)
                    } catch (e: PathBuilderException) {
                        errors += e to it
                    }
                } else errors += e to it
            }
        }
        addToPath(builder.preBuild())
        return PathResult(currentPath, errors)
    }
}