package settings

import animation.ScrubBar
import field.DraggableTrajectory
import field.Robot
import animation.TimeManager
import com.amarcolini.joos.trajectory.Trajectory
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
import kotlinx.browser.window

object Settings : View() {
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
            changed += {
                fun setShowTrajectory(value: Boolean) {
                    ScrubBar.visible = value
                    startButton.visible = value
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
                            TimeManager.listeners += trajectoryListener
                            TimeManager.duration = trajectory.duration()
                            Robot.pose = trajectory.start()
                            setShowTrajectory(true)
                        }
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