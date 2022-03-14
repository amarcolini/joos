package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.canvas.Canvas
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import org.firstinspires.ftc.robotcore.external.Telemetry

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
            val string = line.composed()
            telemetries.forEach { it.addLine(string) }
            packet.addLine(string)
        }
        FtcDashboard.getInstance().sendTelemetryPacket(packet)
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
}