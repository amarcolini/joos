package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import javafx.scene.Node

abstract class FieldEntity : Entity {
    abstract val pose: Pose2d
}