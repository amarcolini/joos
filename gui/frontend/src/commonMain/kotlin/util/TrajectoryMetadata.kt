package util

import com.amarcolini.joos.serialization.*
import field.SplineKnot

class TrajectoryMetadata(
    val startData: StartPieceWithData,
    val pieceData: MutableList<PieceWithData>
) {
    companion object {
        fun fromTrajectory(
            trajectory: SerializableTrajectory,
            lengthMode: SplineKnot.LengthMode = SplineKnot.LengthMode.FIXED_LENGTH
        ): TrajectoryMetadata {
            val startData = StartPieceWithData(trajectory.start, lengthMode)
            var currentPiece: PieceWithData? = null
            val pieceData = mutableListOf<PieceWithData>()
            for (piece in trajectory.pieces) {
                when (piece) {
                    is LinePiece -> piece.withData().also {
                        pieceData += it
                        currentPiece = it
                    }

                    is SplinePiece -> piece.with(lengthMode).also {
                        pieceData += it
                        currentPiece = it
                    }

                    is StationaryTrajectoryPiece -> currentPiece?.stationaryPieces?.plusAssign(piece)
                        ?: startData.stationaryPieces.add(piece)
                }
            }
            return TrajectoryMetadata(startData, pieceData)
        }

        infix fun SplinePiece.with(mode: SplineKnot.LengthMode) = SplinePieceWithData(this, mode)

        fun LinePiece.withData() = LinePieceWithData(this)
    }

    sealed class PieceWithData {
        abstract val piece: MovableTrajectoryPiece
        val stationaryPieces: MutableList<StationaryTrajectoryPiece> = mutableListOf()
    }

    data class StartPieceWithData(
        val start: StartPiece,
        var lengthMode: SplineKnot.LengthMode,
        val stationaryPieces: MutableList<StationaryTrajectoryPiece> = mutableListOf()
    )

    data class SplinePieceWithData(
        override val piece: SplinePiece,
        var lengthMode: SplineKnot.LengthMode
    ) : PieceWithData()

    data class LinePieceWithData(
        override val piece: LinePiece
    ) : PieceWithData()

    fun serializableTrajectory() = SerializableTrajectory(startData.start, pieceData.map {
        it.piece
    }.toMutableList())
}