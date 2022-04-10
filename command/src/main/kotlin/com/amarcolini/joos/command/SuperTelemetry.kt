package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.canvas.Canvas
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.trajectory.PathTrajectorySegment
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TurnSegment
import com.amarcolini.joos.trajectory.WaitSegment
import org.firstinspires.ftc.robotcore.external.Telemetry
import kotlin.math.ceil

/**
 * A powerful telemetry for both the Driver Station and [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard).
 */
class SuperTelemetry {
    private var packet: TelemetryPacket = TelemetryPacket()
    val lines: MutableList<Lineable> = ArrayList()
    private val telemetries: MutableList<Telemetry> = ArrayList()

    var isAutoClear: Boolean = true
        @JvmName("setAutoClear") set

    var msTransmissionInterval: Int = 250
        set(value) {
            telemetries.forEach { it.msTransmissionInterval = value }
            FtcDashboard.getInstance().telemetryTransmissionInterval = value
            field = value
        }

    var itemSeparator: String = " | "
    var captionValueSeparator: String = " : "

    fun fieldOverlay(): Canvas = packet.fieldOverlay()

    interface Lineable {
        fun composed(): String
    }

    inner class Line(var caption: String, vararg items: Item) : Lineable {
        internal constructor(vararg items: Item) : this("", *items)

        val items: MutableList<Item> = items.toMutableList()

        init {
            items.forEach { it.parent = this }
        }

        override fun composed(): String = caption + items.joinToString(itemSeparator, transform = Item::composed)

        fun addData(caption: String, format: String, arg1: Any, vararg args: Any): Line =
            addData(caption, String.format(format, arg1, *args))

        fun addData(caption: String, value: Any): Line {
            val item = Item(caption, value.toString())
            item.parent = this
            items += item
            return this
        }
    }

    inner class Item internal constructor(var caption: String, var value: String) : Lineable {
        internal lateinit var parent: Line

        fun setCaption(caption: String): Item {
            this.caption = caption
            return this
        }

        fun setValue(format: String, vararg args: Any): Item {
            value = String.format(format, *args)
            return this
        }

        fun setValue(value: Any): Item {
            this.value = value.toString()
            return this
        }

        override fun composed(): String = "$caption$captionValueSeparator$value"

        var isRetained: Boolean = false

        fun setRetained(retained: Boolean): Item {
            isRetained = retained
            return this
        }

        private fun getIndex(): Int = lines.indexOf(parent)

        fun addData(caption: String, format: String, arg1: Any, vararg args: Any): Item =
            addData(caption, String.format(format, arg1, *args))

        fun addData(caption: String, value: Any): Item {
            val item = Item(caption, value.toString())
            lines.add(getIndex() + 1, Line(item))
            return item
        }
    }

    fun register(vararg telemetries: Telemetry) {
        telemetries.forEach {
            it.isAutoClear = true
            it.msTransmissionInterval = msTransmissionInterval
            it.clearAll()
        }
        this.telemetries += telemetries
    }

    fun unregister(vararg telemetries: Telemetry): Boolean = this.telemetries.removeAll(telemetries.toSet())

    fun addData(caption: String, format: String, arg1: Any, vararg args: Any): Item =
        addData(caption, String.format(format, arg1, *args))

    fun addData(caption: String, value: Any): Item {
        val item = Item(caption, value.toString())
        lines += item
        return item
    }

    fun removeItem(item: Item): Boolean =
        lines.remove(item) || lines.filterIsInstance<Line>().any { it.items.remove(item) }

    fun clear() {
        lines.removeIf { lineable ->
            when (lineable) {
                is Line -> {
                    if (lineable.items.isNotEmpty()) {
                        lineable.items.removeIf { !it.isRetained }
                        lineable.items.isEmpty()
                    } else false
                }
                is Item -> !lineable.isRetained
                else -> true
            }
        }
    }

    fun clearAll() = lines.clear()

    fun reset() {
        clearAll()
        telemetries.clear()
    }

    fun speak(text: String) = telemetries.forEach { it.speak(text) }

    fun speak(text: String, languageCode: String, countryCode: String) =
        telemetries.forEach { it.speak(text, languageCode, countryCode) }

    fun update() {
        telemetries.forEach { it.clearAll() }
        lines.forEach { line ->
            when (line) {
                is Item -> {
                    telemetries.forEach { it.addData(line.caption, line.value) }
                    packet.put(line.caption, line.value)
                }
                else -> {
                    val string = line.composed()
                    telemetries.forEach { it.addLine(string) }
                    packet.addLine(string)
                }

            }
        }
        FtcDashboard.getInstance()?.sendTelemetryPacket(packet)
        packet = TelemetryPacket()
        telemetries.forEach { it.update() }
        if (isAutoClear) clear()
    }

    @JvmOverloads
    fun addLine(lineCaption: String = ""): Line {
        val line = Line(lineCaption)
        lines += line
        return line
    }

    fun removeLine(line: Line): Boolean = lines.remove(line)

    fun setDisplayFormat(displayFormat: Telemetry.DisplayFormat?) {
        telemetries.forEach { it.setDisplayFormat(displayFormat) }
    }

    /**
     * The radius used by [drawRobot].
     */
    val robotRadius = 9.0

    /**
     * The resolution used by [drawSampledPath] and [drawSampledTrajectory] when sampling.
     */
    val resolution = 2.0

    /**
     * Draws a list of poses on [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard).
     */
    fun drawPoseHistory(
        poseHistory: List<Pose2d>,
        color: String
    ) {
        val canvas = fieldOverlay()
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

    /**
     * Draws a robot on [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard).
     */
    fun drawRobot(
        pose: Pose2d,
        color: String
    ) {
        val canvas = fieldOverlay()
        canvas.setStroke(color)
        canvas.strokeCircle(pose.x, pose.y, robotRadius)
        val (x, y) = pose.headingVec() * robotRadius
        val x1: Double = pose.x + x / 2
        val y1: Double = pose.y + y / 2
        val x2: Double = pose.x + x
        val y2: Double = pose.y + y
        canvas.strokeLine(x1, y1, x2, y2)
    }

    /**
     * Draws a path on [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard).
     */
    fun drawSampledPath(
        path: Path,
        color: String,
        resolution: Double = this.resolution
    ) {
        val canvas = fieldOverlay()
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

    /**
     * Draws a trajectory on [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard).
     */
    @JvmOverloads
    fun drawSampledTrajectory(
        trajectory: Trajectory,
        pathColor: String = "#4CAF50",
        turnColor: String = "#7c4dff",
        waitColor: String = "#dd2c00",
        resolution: Double = this.resolution
    ) {
        val canvas = fieldOverlay()
        trajectory.segments.forEach {
            when (it) {
                is PathTrajectorySegment -> {
                    canvas.setStrokeWidth(1)
                    drawSampledPath(it.path, pathColor, resolution)
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