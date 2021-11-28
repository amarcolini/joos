package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.style.Theme
import com.amarcolini.joos.gui.trajectory.Vector2dStringConverter
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.text.FontWeight
import tornadofx.addClass
import tornadofx.box
import tornadofx.em
import tornadofx.style

class PosIndicator : Entity() {
    private val converter = Vector2dStringConverter()
    var pos = Vector2d()
        set(value) {
            node.text = converter.toString(value)
            field = value
        }
    override val pose get() = Pose2d(pos)
    override val alignment = Pos.TOP_LEFT
    override val node: Label = Label()

    init {
        node.addClass(Theme.padding)
        node.translateX = 10.0
        node.translateY = 10.0
    }

    override fun update(now: Long, theme: Theme) {
        node.style {
            fontFamily = "Jetbrains Mono"
            backgroundColor += theme.background
            borderColor += box(theme.editor)
            borderRadius += box(0.2.em)
            borderWidth += box(0.13.em)
            fontWeight = FontWeight.EXTRA_BOLD
            textFill = theme.text
        }
    }
}