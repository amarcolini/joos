package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.trajectory.Start
import com.amarcolini.joos.gui.trajectory.WaypointTrajectory
import javafx.animation.AnimationTimer
import javafx.scene.Group
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.transform.Affine
import tornadofx.add
import tornadofx.onChange
import tornadofx.onLeftClick
import kotlin.math.min

internal class Renderer : StackPane() {
    val trajectoryRenderer = TrajectoryEntity { -min(width, height) / 144 }
    val robot = Robot()
    val fieldRenderer = FieldRenderer()
    val field = Pane()
    private lateinit var transform: Affine
    val overlay = AnchorPane()
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


    private val entities: MutableList<Entity> = mutableListOf(
        trajectoryRenderer,
        robot,
        scrubBar,
        posIndicator
    )

    private val timer = object : AnimationTimer() {
        override fun handle(now: Long) {
            entities.forEach { entity ->
                entity.update(now)
                if (entity is FieldEntity) {
                    val node = entity.node
                    val combinedTransform = Affine().apply {
                        append(transform)
                        appendTranslation(entity.pose.x, entity.pose.y)
                        appendRotation(entity.pose.heading.degrees)
                    }
                    if (node is Group) node.children.forEach {
                        it.transforms.setAll(Affine().apply {
                            append(transform)
                            appendTranslation(it.layoutX, it.layoutY)
                            appendRotation(entity.pose.heading.degrees)
                        })
                    }
                    else node.transforms.setAll(combinedTransform)
//                    it.node.relocate(it.pose.x, it.pose.y)
//                    it.node.translateX = it.pose.x
//                    it.node.translateY = it.pose.y
//                    it.node.rotate = it.pose.heading.degrees
                }
            }
        }
    }

    init {
        add(fieldRenderer)
        add(field)
        add(overlay)
        overlay.pickOnBoundsProperty().set(false)
        entities.forEach {
            if (it is FixedEntity) {
                overlay.add(it.node)
                AnchorPane.setTopAnchor(it.node, it.topAnchor)
                AnchorPane.setBottomAnchor(it.node, it.bottomAnchor)
                AnchorPane.setLeftAnchor(it.node, it.leftAnchor)
                AnchorPane.setRightAnchor(it.node, it.rightAnchor)
            } else {
                field.add(it.node)
            }
        }

        trajectoryRenderer.trajectoryProperty.onChange { trajectory ->
            trajectory?.trajectory?.duration()?.let { scrubBar.node.duration = it }
            robot.stop()
            robot.trajectory = trajectory?.trajectory
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
        field.maxHeight = fieldSize
        field.maxWidth = fieldSize
        transform = getFieldTransform(width, height)
    }

    override fun isResizable(): Boolean = true
}