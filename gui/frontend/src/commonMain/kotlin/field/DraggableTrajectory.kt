package field

import GUIApp
import GUIApp.Companion.focusManager
import GUIApp.Companion.menus
import GUIApp.Companion.modalManager
import GUIApp.Companion.popupManager
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.heading.SplineHeading
import com.amarcolini.joos.path.heading.ValueHeading
import com.amarcolini.joos.serialization.*
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.constraints.*
import com.amarcolini.joos.util.deg
import getLocalStorageItem
import io.nacular.doodle.controls.modal.ModalManager
import io.nacular.doodle.controls.popupmenu.MenuCreationContext
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.event.PointerListener
import io.nacular.doodle.layout.constraints.Strength.Companion.Strong
import io.nacular.doodle.system.SystemPointerEvent
import io.nacular.doodle.utils.addOrAppend
import kotlinx.coroutines.launch
import kotlinx.serialization.Transient
import settings.KnotMenu
import util.NumberField
import util.TrajectoryMetadata
import util.TrajectoryMetadata.Companion.with
import util.TrajectoryMetadata.Companion.withData
import kotlin.jvm.JvmStatic
import kotlin.properties.Delegates

class DraggableTrajectory(var data: TrajectoryMetadata) : EntityGroup() {
    companion object {
        @JvmStatic
        internal fun loadFromStorage(): DraggableTrajectory? {
            return getLocalStorageItem(GUIApp.trajectoryKey)?.let {
                try {
                    DraggableTrajectory(TrajectoryMetadata.fromTrajectory(SerializableTrajectory.fromJSON(it)))
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private val knots: MutableList<SplineKnot> = mutableListOf()

    enum class Mode {
        View,
        EditPath
    }

    var mode: Mode by Delegates.observable(Mode.EditPath) { _, _, new ->
        when (new) {
            Mode.View -> disableEditing()
            Mode.EditPath -> initializePathEditing()
        }
    }

    val numPieces get() = data.pieceData.size

    private val pathEntity: PathEntity =
        PathEntity(this.data.serializableTrajectory().createPath().path, Stroke(Color.Green))

    val currentPath: Path? get() = pathEntity.path

    data class TempGenericConstraints(
        var maxVel: Double = 30.0,
        var maxAccel: Double = 30.0,
        override var maxAngVel: Angle = 180.deg,
        override var maxAngAccel: Angle = 180.deg,
        override var maxAngJerk: Angle = 0.deg
    ) : TrajectoryConstraints {
        @Transient
        override val velConstraint
            get() = MinVelocityConstraint(
                listOf(
                    TranslationalVelocityConstraint(maxVel),
                    AngularVelocityConstraint(maxAngVel),
                    AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
                )
            )

        @Transient
        override val accelConstraint
            get() = MinAccelerationConstraint(
                listOf(
                    TranslationalAccelerationConstraint(maxAccel),
                    AngularAccelerationConstraint(maxAngAccel)
                )
            )

        @Transient
        override val decelConstraint: TrajectoryAccelerationConstraint
            get() = accelConstraint
    }

    val constraints: TempGenericConstraints = TempGenericConstraints()

    var currentTrajectory: Trajectory? = null
        private set
        get() {
            if (!trajectoryIsUpdated) {
                field = data.serializableTrajectory().createTrajectory(
                    constraints
                ).trajectory
                trajectoryIsUpdated = true
            }
            return field
        }
    private var trajectoryIsUpdated = false

    fun recomputeTrajectory() {
        trajectoryIsUpdated = false
        currentTrajectory
    }

    private fun update() {
        pathEntity.path = data.serializableTrajectory().createPath().path
        trajectoryIsUpdated = false
    }

    fun serializableTrajectory() = data.serializableTrajectory()

    fun toJSON(): String {
        return serializableTrajectory().toJSON().replace(Regex("(?<=\\d\\.\\d)\\d+"), "")
    }

    private fun delete(index: Int) {
        if (index < 0 || index >= data.pieceData.size) return
        data.pieceData.removeAt(index)
        update()
        if (mode == Mode.EditPath)
            updateZIndices(index)
    }

    private fun insert(
        index: Int,
        new: TrajectoryMetadata.PieceWithData,
    ) {
        val actualIndex = index.coerceIn(0, data.pieceData.size)
        data.pieceData.addOrAppend(
            actualIndex,
            new
        )
        update()
        val knot = when (mode) {
            Mode.EditPath -> createPathKnot(new, actualIndex)
            Mode.View -> null
        }
        if (knot != null) {
            knots += knot
            updateZIndices(actualIndex)
        }
    }

    private fun updateZIndices(startingIndex: Int = 0) {
        for (i in startingIndex..knots.lastIndex) {
            knots[i].zOrder = i
        }
    }

    private fun makeSpline(position: Vector2d) =
        SplinePiece(position, 0.deg) with SplineKnot.LengthMode.FIXED_LENGTH

    private fun makeLine(position: Vector2d) =
        LinePiece(position, SplineHeading(0.deg)).withData()

    private fun addMenu(
        knot: SplineKnot,
        menu: (completed: (Unit) -> Unit) -> View
    ) {
        val listener = PointerListener.pressed { event ->
            if (SystemPointerEvent.Button.Button2 in event.buttons && SystemPointerEvent.Button.Button1 !in event.buttons) {
                println("launching coroutine!")
                GUIApp.appScope.launch {
                    println("launching modal manager!")
                    modalManager {
                        pointerOutsideModalChanged += PointerListener.pressed {
                            completed(Unit)
                        }
                        println("instantiating modal!")
                        ModalManager.RelativeModal(menu(this::completed), knot) { modal, knot ->
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
                        }.also { println("done!") }
                    }
                }
            }
        }
        knot.pointerChanged += listener
    }

    private fun makeMenu(
        knot: SplineKnot,
        piece: MovableTrajectoryPiece?,
        customMenu: MenuCreationContext.() -> Unit
    ) = addMenu(knot) {
        val innerMenu = menus({ it(Unit) }, customMenu)
        KnotMenu.createPathKnotMenu(
            listOf(innerMenu),
            knot,
            piece,
            knot.startVisible || knot.endVisible
        )
    }

    private fun makeMenu(
        knot: SplineKnot,
        piece: MovableTrajectoryPiece?,
        pieceIndex: Int,
        insertAfter: Boolean = true,
        insertBefore: Boolean = true,
        deletable: Boolean = true,
        stationaryPieces: List<StationaryTrajectoryPiece> = emptyList()
    ) = makeMenu(knot, piece) {
        val knotPosition = knot.position.toVector2d()
        if (insertAfter) menu("Insert After") {
            action("Spline") { insert(pieceIndex + 1, makeSpline(knotPosition)) }
            action("Line") { insert(pieceIndex + 1, makeLine(knotPosition)) }
            fun addStationaryPiece(piece: StationaryTrajectoryPiece) {
                data.pieceData.getOrNull(pieceIndex)?.stationaryPieces?.plusAssign(piece)
                    ?: data.startData.stationaryPieces.add(piece)
            }
            action("Turn") { addStationaryPiece(TurnPiece(0.deg)) }
            action("Wait") { addStationaryPiece(WaitPiece(0.0)) }
        }
        if (insertBefore) menu("Insert Before") {
            action("Spline") { insert(pieceIndex - 1, makeSpline(knotPosition)) }
            action("Line") { insert(pieceIndex - 1, makeLine(knotPosition)) }
        }
        if (stationaryPieces.isNotEmpty()) menu("Edit Pieces") {
            stationaryPieces.forEach { piece ->
                when (piece) {
                    is TurnPiece -> {
                        val decimals = 0
                        val initialValue = piece.angle.degrees
                        action("Turn ${initialValue.format(decimals)}°") { _ ->
                            val field = NumberField(
                                initialValue,
                                decimals,
                                true,
                                { piece.angle = it.deg },
                                format = { "${it}°" }
                            )
                            popupManager.show(field, relativeTo = knot) { parent, bounds ->
                                parent.center eq bounds.center
                            }
                            field.focusChanged += { _, _, new ->
                                if (!new) popupManager.hide(field)
                                else field.selectAll()
                            }
                            focusManager.requestFocus(field)
                        }
                    }

                    is WaitPiece -> {
                        val decimals = 1
                        val initialValue = piece.duration
                        action("Wait ${initialValue.format(decimals)}s") { _ ->
                            val field =
                                NumberField(initialValue, decimals, false, piece::duration::set, format = { "${it}s" })
                            popupManager.show(field, relativeTo = knot) { parent, bounds ->
                                parent.center eq bounds.center
                            }
                            field.focusChanged += { _, _, new ->
                                if (!new) popupManager.hide(field)
                                else field.selectAll()
                            }
                            focusManager.requestFocus(field)
                        }
                    }

                    else -> {}
                }
            }
        }
        if (!(!deletable || pieceIndex < 0 || pieceIndex >= data.pieceData.size)) action("Delete") {
            delete(pieceIndex)
        }
    }

    private fun createPathKnot(
        pieceData: TrajectoryMetadata.PieceWithData,
        index: Int = data.pieceData.indexOf(pieceData)
    ): SplineKnot {
        val nextSpline = data.pieceData.getOrNull(index + 1)?.piece as? SplinePiece
        val initialHeading = pieceData.piece.heading as? ValueHeading
        val knot = SplineKnot().apply {
            position = pieceData.piece.end.toPoint()
            tangentMode = SplineKnot.TangentMode.FIXED
            if (initialHeading != null) {
                headingVisible = true
                this.heading = initialHeading.target
            } else headingVisible = false
            onChange += {
                val newHeading = (pieceData.piece.heading as? ValueHeading)?.apply {
                    this.target = it.heading
                }
                it.headingVisible = newHeading != null
                update()
            }
        }
        when (pieceData) {
            is TrajectoryMetadata.SplinePieceWithData -> knot.apply {
                tangent = pieceData.piece.tangent
                startTangentMag = pieceData.piece.endTangentMag
                lengthMode = pieceData.lengthMode
                startVisible = true
                endVisible = nextSpline != null
                nextSpline?.startTangentMag?.let { endTangentMag = it }
                onChange += {
                    pieceData.piece.tangent = it.tangent
                    pieceData.piece.endTangentMag = it.startTangentMag
                    pieceData.piece.end = it.position.toVector2d()
                    nextSpline?.startTangentMag = it.endTangentMag
                    update()
                }
            }

            is TrajectoryMetadata.LinePieceWithData -> knot.apply {
                startVisible = false
                endVisible = lengthMode != SplineKnot.LengthMode.FIXED_LENGTH && nextSpline != null
                onChange += {
                    pieceData.piece.end = it.position.toVector2d()
                    nextSpline?.startTangentMag = it.endTangentMag
                    update()
                }
            }
        }
        knot.isSpecial = pieceData.stationaryPieces.isNotEmpty()
        knot.zOrder = index
        makeMenu(
            knot,
            pieceData.piece,
            index,
            stationaryPieces = pieceData.stationaryPieces
        )
        return knot
    }

    private fun initializePathEditing() {
        disableEditing()
        knots += SplineKnot().apply {
            val startData = data.startData
            val startPos = startData.start.pose::vec
            position = startPos().toPoint()
            tangent = startData.start.tangent
            heading = startData.start.pose.heading
            startVisible = false
            endVisible = data.pieceData.getOrNull(0)?.piece is SplinePiece
            headingVisible = true
            onChange += {
                startData.start.pose = Pose2d(it.position.toVector2d(), it.heading)
                startData.start.tangent = it.tangent
                update()
            }
            isSpecial = startData.stationaryPieces.isNotEmpty()
            makeMenu(
                this, null,-1,
                insertAfter = true,
                insertBefore = false,
                deletable = false,
                stationaryPieces = startData.stationaryPieces
            )
        }

        data.pieceData.forEachIndexed { i, pieceData ->
            knots += createPathKnot(pieceData, i)
        }
        children += knots
        recomputeTransforms()
    }

    private fun disableEditing() {
        children.removeAll(knots)
        knots.clear()
    }

    init {
        children += pathEntity
        initializePathEditing()
    }
}