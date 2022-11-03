package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.gui.style.Dark
import com.amarcolini.joos.gui.style.Theme
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos

internal class ScrubBarEntity : FixedEntity() {
    override val topAnchor = null
    override val bottomAnchor: Double = 0.0
    override val leftAnchor: Double = 0.0
    override val rightAnchor: Double = 0.0
    override val node: ScrubBar = ScrubBar()
}