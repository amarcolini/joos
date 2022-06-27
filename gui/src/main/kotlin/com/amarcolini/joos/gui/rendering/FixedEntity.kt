package com.amarcolini.joos.gui.rendering

import javafx.geometry.Pos

abstract class FixedEntity : Entity {
    abstract val topAnchor: Double?
    abstract val bottomAnchor: Double?
    abstract val leftAnchor: Double?
    abstract val rightAnchor: Double?
}