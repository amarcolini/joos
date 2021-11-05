package com.amarcolini.joos.gui.style

import javafx.scene.paint.Color
import tornadofx.*

/**
 * The default dark theme. This is the default theme.
 */
class Dark : Theme() {
    override val base = Color.BLACK
    override val background = c("#3C3F41")
    override val text = c("#BBBBBB")
    override val lightText = text
    override val chart = c("rgb(100, 100, 100)")
    override val tabHover = c("#27292A")
    override val tabBorder = c("#4A88C7")
    override val tabSelected = c("#4E5254")
    override val tabSelectedHover = c("#333537")
    override val editor = c("#2B2B2B")
    override val lineSelected = c("#323232")
    override val valueText = c("#CC7832")
    override val propertyText = c("#467CDA")
    override val error = c("#FF6B68")
    override val scrollBarHover = c("rgba(67, 70, 71, 0.7)")
    override val thumb = c("#595B5D")
    override val thumbHover = c("#666868")
    override val control = c("#4C5052")
    override val controlBorder = c("#5E6060")
    override val controlFocus = c("#466D94")
    override val menuHover = c("#4B6EAF")

    init {
        style()
    }
}