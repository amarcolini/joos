package com.amarcolini.joos.gui.rendering

import javafx.geometry.Pos

abstract class FixedEntity : Entity {
    open val alignment: Pos = Pos.CENTER
}