package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.gui.Global
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.text.TextAlignment
import tornadofx.onChange

internal class ScrubBar(
    private var prefHeight: Double = 50.0
) : Canvas() {
    var time = 0.0
        get() = timeProperty.get().coerceIn(0.0, duration)
        set(value) {
            timeProperty.set(value)
            field = value
            draw()
        }
    val timeProperty = SimpleDoubleProperty()
    var duration = 0.0
        set(value) {
            field = value
            draw()
        }

    init {
        graphicsContext2D.textAlign = TextAlignment.CENTER
        graphicsContext2D.textBaseline = VPos.CENTER
        setOnMousePressed {
            time = (it.x / width).coerceIn(0.0..1.0) * duration
            draw()
        }
        setOnMouseDragged {
            time = (it.x / width).coerceIn(0.0..1.0) * duration
            draw()
        }
        Global.theme.onChange { draw() }
        draw()
    }

    private fun draw() {
        val g = graphicsContext2D
        g.clearRect(0.0, 0.0, width, height)
        g.fill = Global.theme.value.editor
        g.fillRect(0.0, 0.0, width + 5, height + 5)
        g.fill = Global.theme.value.propertyText
        g.fillRect(0.0, 0.0, (time / duration) * width, height)
        g.stroke = Global.theme.value.text
        g.strokeText(
            String.format("%.2f / %.2f", time, duration),
            width / 2,
            height / 2
        )
    }

    override fun prefHeight(width: Double) = prefHeight

    override fun resize(width: Double, height: Double) {
        super.setWidth(width)
        super.setHeight(height)
        draw()
    }

    override fun minHeight(width: Double) = prefHeight
    override fun minWidth(height: Double) = 60.0
    override fun maxHeight(width: Double) = prefHeight
    override fun maxWidth(width: Double) = Double.MAX_VALUE
    override fun isResizable(): Boolean = true
}