package com.amarcolini.joos.util

import com.acmerobotics.dashboard.canvas.Canvas
import com.amarcolini.joos.command.CommandScheduler
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.trajectory.PathTrajectorySegment
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TurnSegment
import com.amarcolini.joos.trajectory.WaitSegment
import kotlin.math.ceil


/**
 * Class containing various utilities for use with [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard).
 */
object DashboardUtil {
    private const val ROBOT_RADIUS = 9.0
    private const val DEFAULT_RESOLUTION = 2.0

    @JvmStatic
    @JvmOverloads
    fun drawPoseHistory(
        poseHistory: List<Pose2d>,
        color: String,
        canvas: Canvas = CommandScheduler.packet.fieldOverlay()
    ) {
        val xPoints = DoubleArray(poseHistory.size)
        val yPoints = DoubleArray(poseHistory.size)
        for (i in poseHistory.indices) {
            val pose = poseHistory[i]
            xPoints[i] = pose.x
            yPoints[i] = pose.y
        }
        canvas.setStroke(color)
        canvas.strokePolyline(xPoints, yPoints)
    }

    @JvmStatic
    @JvmOverloads
    fun drawRobot(
        pose: Pose2d,
        color: String,
        canvas: Canvas = CommandScheduler.packet.fieldOverlay()
    ) {
        canvas.setStroke(color)
        canvas.strokeCircle(pose.x, pose.y, ROBOT_RADIUS)
        val (x, y) = pose.headingVec() * ROBOT_RADIUS
        val x1: Double = pose.x + x / 2
        val y1: Double = pose.y + y / 2
        val x2: Double = pose.x + x
        val y2: Double = pose.y + y
        canvas.strokeLine(x1, y1, x2, y2)
    }

    @JvmStatic
    @JvmOverloads
    fun drawSampledPath(
        path: Path,
        color: String,
        canvas: Canvas = CommandScheduler.packet.fieldOverlay(),
        resolution: Double = DEFAULT_RESOLUTION
    ) {
        val samples = ceil(path.length() / resolution).toInt()
        val xPoints = DoubleArray(samples)
        val yPoints = DoubleArray(samples)
        val dx: Double = path.length() / (samples - 1)
        for (i in 0 until samples) {
            val displacement = i * dx
            val (x, y, _) = path[displacement]
            xPoints[i] = x
            yPoints[i] = y
        }
        canvas.setStroke(color)
        canvas.strokePolyline(xPoints, yPoints)
    }

    @JvmStatic
    @JvmOverloads
    fun drawSampledTrajectory(
        trajectory: Trajectory,
        pathColor: String = "#4CAF50",
        turnColor: String = "#7c4dff",
        waitColor: String = "#dd2c00",
        canvas: Canvas = CommandScheduler.packet.fieldOverlay(),
        resolution: Double = DEFAULT_RESOLUTION
    ) {
        trajectory.segments.forEach {
            when (it) {
                is PathTrajectorySegment -> {
                    canvas.setStrokeWidth(1)
                    drawSampledPath(it.path, pathColor, canvas, resolution)
                }
                is TurnSegment -> {
                    val pose = it.start()
                    canvas.setFill(turnColor)
                    canvas.fillCircle(pose.x, pose.y, 2.0)
                }
                is WaitSegment -> {
                    val pose = it.start()
                    canvas.setStrokeWidth(1)
                    canvas.setStroke(waitColor)
                    canvas.strokeCircle(pose.x, pose.y, 3.0)
                }
            }
        }
    }
}