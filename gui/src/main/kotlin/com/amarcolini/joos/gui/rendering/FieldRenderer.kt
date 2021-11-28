package com.amarcolini.joos.gui.rendering

import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.transform.Affine
import kotlin.math.min

internal fun getFieldTransform(width: Double, height: Double): Affine {
    val fieldSize = min(width, height)

    val affine = Affine()
    affine.appendTranslation(width / 2, height / 2)
    affine.appendScale(fieldSize / 144, fieldSize / 144)
    affine.appendRotation(90.0)
    affine.appendScale(-1.0, 1.0)
    return affine
}

internal class FieldRenderer(var background: Image) : Canvas() {
    init {
        graphicsContext2D.globalAlpha = 1.0
        draw(width, height)
    }

    override fun resize(width: Double, height: Double) {
        super.setWidth(width)
        super.setHeight(height)
        draw(width, height)
    }

    fun draw(width: Double, height: Double) {
        graphicsContext2D.clearRect(0.0, 0.0, width, height)
        val fieldSize = min(width, height)
        val offsetX = (width - fieldSize) / 2.0
        val offsetY = (height - fieldSize) / 2.0
        graphicsContext2D.drawImage(background, offsetX, offsetY, fieldSize, fieldSize)
    }

    override fun minHeight(width: Double) = 50.0
    override fun minWidth(height: Double) = 50.0
    override fun maxHeight(width: Double) = Double.MAX_VALUE
    override fun maxWidth(width: Double) = Double.MAX_VALUE
    override fun isResizable(): Boolean = true
}