package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.style.Theme
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TurnSegment
import com.amarcolini.joos.trajectory.WaitSegment
import com.amarcolini.joos.util.DoubleProgression
import javafx.animation.AnimationTimer
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.input.MouseDragEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.*
import kotlin.math.min
import kotlin.math.roundToInt
import tornadofx.*

private const val SPATIAL_RESOLUTION = 2.0

internal class TrajectoryRenderer(val theme: SimpleObjectProperty<Theme>) : StackPane() {
    private var path: Group = Group()
    private var pathOffset: Vector2d = Vector2d()
    var robotDimensions: Vector2d = Vector2d(18.0, 18.0)
        set(value) {
            val path = Path()
                .moveTo(value.y / 2, 0.0)
                .lineTo(0.0, 0.0)
                .lineTo(value.y / 2, 0.0)
                .lineTo(value.y / 2, value.x / 2)
                .lineTo(-value.y / 2, value.x / 2)
                .lineTo(-value.y / 2, -value.x / 2)
                .lineTo(value.y / 2, -value.x / 2)
                .closepath()
            robot = path
            field = value
        }
    private val timer = object : AnimationTimer() {
        var lastUpdate: Double? = null
        override fun handle(now: Long) {
            val seconds = now / 1e9
            if (lastUpdate == null) {
                lastUpdate = seconds
                if (time == duration) {
                    time = 0.0
                }
                return
            }
            val dT = seconds - (lastUpdate ?: return)
            time += dT
            if (time + dT > duration) {
                time = duration
                this@TrajectoryRenderer.stop()
                return
            }
            lastUpdate = seconds
        }

        override fun stop() {
            super.stop()
            lastUpdate = null
        }
    }
    private val scrubBar = ScrubBar(theme)
    private var duration: Double = 0.0
        set(value) {
            scrubBar.duration = value
            field = value
        }
    private var robotPose: Pose2d = Pose2d()
    private var robot: Shape = Rectangle(robotDimensions.x, robotDimensions.y)
    var trajectory: Trajectory? = null
        set(value) {
            field = value

            path.children.clear()
            if (value == null) return

            // compute path samples
            val displacementSamples = (value.length() / SPATIAL_RESOLUTION).roundToInt()
            val displacements =
                DoubleProgression.fromClosedInterval(0.0, value.length(), displacementSamples)
            val poses = displacements.map { value.path[it] }
            val start = value.start()
            val linePath = Path()
            linePath.elements += MoveTo(start.x, start.y)
            linePath.elements += poses.drop(1).map {
                LineTo(it.x, it.y)
            }
            linePath.stroke = Color.GREEN
            linePath.strokeWidth = 1.0
            path.add(linePath)
            path.children.addAll(value.segments.map {
                when (it) {
                    is TurnSegment -> circle(it.start().x, it.start().y, 2.0) {
                        stroke = Color.DARKGREEN
                        fill = Color.TRANSPARENT
                        strokeWidth = 1.0
                    }
                    is WaitSegment -> circle(it.start().x, it.start().y, 1.0) {
                        fill = Color.DARKGREEN
                    }
                    else -> circle(it.end().x, it.end().y, 0.6) {
                        fill = Color.GREEN
                    }
                }
            })
            pathOffset =
                Vector2d(path.boundsInLocal.centerX, path.boundsInLocal.centerY)
            duration = value.duration()
            scrubBar.duration = duration
            reset()
            time = 0.0
        }

    init {
        scrubBar.timeProperty.onChange { time = it }
        scrubBar.setOnMousePressed {
            stop()
            time = scrubBar.time
        }
        scrubBar.addEventHandler(MouseDragEvent.MOUSE_DRAGGED) {
            stop()
        }
        robotDimensions = Vector2d(18.0, 18.0)
        val field = FieldRenderer()
        add(field)
        add(path)
        add(robot)
        add(scrubBar)

        setAlignment(robot, Pos.CENTER)
        setAlignment(path, Pos.CENTER)
        setAlignment(scrubBar, Pos.BOTTOM_CENTER)

        setOnMousePressed {
            if (isRunning) stop()
            else start()
        }
    }

    override fun isResizable(): Boolean = true

    override fun resize(width: Double, height: Double) {
        super.resize(width, height)
        setPose(robot, robotPose)
        setPose(path, Pose2d(pathOffset))
    }

    var isRunning = false
        private set

    fun start() {
        isRunning = true
        timer.start()
    }

    fun stop() {
        isRunning = false
        timer.stop()
    }

    fun reset() {
        stop()
        time = 0.0
    }

    var time: Double = 0.0
        set(value) {
            val pose = trajectory?.get(time)
            if (pose != null) {
                robotPose = pose
                setPose(robot, robotPose)
            }
            field = value
            timeProperty.set(value)
            scrubBar.time = value
        }

    val timeProperty = SimpleDoubleProperty(time)

    private fun getPoseOffset(pose: Pose2d, width: Double, height: Double): Pose2d {
        val fieldSize = min(width, height)
        return Pose2d(Vector2d(-pose.y, -pose.x) * (fieldSize / 144), Math.toDegrees(-pose.heading))
    }

    private fun setPose(node: Node, pose: Pose2d) {
        val offset = getPoseOffset(pose, width, height)
        val fieldSize = min(width, height)
        node.translateX = offset.x
        node.translateY = offset.y
        node.rotate = offset.heading + 90
        node.scaleX = -fieldSize / 144
        node.scaleY = fieldSize / 144
    }
}