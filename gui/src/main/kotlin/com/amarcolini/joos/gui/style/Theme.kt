package com.amarcolini.joos.gui.style

import javafx.scene.effect.DropShadow
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * Class defining the style of the application.
 *
 * @see Dark
 * @see Light
 */
sealed class Theme : Stylesheet() {
    companion object {
        val editorText by cssclass()
        val valueText by cssclass()
        val propertyText by cssclass()
        val error by csspseudoclass()
        val errorText by cssclass()
        val padding by cssclass()
        val mono = Font.loadFont(this::class.java.getResourceAsStream("/joos/gui/fonts/JetBrainsMono[wght].ttf"), 40.0)!!
    }

    abstract val base: Color
    abstract val background: Color
    abstract val text: Color
    abstract val lightText: Color
    abstract val chart: Color
    abstract val tabHover: Color
    abstract val tabBorder: Color
    abstract val tabSelected: Color
    abstract val tabSelectedHover: Color
    abstract val editor: Color
    abstract val lineSelected: Color
    abstract val valueText: Color
    abstract val propertyText: Color
    abstract val error: Color
    abstract val scrollBarHover: Color
    abstract val thumb: Color
    abstract val thumbHover: Color
    abstract val control: Color
    abstract val controlBorder: Color
    abstract val controlFocus: Color
    abstract val menuHover: Color

    fun style() {
        root {
            baseColor = base
            text {
                fill = text
                fontFamily = Font.getDefault().family
                fontWeight = FontWeight.EXTRA_LIGHT
            }
            faintFocusColor = Color.TRANSPARENT
            focusColor = Color.TRANSPARENT
            backgroundColor = multi(background)
            chartPlotBackground {
                backgroundColor = multi(chart)
            }
            tabHeaderBackground {
                backgroundColor = multi(background)
            }
            columnHeaderBackground {
                backgroundColor = multi(background)
            }
        }

        tab {
            backgroundRadius += box(0.percent)
            borderColor = multi(box(Color.TRANSPARENT))
            borderWidth = multi(box(0.3.em))
            padding = box(0.em, 0.3.em)
            labelPadding = box(0.percent)
            borderStyle += BorderStrokeStyle.SOLID
            backgroundColor += background
            and(hover) {
                backgroundColor += tabHover
            }
            and(selected) {
                backgroundColor += tabSelected
                borderColor +=
                    box(
                        bottom = tabBorder,
                        top = Color.TRANSPARENT,
                        left = Color.TRANSPARENT,
                        right = Color.TRANSPARENT,
                    )
                and(hover) {
                    backgroundColor += tabSelectedHover
                }
            }
        }

        s(listView, cell) {
            backgroundColor = multi(editor)
        }

        cell {
            textFill = text
            fontFamily = "Jetbrains Mono"
            and(focused) {
                backgroundColor = multi(lineSelected)
            }
        }

        editorText {
            fill = text
            borderWidth += box(0.1.em)
            fontWeight = FontWeight.EXTRA_LIGHT
            fontFamily = "Jetbrains Mono"
        }

        valueText {
            fill = valueText
            fontFamily = "Jetbrains Mono"
        }

        propertyText {
            fill = propertyText
            fontFamily = "Jetbrains Mono"
        }

        error {
            borderStyle += BorderStrokeStyle.DASHED
            borderColor += box(
                bottom = error,
                top = Color.TRANSPARENT,
                left = Color.TRANSPARENT,
                right = Color.TRANSPARENT,
            )
        }

        errorText {
            fill = error
            fontFamily = "Jetbrains Mono"
        }

        scrollBar {
            backgroundColor += Color.TRANSPARENT
            animated = true
            prefHeight = 0.9.em
            and(hover) {
                backgroundColor += scrollBarHover
            }
            thumb {
                backgroundColor += thumb
                and(hover) {
                    backgroundColor += thumbHover
                }
                borderWidth += box(0.percent)
                backgroundRadius += box(0.percent)
                labelPadding = box(100.percent)
            }
            s(incrementArrow, decrementArrow, incrementButton, decrementButton) {
                backgroundColor += Color.TRANSPARENT
                padding = box(0.percent)
            }
        }

        s(splitPaneDivider, separator) {
            backgroundColor += thumb
            padding = box(0.percent)
            borderWidth += box(0.percent)
            prefWidth = 0.3.em
        }

        button {
            backgroundColor += control
//            effect = DropShadow(2.0, c("#2F3233"))
            borderColor += box(controlBorder)
            borderRadius += box(0.3.em)
            focusColor = controlFocus
            faintFocusColor = controlFocus
            textFill = text
            and(pressed) {
                borderColor += box(controlFocus)
            }
        }

        textField {
            backgroundColor += control
            borderColor += box(controlBorder)
            borderRadius += box(0.3.em)
            focusColor = controlFocus
            faintFocusColor = controlFocus
            textFill = text
            and(focused) {
                borderColor += box(controlFocus)
            }
            padding = box(0.2.em)
        }

        contextMenu {
            borderColor += box(controlBorder)
            backgroundColor += background
            padding = box(0.percent)
            effect = DropShadow(0.0, Color.TRANSPARENT)
            separator {
                backgroundColor += controlBorder
                fill = controlBorder
                borderWidth += box(0.em)
            }
        }

        headerPanel {
            backgroundColor += background
            borderColor += box(Color.TRANSPARENT)
        }

        s(splitPane, menuBar) {
            backgroundColor += background
        }

        menuBar {
            borderColor += box(
                bottom = controlBorder,
                top = Color.TRANSPARENT,
                left = Color.TRANSPARENT,
                right = Color.TRANSPARENT
            )
            padding = box(0.percent)
        }

        s(menu, menuItem) {
            padding = box(0.3.em, 0.5.em)
            backgroundColor += background
            and(hover, selected) {
                backgroundColor += menuHover
                child(label) {
                    text {
                        fill = lightText
                    }
                }
            }
        }

        tabDownButton {
            backgroundColor += background
        }

        s(comboBoxBase, comboBox) {
            backgroundColor += editor
            text {
                fill = text
            }
            listCell {
                and(hover) {
                    backgroundColor += menuHover
                    text {
                        fill = lightText
                    }
                }
            }
        }

        s(fieldset, padding) {
            padding = box(0.3.em)
        }

        tooltip {
            backgroundColor += background
            effect = DropShadow(0.0, Color.TRANSPARENT)
            borderRadius += box(0.2.em)
            borderColor += box(controlBorder)
        }
    }
}