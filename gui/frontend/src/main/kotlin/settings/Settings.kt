package settings

import GUIApp
import GUIApp.Companion.appScope
import GUIApp.Companion.imageLoader
import animation.ScrubBar
import animation.TimeManager
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.util.deg
import field.DraggableTrajectory
import field.DraggableTrajectory.constraints
import field.Field
import field.Robot
import io.nacular.doodle.controls.StyledTextVisualizer
import io.nacular.doodle.controls.buttons.PushButton
import io.nacular.doodle.controls.dropdown.Dropdown
import io.nacular.doodle.controls.form.verticalLayout
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.lighter
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.event.KeyCode
import io.nacular.doodle.event.KeyEvent
import io.nacular.doodle.event.KeyListener
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.HorizontalFlowLayout
import io.nacular.doodle.layout.Insets
import io.nacular.doodle.layout.constraints.constrain
import io.nacular.doodle.text.StyledText
import io.nacular.doodle.utils.VerticalAlignment
import kotlinx.browser.window
import kotlinx.coroutines.launch
import util.BetterViewBuilder.Companion.padding
import util.BetterViewBuilder.Companion.viewBuilder
import util.NumberField
import kotlin.math.min

object Settings : View() {
    private val fieldImageButton = PushButton("Change Field Image").apply {
        visible = true
        size = Size(160, 35)
        idealSize = size
        pressedChanged += { _, _, new ->
            if (new) {
                val url = window.prompt("Enter image URL:", "https://")
                if (url != null) {
                    window.localStorage.setItem(GUIApp.fieldImageKey, url)
                    appScope.launch {
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
                val json = DraggableTrajectory.toJSON()
                window.localStorage.setItem(GUIApp.trajectoryKey, json)
                window.alert(json)
            }
        }
    }

    private val control = Dropdown(
        listOf(
            StyledText(" Edit Path ", background = Color.Blue.lighter().paint),
            StyledText(" Edit Heading ", background = Color.Red.lighter().paint),
            StyledText("View Trajectory")
        ), StyledTextVisualizer()
    )
        .apply {
            val trajectoryListener: (Double, Double) -> Unit = { _, new ->
                val current = DraggableTrajectory.currentTrajectory
                if (current != null) {
                    Robot.pose = current[new]
                    Field.rerenderNow()
                }
            }

            size = Size(160, 35)
            idealSize = size
            changed += {
                fun setShowTrajectory(value: Boolean) {
                    ScrubBar.visible = value
                    trajectoryConfigMenu.visible = value
                    Robot.visible = value
                    Robot.pose = DraggableTrajectory.currentPath.start()
                    Robot.rerenderNow()
                    if (!value) {
                        TimeManager.listeners -= trajectoryListener
                        TimeManager.setTime(0.0, false)
                    }
                }
                when (it.selection) {
                    0 -> {
                        setShowTrajectory(false)
                        DraggableTrajectory.mode = DraggableTrajectory.Mode.EditPath
                    }

                    1 -> {
                        setShowTrajectory(false)
                        DraggableTrajectory.mode = DraggableTrajectory.Mode.EditHeading
                    }

                    2 -> {
                        DraggableTrajectory.disableEditing()
                        val trajectory: Trajectory? = DraggableTrajectory.currentTrajectory
                        if (trajectory == null) {
                            window.alert("Failed to create Trajectory!")
                        } else {
                            val json = DraggableTrajectory.toJSON()
                            window.localStorage.setItem(GUIApp.trajectoryKey, json)
                            TimeManager.listeners += trajectoryListener
                            TimeManager.duration = trajectory.duration()
                            Robot.pose = trajectory.start()
                            setShowTrajectory(true)
                        }
                    }
                }
            }
        }

    private val trajectoryConfigMenu = viewBuilder {
        visible = false
        layout = verticalLayout(this)
        +PushButton("Start").apply {
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
                {}
            ).apply {
//                this.insets = Insets(2.0)
                this.width = 70.0
                this.focusChanged += { _, _, new ->
                    if (!new) setter(this.value)
                }
                this.keyChanged += object : KeyListener {
                    override fun pressed(event: KeyEvent) {
                        if (event.code == KeyCode.Enter || event.code == KeyCode.NumpadEnter) {
                            setter(this@apply.value)
                        }
                    }
                }
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
            DraggableTrajectory.recomputeTrajectory()
            val trajectory = DraggableTrajectory.currentTrajectory ?: run {
                window.alert("Failed to satisfy constraints!")
                return
            }
            TimeManager.duration = trajectory.duration()
            Robot.pose = trajectory.start()
            ScrubBar.rerender()
        }

        listOf(constraints::maxVel, constraints::maxAccel).forEach { prop ->
            +viewBuilder {
                layout = HorizontalFlowLayout(verticalAlignment = VerticalAlignment.Middle)
                +Label("${prop.name}:")
                +createNumberField(prop.get()) {
                    prop.set(it)
                    updateTrajectory()
                }
            }
        }

        listOf(constraints::maxAngVel, constraints::maxAngAccel).forEach { prop ->
            +viewBuilder {
                layout = HorizontalFlowLayout(verticalAlignment = VerticalAlignment.Middle)
                +Label("${prop.name}:")
                +createNumberField(prop.get().degrees) {
                    prop.set(it.deg)
                    updateTrajectory()
                }.apply { format = { "$it°" } }
            }
        }

        doLayout()
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
        val padding = object : View() {
            init {
                size = Size(100.0, 200.0)
                layout = verticalLayout(this)
                children += listOf(exportButton, fieldImageButton, control, trajectoryConfigMenu)
                doLayout()
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