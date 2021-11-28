package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.style.Theme
import com.amarcolini.joos.trajectory.Trajectory
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.shape.Path
import tornadofx.closepath
import tornadofx.lineTo
import tornadofx.moveTo
import kotlin.math.min

internal class Robot : Entity() {
    override var pose: Pose2d = Pose2d()
    var dimensions: Vector2d = Vector2d(18.0, 18.0)
        set(value) {
            node.elements.clear()
            node.moveTo(value.y / 2, 0.0)
                .lineTo(0.0, 0.0)
                .lineTo(value.y / 2, 0.0)
                .lineTo(value.y / 2, value.x / 2)
                .lineTo(-value.y / 2, value.x / 2)
                .lineTo(-value.y / 2, -value.x / 2)
                .lineTo(value.y / 2, -value.x / 2)
                .closepath()
            field = value
        }
    override val node: Path = Path()
        .moveTo(dimensions.y / 2, 0.0)
        .lineTo(0.0, 0.0)
        .lineTo(dimensions.y / 2, 0.0)
        .lineTo(dimensions.y / 2, dimensions.x / 2)
        .lineTo(-dimensions.y / 2, dimensions.x / 2)
        .lineTo(-dimensions.y / 2, -dimensions.x / 2)
        .lineTo(dimensions.y / 2, -dimensions.x / 2)
        .closepath()
    var trajectory: Trajectory? = null

    private var last: Long? = null
    private var running: Boolean = false
    val timeProperty = SimpleDoubleProperty(0.0)
    private var t: Double = 0.0
        get() = timeProperty.get()
        set(value) {
            field = value
            timeProperty.set(value)
        }

    override fun update(now: Long, theme: Theme) {
        val trajectory = trajectory ?: return
        pose = trajectory[min(trajectory.duration(), t)]
        if (!running) return
        if (t >= trajectory.duration()) t = 0.0

        val last = last
        if (last == null) {
            this.last = now
            return
        }

        t += (now - last) / 1e9
        if (t >= trajectory.duration()) {
            stop()
            reset()
            return
        }
        this.last = now
    }

    fun start() {
        last = null
        running = true
    }

    fun stop() {
        last = null
        running = false
    }

    fun toggle() {
        last = null
        running = !running
    }

    fun reset() {
        trajectory?.duration()?.let { t = it }
    }
}