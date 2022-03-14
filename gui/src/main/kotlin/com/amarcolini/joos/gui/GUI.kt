package com.amarcolini.joos.gui

import com.amarcolini.joos.gui.rendering.Backgrounds
import com.amarcolini.joos.gui.style.Theme
import com.amarcolini.joos.gui.style.Themes
import com.amarcolini.joos.gui.trajectory.WaypointTrajectory
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import tornadofx.launch

/**
 * Class for interacting with the application.
 */
class GUI {
    private val args = HashMap<String, String>()

    init {
        args["add-modules"] = "javafx.controls,javafx.fxml"
    }

    /**
     * Sets the trajectory to be opened on startup.
     */
    fun followTrajectory(trajectory: WaypointTrajectory): GUI {
        Global.trajectory = trajectory
        return this
    }

    /**
     * Sets the constraints to be obeyed on startup.
     */
    fun setConstraints(constraints: TrajectoryConstraints): GUI {
        Global.constraints = constraints
        return this
    }

    fun setTheme(name: String, theme: Theme): GUI {
        Global.theme.value = theme
        Global.extraThemes[name] = theme
        return this
    }

    fun setTheme(theme: Themes): GUI {
        Global.theme.value = theme.style
        return this
    }

    fun setBackground(background: Backgrounds): GUI {
        Global.background = background.image
        return this
    }

    fun setBackground(name: String, background: Image): GUI {
        Global.background = lazy { background }
        Global.extraBackgrounds[name] = background
        return this
    }

    /**
     * Uses the provided arguments and launches the application.
     *
     *
     * *Note*: This method must only be called *once*.
     */
    fun start() {
        launch<MainApp>(args.map { "--${it.key}=${it.value}" }.toTypedArray())
    }
}