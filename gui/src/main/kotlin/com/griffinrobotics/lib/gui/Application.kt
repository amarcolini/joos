package com.griffinrobotics.lib.gui

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.griffinrobotics.lib.gui.rendering.TrajectoryRenderer
import com.griffinrobotics.lib.gui.style.Custom
import com.griffinrobotics.lib.gui.style.Dark
import com.griffinrobotics.lib.gui.style.Light
import com.griffinrobotics.lib.gui.style.Theme
import com.griffinrobotics.lib.gui.trajectory.TrajectoryEditor
import com.griffinrobotics.lib.gui.trajectory.Waypoints
import com.griffinrobotics.lib.trajectory.config.TrajectoryConstraints
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.TabPane
import javafx.scene.input.KeyCombination
import javafx.stage.Stage
import tornadofx.*
import javax.swing.text.Style

internal class MainApp : App(MainView::class, Dark::class) {
    init {
        reloadStylesheetsOnFocus()
    }

    override fun start(stage: Stage) {
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

    override
    val root = borderpane {
        top = menubar {
            menu("_File") {
                menu("_New") {
                }
            }
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
        bottom = splitpane {
            tabpane {
                tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
                tab("Trajectory") {
                    add(editor)
                }
            }
            add(editor.renderer)
        }
        setMinSize(500.0, 500.0)
    }
}

fun main(args: Array<String>) {
    launch<MainApp>(args)
}