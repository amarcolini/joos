package com.amarcolini.joos.gui.rendering

import javafx.scene.Node

sealed interface Entity {
    val node: Node
    fun update(now: Long) {}
}