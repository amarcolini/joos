package com.griffinrobotics.lib.gui.style

import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import tornadofx.*

/**
 * The default light theme.
 */
class Light : Theme() {
    override val base = Color.WHITE
    override val background = c("#F2F2F2")
    override val text = c("#000000")
    override val lightText = Color.WHITE
    override val chart = c("#D5D5D5")
    override val tabHover = c("#DADADA")
    override val tabBorder = c("#4083C9")
    override val tabSelected = c("#FFFFFF")
    override val tabSelectedHover = c("#E6E6E6")
    override val editor = c("#FFFFFF")
    override val lineSelected = c("#FCFAED")
    override val valueText = c("#00627A")
    override val propertyText = c("#4A86E8")
    override val error = c("#F50000")
    override val scrollBarHover = c("#F2F2F2")
    override val thumb = c("#E3E3E3")
    override val thumbHover = c("#D8D8D8")
    override val control = c("#FFFFFF")
    override val controlBorder = c("#C4C4C4")
    override val controlFocus = c("#97C3F3")
    override val menuHover = c("#2675BF")

    init {
        style()
    }
}