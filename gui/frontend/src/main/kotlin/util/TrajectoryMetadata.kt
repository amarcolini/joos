package util

import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.serialization.*
import field.SplineKnot
import field.toPoint
import io.nacular.doodle.geometry.Point

data class TrajectoryMetadata(
    val startData: StartPieceWithData,
    val pieceData: MutableList<PieceWithData>
) {
    companion object {
        fun fromTrajectory(
            trajectory: SerializableTrajectory,
            lengthMode: SplineKnot.LengthMode = SplineKnot.LengthMode.FIXED_LENGTH
        ): TrajectoryMetadata {
            val startData = StartPieceWithData(trajectory.start, lengthMode)
            var currPos = { startData.start.pose.vec() }
            val pieceData = trajectory.pieces.map {
                when (it) {
                    is LinePiece -> {
                        currPos = it::end
                        PieceWithData(it, it::end)
                    }
                    is SplinePiece -> {
                        currPos = it::end
                        SplinePieceWithData(it, lengthMode)
                    }
                    else -> PieceWithData(it, currPos)
                }
            }
            return TrajectoryMetadata(startData, pieceData.toMutableList())
        }

        infix fun SplinePiece.with(mode: SplineKnot.LengthMode) = SplinePieceWithData(this, mode)

        fun LinePiece.withData() = PieceWithData(this, this::end)
    }

    fun recomputeKnotPositions() {
        var currPos = { startData.start.pose.vec() }
        pieceData.forEach {
            when (it.trajectoryPiece) {
                is LinePiece -> currPos = it.knotPosition
                is SplinePiece -> currPos = it.knotPosition
                else -> it.knotPosition = currPos
            }
        }
    }

    open class PieceWithData(
        open val trajectoryPiece: TrajectoryPiece,
        var knotPosition: () -> Vector2d
    )

    data class StartPieceWithData(
        val start: TrajectoryStart,
        var lengthMode: SplineKnot.LengthMode,
    )

    class SplinePieceWithData(
        override val trajectoryPiece: SplinePiece,
        var lengthMode: SplineKnot.LengthMode
    ) : PieceWithData(trajectoryPiece, trajectoryPiece::end)

    fun getTrajectory() = SerializableTrajectory(startData.start, pieceData.map {
        it.trajectoryPiece
    }.toMutableList())
}