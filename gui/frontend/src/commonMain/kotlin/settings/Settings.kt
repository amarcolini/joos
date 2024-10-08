package settings

import GUIApp
import GUIApp.Companion.appScope
import GUIApp.Companion.imageLoader
import GUIApp.Companion.modalManager
import animation.ScrubBar
import animation.TimeManager
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.serialization.SerializableTrajectory
import com.amarcolini.joos.serialization.StartPiece
import field.DraggableTrajectory
import field.Field
import field.Robot
import io.nacular.doodle.controls.IndexedItem
import io.nacular.doodle.controls.ItemVisualizer
import io.nacular.doodle.controls.StringVisualizer
import io.nacular.doodle.controls.StyledTextVisualizer
import io.nacular.doodle.controls.buttons.PushButton
import io.nacular.doodle.controls.dropdown.SelectBox
import io.nacular.doodle.controls.form.verticalLayout
import io.nacular.doodle.controls.modal.ModalManager
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.lighter
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.event.*
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.HorizontalFlowLayout
import io.nacular.doodle.layout.Insets
import io.nacular.doodle.layout.constraints.Strength
import io.nacular.doodle.layout.constraints.constrain
import io.nacular.doodle.text.StyledText
import io.nacular.doodle.theme.basic.dropdown.BasicSelectBoxBehavior
import io.nacular.doodle.utils.Dimension
import io.nacular.doodle.utils.ObservableList
import io.nacular.doodle.utils.VerticalAlignment
import io.nacular.doodle.utils.observable
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import setLocalStorageItem
import util.BetterViewBuilder.Companion.padding
import util.BetterViewBuilder.Companion.viewBuilder
import util.NumberField
import util.TrajectoryMetadata
import kotlin.math.min

internal object Settings : View() {
    val trajectories = ObservableList(arrayListOf<DraggableTrajectory>())

    init {
        trajectories.changed += { list: ObservableList<DraggableTrajectory>, diffs ->
            trajectorySelect.visible = list.size > 1
            if (list.isNotEmpty() && currentTrajectory?.let { list.contains(it) } != true) {
                currentTrajectory = trajectories.first()
            }
            currentTrajectory?.visible = true
            updateControlSelection()
        }
    }

    private var currentTrajectory: DraggableTrajectory? by observable(null) { old, new ->
        if (old != null) {
            Field.children -= old
            old.visible = false
        }
        if (new != null) {
            new.visible = true
            Field.children += new
        }
        val valid = new != null
        if (!valid) hideTrajectoryMenu()
//        val index = trajectories.indexOf(new)
//        if (index != -1) {
//            trajectorySelect.selection = index
//        }
        trajectoryConfigMenu.visible = valid
        exportButton.visible = valid
        control.visible = valid
        updateControlSelection()
    }

    private val trajectorySelect = SelectBox(
        trajectories, object : ItemVisualizer<DraggableTrajectory, IndexedItem> {
            private val delegate = StringVisualizer(setOf(Dimension.Height, Dimension.Width))
            override fun invoke(item: DraggableTrajectory, previous: View?, context: IndexedItem): View {
                return delegate.invoke("Trajectory ${context.index + 1}", previous, context)
            }
        }
    )
        .apply {
            BasicSelectBoxBehavior
            size = Size(160, 35)
            idealSize = size
            changed += {
                currentTrajectory = trajectories[it.selection]
            }
        }

    private val fieldImageButton = PushButton("Change Field Image").apply {
        visible = true
        size = Size(160, 35)
        idealSize = size
        pressedChanged += { _, _, new ->
            if (new) {
                appScope.launch {
                    val url = modalManager {
                        val popup = FieldImageMenu(::completed)
                        pointerOutsideModalChanged += PointerListener.pressed {
                            completed(null)
                        }
                        ModalManager.RelativeModal(popup, this@apply) { modal, view ->
                            (modal.top eq view.bottom + 10)..Strength.Strong
                            (modal.top greaterEq 5)..Strength.Strong
                            (modal.left greaterEq 5)..Strength.Strong
                            (modal.centerX eq view.center.x)..Strength.Strong

                            modal.right lessEq parent.right - 5

                            when {
                                parent.height.readOnly - view.bottom > modal.height.readOnly + 15 -> modal.bottom lessEq parent.bottom - 5
                                else -> modal.bottom lessEq view.y - 10
                            }

                            modal.width.preserve
                            modal.height.preserve
                        }
                    }
                    //                val url = window.prompt("Enter image URL:", "https://")
//                    val url: String? =
//                        "https://preview.redd.it/custom-centerstage-field-diagrams-works-with-meepmeep-v0-uqcy8o9sfpob1.png?width=1080&crop=smart&auto=webp&s=73ae9941723772ab9c6cb322f28fb0c66a7465aa"
                    if (url != null) {
                        setLocalStorageItem(GUIApp.fieldImageKey, url)
                        imageLoader.load(GUIApp.parseURL(url))?.let {
                            Field.backgrounds["Generic"] = it
                            Field.rerender()
                        }
                    }
                }
            }
        }
    }

    private val exportButton = PushButton("Export Trajectory").apply {
        visible = true
        size = Size(160, 35)
        idealSize = size
        pressedChanged += { _, _, new ->
            if (new) {
                val json = currentTrajectory?.serializableTrajectory()?.toJSON()
                if (json != null) {
                    setLocalStorageItem(GUIApp.trajectoryKey, json)
//                window.alert(json)
                }
            }
        }
    }

    fun hideTrajectoryMenu() {
        ScrubBar.visible = false
        trajectoryConfigMenu.visible = false
        Robot.visible = false
        Robot.rerenderNow()
        TimeManager.listeners -= trajectoryListener
        TimeManager.setTime(0.0, false)
    }

    fun showTrajectoryMenu(trajectory: DraggableTrajectory): Boolean {
        val compiled = trajectory.currentTrajectory ?: return false
        if (trajectory != currentTrajectory) currentTrajectory = trajectory
        TimeManager.duration = compiled.duration()
        TimeManager.setTime(0.0, false)
        TimeManager.listeners += trajectoryListener
        Robot.pose = compiled.start()
        ScrubBar.visible = true
        trajectoryConfigMenu.visible = true
        Robot.visible = true
        Robot.pose = trajectory.currentPath?.start() ?: trajectory.data.startData.start.pose
        ScrubBar.rerenderNow()
        Robot.rerenderNow()
        return true
    }

    private val trajectoryListener: (Double, Double) -> Unit = { _, new ->
        val current = currentTrajectory?.currentTrajectory
        if (current != null) {
            Robot.pose = current[new]
            Field.rerenderNow()
        }
    }

    val control = SelectBox(
        listOf(
            StyledText(" Edit Path ", background = Color.Blue.lighter().paint),
            StyledText(" Edit Heading ", background = Color.Red.lighter().paint),
            StyledText("View Trajectory")
        ), StyledTextVisualizer()
    )
        .apply {
            size = Size(160, 35)
            idealSize = size
            changed += {
                updateControlSelection()
            }
        }

    fun updateControlSelection() {
        when (control.selection) {
            0 -> {
                hideTrajectoryMenu()
                currentTrajectory?.mode = DraggableTrajectory.Mode.EditPath
            }

            1 -> {
                hideTrajectoryMenu()
                currentTrajectory?.mode = DraggableTrajectory.Mode.EditHeading
            }

            2 -> {
                currentTrajectory?.mode = DraggableTrajectory.Mode.View
                if (currentTrajectory?.let { traj -> showTrajectoryMenu(traj) } != true) {
//                            window.alert("Failed to create Trajectory!")
                } else {
                    val json = currentTrajectory?.serializableTrajectory()?.toJSON()
                    if (json != null) {
                        setLocalStorageItem(GUIApp.trajectoryKey, json)
                    }
                }
            }
        }
    }

    private val json = Json { prettyPrint = false }

    private val trajectoryConfigMenu = viewBuilder {
        visible = false
        layout = verticalLayout(this)
        PushButton("Start").apply {
            size = Size(160, 35)
            idealSize = size
            pressedChanged += { _, _, new ->
                if (new) {
                    text = when (text) {
                        "Stop" -> {
                            TimeManager.stop()
                            "Start"
                        }

                        else -> {
                            TimeManager.play()
                            TimeManager.animation?.completed?.plusAssign {
                                text = "Start"
                            }
                            "Stop"
                        }
                    }
                }
            }
            TimeManager.animationChanged += { _, new ->
                text = if (new != null) "Stop" else "Start"
            }
        }

        val createNumberField = { initialValue: Double, setter: (Double) -> Unit ->
            NumberField(
                initialValue,
                1,
                false,
                setter
            ).apply {
//                this.insets = Insets(2.0)
                this.minimumSize = Size(100.0, 70.0)
                this.idealSize = minimumSize
            }
        }

        +padding(h = 20.0)
        +Label("Robot dimensions:")
        +viewBuilder {
            layout = HorizontalFlowLayout(verticalAlignment = VerticalAlignment.Middle)
            +createNumberField(Robot.dimensions.y) { Robot.dimensions = Robot.dimensions.copy(y = it) }.apply {
                numberFormat = { min(it, 24.0) }
            }
            +Label(" by ")
            +createNumberField(Robot.dimensions.x) { Robot.dimensions = Robot.dimensions.copy(x = it) }.apply {
                numberFormat = { min(it, 24.0) }
            }
        }

        fun updateTrajectory() {
            currentTrajectory?.recomputeTrajectory()
            val trajectory = currentTrajectory?.currentTrajectory ?: run {
//                window.alert("Failed to satisfy constraints!")
                return
            }
            TimeManager.duration = trajectory.duration()
            Robot.pose = trajectory.start()
            ScrubBar.rerender()
        }

//        listOf(constraints::maxVel, constraints::maxAccel).forEach { prop ->
//            +viewBuilder {
//                layout = HorizontalFlowLayout(verticalAlignment = VerticalAlignment.Middle)
//                +Label("${prop.name}:")
//                +createNumberField(prop.get()) {
//                    prop.set(it)
//                    updateTrajectory()
//                }
//            }
//        }
//
//        listOf(constraints::maxAngVel, constraints::maxAngAccel).forEach { prop ->
//            +viewBuilder {
//                layout = HorizontalFlowLayout(verticalAlignment = VerticalAlignment.Middle)
//                +Label("${prop.name}:")
//                +createNumberField(prop.get().degrees) {
//                    prop.set(it.deg)
//                    updateTrajectory()
//                }.apply { format = { "$it°" } }
//            }
//        }
    }

//    val subPieceList = mutableListModelOf<PieceWithData>()
//    private val subPieceView = ScrollPanel(DynamicList<PieceWithData, DynamicListModel<PieceWithData>>(
//        subPieceList, object : ItemVisualizer<PieceWithData, IndexedItem> {
//            override fun invoke(item: PieceWithData, previous: View?, context: IndexedItem): View = viewBuilder {
//                when (item.trajectoryPiece) {
//                    is TurnPiece -> {
//                        +Label("Turn")
//                        +TextField()
//                    }
//                    is WaitPiece ->
//                    else -> return@viewBuilder
//                }
//                constrain {
//                    it.
//                }
//            }
//        }
//    ))

    init {
        minimumSize = Size(230.0, 100.0)
        width = minimumSize.width
        insets = Insets(left = 10.0)
        //Just resetting all UI components
        currentTrajectory = DraggableTrajectory(
            TrajectoryMetadata.fromTrajectory(
                SerializableTrajectory(StartPiece(Pose2d()), mutableListOf())
            )
        )
        currentTrajectory = null
        val padding = object : View() {
            init {
                size = Size(100.0, 200.0)
                layout = verticalLayout(this)
                children += listOf(fieldImageButton, trajectorySelect, exportButton, control, trajectoryConfigMenu)
            }
        }
        children += padding
        layout = constrain(padding) {
            it.center eq parent.center
            it.left eq insets.left
            it.right eq parent.right - insets.right
        }
    }
}