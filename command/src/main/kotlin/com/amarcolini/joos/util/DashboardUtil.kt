package com.amarcolini.joos.util
import com.acmerobotics.dashboard.canvas.Canvas
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import kotlin.math.ceil

/**
 * Class containing various utilities for use with [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard).
 */
object DashboardUtil {
    private const val ROBOT_RADIUS = 9.0
    private const val DEFAULT_RESOLUTION = 2.0

    @JvmStatic
    fun drawRobot(canvas: Canvas, pose: Pose2d) {
        canvas.strokeCircle(pose.x, pose.y, ROBOT_RADIUS)
        val (x, y) = pose.headingVec() * ROBOT_RADIUS
        val x1: Double = pose.y + x / 2
        val y1: Double = pose.y + y / 2
        val x2: Double = pose.y + x
        val y2: Double = pose.y + y
        canvas.strokeLine(x1, y1, x2, y2)
    }

    @JvmStatic
    fun drawSampledPath(canvas: Canvas, path: Path, resolution: Double = DEFAULT_RESOLUTION) {
        val samples = ceil(path.length() / resolution).toInt()
        val xPoints = DoubleArray(samples)
        val yPoints = DoubleArray(samples)
        val dx: Double = path.length() / (samples - 1)
        for (i in 0 until samples) {
            val displacement = i * dx
            val (x, y) = path[displacement]
            xPoints[i] = x
            yPoints[i] = y
        }
        canvas.strokePolyline(xPoints, yPoints)
    }
}