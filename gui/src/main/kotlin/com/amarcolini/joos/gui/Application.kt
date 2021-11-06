package com.amarcolini.joos.gui

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.amarcolini.joos.gui.rendering.TrajectoryRenderer
import com.amarcolini.joos.gui.style.Custom
import com.amarcolini.joos.gui.style.Dark
import com.amarcolini.joos.gui.style.Light
import com.amarcolini.joos.gui.trajectory.TrajectoryEditor
import com.amarcolini.joos.gui.trajectory.Waypoints
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.TabPane
import javafx.stage.Screen
import javafx.stage.Stage
import tornadofx.*

internal class MainApp : App(MainView::class, Dark::class) {
    init {
        reloadStylesheetsOnFocus()
    }

    override fun start(stage: Stage) {
        stage.isMaximized = true
//        stage.minHeight = 500.0
//        stage.minWidth = 500.0
        stage.maxWidth = Screen.getPrimary().visualBounds.width
        stage.maxHeight = Screen.getPrimary().visualBounds.height
        stage.width = Screen.getPrimary().visualBounds.width
        stage.height = Screen.getPrimary().visualBounds.height
        when (parameters.named["theme"]?.lowercase()) {
            "light" -> {
                importStylesheet<Light>()
            }
            "dark" -> {
                importStylesheet<Dark>()
            }
            else -> {
                try {
                    val mapper = JsonMapper()
                    mapper.registerKotlinModule()
                    val theme = mapper.readValue(parameters.named["theme"], Custom::class.java)
                    importStylesheet(theme.base64URL.toExternalForm())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        super.start(stage)
    }
}

internal class MainView : View() {
    val theme = SimpleObjectProperty(
        when (app.parameters.named["theme"]?.lowercase()) {
            "light" -> Light()
            else -> Dark()
        }
    )
    val editor = TrajectoryEditor(TrajectoryRenderer(theme))

    init {
        val mapper = JsonMapper()
        mapper.registerKotlinModule()
        try {
            val trajectory =
                mapper.readValue(app.parameters.named["trajectory"], Waypoints::class.java)
            editor.waypoints.setAll(trajectory.waypoints)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            editor.constraints.set(
                mapper.readValue(
                    app.parameters.named["constraints"],
                    TrajectoryConstraints::class.java
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override val root = vbox {
        menubar {
            menu("_Theme") {
                item("_Dark").action {
                    val dark = Dark()
                    scene.stylesheets.clear()
                    importStylesheet(dark.base64URL.toExternalForm())
                    theme.set(dark)
                }
                item("_Light").action {
                    val light = Light()
                    scene.stylesheets.clear()
                    importStylesheet(light.base64URL.toExternalForm())
                    theme.set(light)
                }
            }
        }
        splitpane {
            tabpane {
                tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
                tab("Trajectory") {
                    add(editor)
                }
            }
            add(editor.renderer)
        }
    }
}

fun main(args: Array<String>) {
    launch<MainApp>(args)
}