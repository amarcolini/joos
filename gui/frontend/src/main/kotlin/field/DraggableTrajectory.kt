package field

import GUIApp
import GUIApp.Companion.modalManager
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.heading.SplineHeading
import com.amarcolini.joos.path.heading.ValueHeading
import com.amarcolini.joos.serialization.*
import com.amarcolini.joos.util.deg
import io.nacular.doodle.controls.modal.ModalManager
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.event.PointerListener
import io.nacular.doodle.layout.constraints.Strength.Companion.Strong
import io.nacular.doodle.system.SystemPointerEvent
import io.nacular.doodle.utils.addOrAppend
import kotlinx.coroutines.launch
import settings.SplineKnotMenu
import util.TrajectoryMetadata
import util.TrajectoryMetadata.Companion.withData
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

object DraggableTrajectory : EntityGroup() {
    private val knots: MutableList<SplineKnot> = mutableListOf()

    enum class Mode {
        View,
        EditHeading,
        EditPath
    }

    var mode: Mode by Delegates.observable(Mode.EditPath) { _, old, new ->
        if (old == new) return@observable
        when (new) {
            Mode.View -> disableEditing()
            Mode.EditHeading -> initializeHeadingEditing()
            Mode.EditPath -> initializePathEditing()
        }
    }

    private var trajectory: TrajectoryMetadata = TrajectoryMetadata.fromTrajectory(
        SerializableTrajectory(
            TrajectoryStart(Pose2d()),
            mutableListOf(
                LinePiece(Vector2d(30.0, 30.0), SplineHeading(45.deg)),
                SplinePiece(Vector2d(40.0, 0.0), 30.deg)
            )
        )
    )

    private val pathEntity: PathEntity = PathEntity(trajectory.getTrajectory().createPath().path, Stroke(Color.Green))

    val currentPath: Path get() = pathEntity.path

    private fun update() {
        pathEntity.path = trajectory.getTrajectory().createPath().path
    }

    fun disableEditing() {
        children.removeAll(knots)
        knots.clear()
    }

    private fun addAfter(trajectoryPiece: TrajectoryMetadata.PieceWithData, new: TrajectoryMetadata.PieceWithData) {
        val index = trajectory.pieceData.indexOf(trajectoryPiece)
        if (index < 0) return
        trajectory.pieceData.addOrAppend(
            index,
            new
        )
        update()

    }

    fun addSplineAfter(trajectoryPiece: TrajectoryMetadata.PieceWithData) {

    }

    fun addLineAfter(trajectoryPiece: TrajectoryMetadata.PieceWithData) {
        val index = trajectory.pieceData.indexOf(trajectoryPiece)
        if (index < 0) return
        trajectory.pieceData.addOrAppend(
            index,
            LinePiece(trajectoryPiece.knotPosition()).withData()
        )
    }

    private fun initializePathEditing() {
        disableEditing()
        knots += PathKnot().apply {
            position = trajectory.startData.start.pose.vec().toPoint()
            tangent = trajectory.startData.start.tangent
            startVisible = false
//            endVisible = trajectory.pieces[0] is SplinePiece
            endVisible = true
            onChange += {
                trajectory.startData.start.pose = Pose2d(it.position.toVector2d())
                trajectory.startData.start.tangent = it.tangent
                update()
            }
        }
        trajectory.pieceData.forEachIndexed { i, pieceData ->
            when (val piece = pieceData.trajectoryPiece) {
                is SplinePiece -> knots += PathKnot().apply {
                    position = piece.end.toPoint()
                    tangent = piece.tangent
                    startTangentMag = piece.endTangentMag
                    startVisible = true
                    val nextSpline = trajectory.pieceData.getOrNull(i + 1)?.trajectoryPiece as? SplinePiece
                    endVisible = nextSpline != null
                    nextSpline?.startTangentMag?.let { endTangentMag = it }
                    pointerChanged += PointerListener.pressed { event ->
                        if (SystemPointerEvent.Button.Button2 in event.buttons && SystemPointerEvent.Button.Button1 !in event.buttons) {
                            GUIApp.appScope.launch {
                                modalManager {
                                    val menu = SplineKnotMenu.create(pieceData, this@apply) { completed(Unit) }
                                    pointerOutsideModalChanged += PointerListener.pressed {
                                        completed(Unit)
                                    }
                                    ModalManager.RelativeModal(menu, this@apply) { modal, knot ->
                                        (modal.top eq knot.bottom + 10)..Strong
                                        (modal.top greaterEq 5)..Strong
                                        (modal.left greaterEq 5)..Strong
                                        (modal.centerX eq knot.center.x)..Strong

                                        modal.right lessEq parent.right - 5

                                        when {
                                            parent.height.readOnly - knot.bottom > modal.height.readOnly + 15 -> modal.bottom lessEq parent.bottom - 5
                                            else -> modal.bottom lessEq knot.y - 10
                                        }

                                        modal.width.preserve
                                        modal.height.preserve
                                    }
                                }
                            }
                        }
                    }
                    onChange += {
                        piece.end = it.position.toVector2d()
                        piece.tangent = it.tangent
                        piece.endTangentMag = it.startTangentMag
                        nextSpline?.startTangentMag = it.endTangentMag
                        update()
                    }
                }
                is LinePiece -> knots += PathKnot().apply {
                    position = piece.end.toPoint()
                    tangentMode = SplineKnot.TangentMode.FIXED
                    startVisible = false
                    val nextSpline = trajectory.pieceData.getOrNull(i + 1)?.trajectoryPiece as? SplinePiece
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

    private fun initializeHeadingEditing() {
        disableEditing()
        knots += HeadingKnot().apply {
            position = trajectory.startData.start.pose.vec().toPoint()
            startVisible = false
            endVisible = true
            onChange += {
                trajectory.startData.start.pose = Pose2d(it.position.toVector2d(), it.tangent)
                update()
            }
        }
        trajectory.pieceData.forEachIndexed { _, pieceData ->
            val piece = pieceData.trajectoryPiece
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