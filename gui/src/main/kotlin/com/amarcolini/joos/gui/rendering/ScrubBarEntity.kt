package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.gui.style.Dark
import com.amarcolini.joos.gui.style.Theme
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos

internal class ScrubBarEntity : Entity() {
    private val themeProperty = SimpleObjectProperty<Theme>(Dark())
    override val node: ScrubBar = ScrubBar(themeProperty)
    override val pose = Pose2d()
    override val alignment = Pos.BOTTOM_CENTER

    override fun update(now: Long, theme: Theme) {
        themeProperty.set(theme)
    }
}