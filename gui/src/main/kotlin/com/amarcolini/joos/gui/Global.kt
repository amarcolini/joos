package com.amarcolini.joos.gui

import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.rendering.Backgrounds
import com.amarcolini.joos.gui.style.Dark
import com.amarcolini.joos.gui.style.Theme
import com.amarcolini.joos.gui.trajectory.WaypointTrajectory
import com.amarcolini.joos.trajectory.config.GenericConstraints
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints
import io.github.classgraph.ClassGraph
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image

internal object Global {
    var theme: SimpleObjectProperty<Theme> = SimpleObjectProperty(Dark())
    var trajectory: WaypointTrajectory = WaypointTrajectory()
    var constraints: TrajectoryConstraints = GenericConstraints()
    var background: Lazy<Image> = Backgrounds.Generic.image
    var robotDimensions: Vector2d = Vector2d(18.0, 18.0)
    val extraBackgrounds: MutableMap<String, Image> = HashMap()
    val extraThemes: MutableMap<String, Theme> = HashMap()
    internal val resources = run {
        val classGraph = ClassGraph()
        classGraph.acceptPaths("joos/gui")
        if (this::class.java.module.name != null)
            classGraph
                .acceptModules(this::class.java.module.name)
                .rejectClasses()
                .scan().allResources
        else null
    }
}