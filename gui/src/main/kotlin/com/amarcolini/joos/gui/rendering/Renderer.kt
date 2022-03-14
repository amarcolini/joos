package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.trajectory.Start
import com.amarcolini.joos.gui.trajectory.WaypointTrajectory
import javafx.animation.AnimationTimer
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Pos
import javafx.scene.Camera
import javafx.scene.SnapshotParameters
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.layout.StackPane
import tornadofx.add
import tornadofx.onChange
import tornadofx.onLeftClick
import java.awt.image.RenderedImage
import java.io.File
import java.nio.channels.Channels
import javax.imageio.ImageIO
import javax.imageio.stream.ImageOutputStream
import kotlin.math.min

internal class Renderer : StackPane() {
    val trajectoryRenderer = TrajectoryEntity { -min(width, height) / 144 }
    val robot = Robot()
    val fieldRenderer = FieldRenderer()
    val scrubBar = ScrubBarEntity()
    val posIndicator = PosIndicator()
    var trajectory: WaypointTrajectory = trajectoryRenderer.trajectory
        set(value) {
            trajectoryRenderer.trajectory = value
            field = value
        }
        get() = trajectoryRenderer.trajectory
    val waypoints get() = trajectoryRenderer.waypoints
    val constraints get() = trajectoryRenderer.constraints


    private val entities: List<Entity> = listOf(
        trajectoryRenderer,
        robot,
        scrubBar,
        posIndicator
    )

    private val timer = object : AnimationTimer() {
        override fun handle(now: Long) {
            entities.forEach {
                it.update(now)
                if (it is FieldEntity) {
                    val fieldSize = min(width, height)
                    val offset = Pose2d(
                        Vector2d(-it.pose.y, -it.pose.x) * (fieldSize / 144),
                        Math.toDegrees(-it.pose.heading)
                    )
                    it.node.translateX = offset.x
                    it.node.translateY = offset.y
                    it.node.rotate = offset.heading + 90
                }
            }
        }
    }

    init {
        add(fieldRenderer)
        entities.forEach {
            add(it.node)
            if (it is FixedEntity) {
                setAlignment(it.node, it.alignment)
            }
        }

        trajectoryRenderer.trajectoryProperty.onChange {
            it?.trajectory?.duration()?.let { scrubBar.node.duration = it }
            robot.stop()
            robot.trajectory = it?.trajectory
        }
        robot.timeProperty.bindBidirectional(scrubBar.node.timeProperty)
        robot.timeProperty.onChange {
            scrubBar.node.time = it
        }

        var dragging = false
        setOnDragDetected {
            if (trajectoryRenderer.beingDragged) {
                dragging = true
            }
        }
        scrubBar.node.setOnMousePressed {
            robot.stop()
            dragging = true
        }
        onLeftClick {
            requestFocus()
            if (!dragging)
                robot.toggle()
            dragging = false
        }
        setOnKeyPressed {
            if (it.code == KeyCode.SPACE) robot.toggle()
        }

        setOnMouseMoved {
            val scale = -min(width, height) / 144
            posIndicator.pos = Vector2d(
                (it.y - height / 2) / scale,
                (it.x - width / 2) / scale
            )
        }
        setOnMouseDragged {
            val scale = -min(width, height) / 144
            posIndicator.pos = Vector2d(
                (it.y - height / 2) / scale,
                (it.x - width / 2) / scale
            )
        }

        trajectoryRenderer.trajectory = WaypointTrajectory(listOf(Start()))

        timer.start()
    }

    override fun resize(width: Double, height: Double) {
        super.resize(width, height)
        val fieldSize = min(width, height)
        val scaleY = fieldSize / 144
        val scaleX = -scaleY
        entities.forEach {
            if (it is FieldEntity) {
                it.node.scaleX = scaleX
                it.node.scaleY = scaleY
            }
        }
    }

    override fun isResizable(): Boolean = true
}