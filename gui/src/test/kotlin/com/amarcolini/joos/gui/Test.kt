package com.amarcolini.joos.gui

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.rendering.Backgrounds
import com.amarcolini.joos.gui.style.Themes
import com.amarcolini.joos.gui.trajectory.WaypointBuilder
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.config.GenericConstraints
import com.amarcolini.joos.util.deg

fun main() {
    GUI()
        .setBackground(Backgrounds.FreightFrenzy)
        .setTheme(Themes.Light)
        .followTrajectory(
            WaypointBuilder(constraints = GenericConstraints(40.0, 1000.0))
                .forward(10.0)
                .build()
        )
        .start()
}