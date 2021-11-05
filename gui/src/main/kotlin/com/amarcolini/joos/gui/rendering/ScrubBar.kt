package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.gui.style.Theme
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.VPos
import javafx.scene.canvas.Canvas
import javafx.scene.text.TextAlignment
import tornadofx.*

internal class ScrubBar(private val theme: SimpleObjectProperty<Theme>, private var prefHeight: Double = 50.0) : Canvas() {
    var time = 0.0
        set(value) {
            timeProperty.set(value)
            field = value
            update()
        }
    val timeProperty = SimpleDoubleProperty()
    var duration = 0.0
        set(value) {
            field = value
            update()
        }

    init {
        graphicsContext2D.textAlign = TextAlignment.CENTER
        graphicsContext2D.textBaseline = VPos.CENTER
        setOnMousePressed {
            time = (it.x / width).coerceIn(0.0..1.0) * duration
            update()
        }
        setOnMouseDragged {
            time = (it.x / width).coerceIn(0.0..1.0) * duration
            update()
        }
        theme.onChange {
            update()
        }
        update()
    }

    private fun update() {
        val g = graphicsContext2D
        g.clearRect(0.0, 0.0, width, height)
        g.fill = theme.value.editor
        g.fillRect(0.0, 0.0, width, height)
        g.fill = theme.value.propertyText
        g.fillRect(0.0, 0.0, (time / duration) * width, height)
        g.stroke = theme.value.text
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
        update()
    }

    override fun minHeight(width: Double) = prefHeight
    override fun minWidth(height: Double) = 60.0
    override fun maxHeight(width: Double) = prefHeight
    override fun maxWidth(width: Double) = Double.MAX_VALUE
    override fun isResizable(): Boolean = true
}