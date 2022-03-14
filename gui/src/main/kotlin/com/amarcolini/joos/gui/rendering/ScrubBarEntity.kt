package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.gui.style.Dark
import com.amarcolini.joos.gui.style.Theme
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos

internal class ScrubBarEntity : FixedEntity() {
    override val node: ScrubBar = ScrubBar()
    override val alignment = Pos.BOTTOM_CENTER
}