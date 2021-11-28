package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.gui.style.Theme
import javafx.geometry.Pos
import javafx.scene.Node

internal abstract class Entity {
    abstract val node: Node
    abstract val pose: Pose2d
    open val alignment: Pos = Pos.CENTER

    abstract fun update(now: Long, theme: Theme)
}