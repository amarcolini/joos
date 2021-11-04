package com.griffinrobotics.lib.gui

import com.griffinrobotics.lib.gui.style.Theme
import com.griffinrobotics.lib.gui.trajectory.WaypointBuilder
import com.griffinrobotics.lib.trajectory.config.MecanumConfig
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

class Test : Theme() {
    override val base = Color.WHITE
    override val background = Color.WHITE
    override val text = Color.WHITE
    override val lightText = Color.WHITE
    override val chart = Color.WHITE
    override val tabHover = Color.WHITE
    override val tabBorder = Color.WHITE
    override val tabSelected = Color.WHITE
    override val tabSelectedHover = Color.WHITE
    override val editor = Color.WHITE
    override val lineSelected = Color.WHITE
    override val valueText = Color.WHITE
    override val propertyText = Color.WHITE
    override val error = Color.WHITE
    override val scrollBarHover = Color.WHITE
    override val thumb = Color.WHITE
    override val thumbHover = Color.WHITE
    override val control = Color.WHITE
    override val controlBorder = Color.WHITE
    override val controlFocus = Color.WHITE
    override val menuHover = Color.WHITE

    init {
        style()
    }
}

fun main() {
    GUI()
        .setConstraints(MecanumConfig(500.0))
        .followTrajectory(WaypointBuilder().forward(10.0).build())
        .setTheme(Test())
        .start()
}