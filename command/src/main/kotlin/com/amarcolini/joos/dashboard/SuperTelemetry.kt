package com.amarcolini.joos.dashboard

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
import java.util.function.Supplier
import kotlin.math.ceil

/**
 * A powerful telemetry for both the Driver Station and [FTC Dashboard](https://github.com/acmerobotics/ftc-dashboard).
 */
object SuperTelemetry {
    private var packet: TelemetryPacket = TelemetryPacket()
    val lines: MutableList<Linable> = ArrayList()
    private val telemetries: MutableList<Telemetry> = ArrayList()

    var isAutoClear: Boolean = true
        @JvmName("setAutoClear") set

    var msTransmissionInterval: Int = 250
        set(value) {
            telemetries.forEach { it.msTransmissionInterval = value }
            FtcDashboard.getInstance()?.telemetryTransmissionInterval = value
            field = value
        }

    var itemSeparator: String = " | "
    var captionValueSeparator: String = " : "

    fun fieldOverlay(): Canvas = packet.fieldOverlay()

    /**
     * The basic telemetry line.
     */
    abstract class Linable {
        abstract fun composed(): String

        var isRetained: Boolean = false

        open fun add(item: Linable): Linable {
            lines.add(lines.indexOf(this) + 1, item)
            return item
        }
    }

    /**
     * A [Linable] that can share a line with other [Inlinable]s.
     */
    abstract class Inlinable : Linable() {
        var parent: Line? = null
            internal set

        open fun addData(caption: String, format: String, arg1: Any?, vararg args: Any?): Item =
            Item(caption, String.format(format, arg1, *args)).also { add(it) }

        open fun addData(caption: String, value: Any?): Item =
            Item(caption, value.toString()).also { add(it) }

        open fun addDataProvider(caption: String, provider: Supplier<Any?>): ItemProvider =
            ItemProvider(caption, provider).also { add(it) }

        open fun add(item: Inlinable): Inlinable {
            val parent = parent
            if (parent == null) {
                lines.add(lines.indexOf(this) + 1, item)
            } else {
                parent.items.add(parent.items.indexOf(this) + 1, item)
            }
            return item
        }

        override fun add(item: Linable): Linable {
            val parent = parent
            if (parent == null)
                lines.add(lines.indexOf(this) + 1, item)
            else lines.add(lines.indexOf(parent) + 1, item)
            return item
        }
    }

    /**
     * A container for [Inlinable]s with a caption.
     */
    class Line(var caption: String, vararg items: Inlinable) : Linable() {
        internal constructor(vararg items: Inlinable) : this("", *items)

        val items: MutableList<Inlinable> = items.toMutableList()

        init {
            items.forEach { it.parent = this }
        }

        override fun composed(): String = caption + items.joinToString(itemSeparator, transform = Linable::composed)

        fun setRetained(retained: Boolean): Line {
            isRetained = retained
            return this
        }

        fun setCaption(caption: String): Line {
            this.caption = caption
            return this
        }

        fun addData(caption: String, format: String, arg1: Any?, vararg args: Any?): Line =
            add(Item(caption, String.format(format, arg1, *args)))

        fun addData(caption: String, value: Any?): Line =
            add(Item(caption, value.toString()))

        fun addDataProvider(caption: String, provider: Supplier<Any?>): Line =
            add(ItemProvider(caption, provider))

        fun add(item: Inlinable): Line {
            item.parent = this
            items += item
            return this
        }
    }

    /**
     * A telemetry item containing a caption and a value. Can share a line with other items.
     */
    class Item(var caption: String, var value: String) : Inlinable() {

        fun setCaption(caption: String): Item {
            this.caption = caption
            return this
        }

        fun setValue(format: String, vararg args: Any?): Item {
            value = String.format(format, *args)
            return this
        }

        fun setValue(value: Any?): Item {
            this.value = value.toString()
            return this
        }

        override fun composed(): String = "$caption$captionValueSeparator$value"

        fun setRetained(retained: Boolean): Item {
            isRetained = retained
            return this
        }
    }

    /**
     * A telemetry item that holds a data provider. Useful for changing data.
     */
    class ItemProvider(var caption: String, var provider: Supplier<Any?>) : Inlinable() {
        fun setCaption(caption: String): ItemProvider {
            this.caption = caption
            return this
        }

        fun setProvider(provider: Supplier<Any?>): ItemProvider {
            this.provider = provider
            return this
        }

        override fun composed(): String = "$caption$captionValueSeparator${provider.get()}"

        init {
            isRetained = true
        }

        fun setRetained(retained: Boolean): ItemProvider {
            isRetained = retained
            return this
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

    fun addData(caption: String, format: String, arg1: Any?, vararg args: Any?): Item =
        addData(caption, String.format(format, arg1, *args))

    fun addData(caption: String, value: Any?): Item {
        val item = Item(caption, value.toString())
        lines += item
        return item
    }

    fun addDataProvider(caption: String, provider: Supplier<Any?>): ItemProvider {
        val item = ItemProvider(caption, provider)
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
                        lineable.items.isEmpty() && !lineable.isRetained
                    } else !lineable.isRetained
                }

                else -> !lineable.isRetained
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
        telemetries.forEach { it.clearAll() }
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