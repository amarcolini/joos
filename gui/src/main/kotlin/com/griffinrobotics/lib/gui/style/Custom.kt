package com.griffinrobotics.lib.gui.style

import javafx.scene.paint.Color
import javafx.scene.paint.Paint

data class Custom(
    override val base: Color,
    override val background: Color,
    override val text: Color,
    override val lightText: Color,
    override val chart: Color,
    override val tabHover: Color,
    override val tabBorder: Color,
    override val tabSelected: Color,
    override val tabSelectedHover: Color,
    override val editor: Color,
    override val lineSelected: Color,
    override val valueText: Color,
    override val propertyText: Color,
    override val error: Color,
    override val scrollBarHover: Color,
    override val thumb: Color,
    override val thumbHover: Color,
    override val control: Color,
    override val controlBorder: Color,
    override val controlFocus: Color,
    override val menuHover: Color
) : Theme() {
    init {
        style()
    }
}