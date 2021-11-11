package com.amarcolini.joos.gui

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.amarcolini.joos.gui.style.Theme
import com.amarcolini.joos.gui.style.Themes
import com.amarcolini.joos.gui.trajectory.Waypoints
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints
import tornadofx.launch

/**
 * Class for interacting with the application.
 */
class GUI {
    private val args = HashMap<String, String>()
    private val mapper = JsonMapper()

    init {
        mapper.registerKotlinModule()
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        args["add-modules"] = "javafx.controls,javafx.fxml"
    }

    /**
     * Sets the trajectory to be opened on startup.
     */
    fun followTrajectory(trajectory: Waypoints): GUI {
        val string = mapper.writeValueAsString(trajectory)
        args["trajectory"] = string
        return this
    }

    /**
     * Sets the constraints to be obeyed on startup.
     */
    fun setConstraints(constraints: TrajectoryConstraints): GUI {
        val string = mapper.writeValueAsString(constraints)
        args["constraints"] = string
        return this
    }

    fun setTheme(theme: Themes): GUI {
        val string = theme.name.lowercase()
        args["theme"] = string
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