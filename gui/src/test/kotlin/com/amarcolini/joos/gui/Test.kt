package com.amarcolini.joos.gui

import com.amarcolini.joos.gui.rendering.Backgrounds
import com.amarcolini.joos.gui.style.Themes
import com.amarcolini.joos.gui.trajectory.WaypointBuilder
import com.amarcolini.joos.trajectory.config.GenericConstraints

fun main() {
    GUI()
        .setBackground(Backgrounds.FreightFrenzy)
        .setTheme(Themes.Light)
        .followTrajectory(WaypointBuilder(constraints = GenericConstraints(40.0, 1000.0))
            .forward(10.0)
            .build()
        )
        .start()
}