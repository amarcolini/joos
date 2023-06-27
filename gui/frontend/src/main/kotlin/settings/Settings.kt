package settings

import animation.ScrubBar
import field.DraggableTrajectory
import field.Robot
import animation.TimeManager
import field.Field
import io.nacular.doodle.controls.StyledTextVisualizer
import io.nacular.doodle.controls.buttons.PushButton
import io.nacular.doodle.controls.dropdown.Dropdown
import io.nacular.doodle.controls.form.verticalLayout
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.lighter
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.Insets
import io.nacular.doodle.layout.constraints.constrain
import io.nacular.doodle.text.StyledText

object Settings : View() {
    private val control = Dropdown(
        listOf(
            StyledText(" Edit Path ", background = Color.Blue.lighter().paint),
            StyledText(" Edit Heading ", background = Color.Red.lighter().paint),
            StyledText("View Trajectory")
        ), StyledTextVisualizer()
    )
        .apply {
            size = Size(160, 35)
            changed += {
                fun setShowTrajectory(value: Boolean) {
                    ScrubBar.visible = value
                    startButton.visible = value
                    Robot.visible = value
                    Robot.pose = DraggableTrajectory.currentPath.start()
                    Robot.rerenderNow()
                    TimeManager.setTime(0.0, false)
                    Field.bounds = Rectangle(1.0, 1.0) //Hacky trick to get display to relayout
                }
                when (it.selection) {
                    0 -> {
                        setShowTrajectory(false)
                        DraggableTrajectory.initializePathEditing()
                    }
                    1 -> {
                        setShowTrajectory(false)
                        DraggableTrajectory.initializeHeadingEditing()
                    }
                    2 -> {
                        DraggableTrajectory.disableEditing()
                        val start = DraggableTrajectory.currentPath.internalGet(0, 0.0)
                        Robot.pose = start
                        setShowTrajectory(true)
                    }
                }
            }
        }

    private val startButton = PushButton("Start").apply {
        visible = false
        size = Size(160, 35)
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

    init {
        minimumSize = Size(170.0, 100.0)
        width = minimumSize.width
        insets = Insets(left = 10.0)
        val padding = object : View() {
            init {
                layout = verticalLayout(this)
                children += listOf(control, startButton)
            }
        }
        children += padding
        layout = constrain(padding) {
            it.center eq parent.center
            it.left eq insets.left
            it.right eq parent.right
        }
    }
}