package com.amarcolini.joos.gui.trajectory

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.AngleUnit
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import javafx.util.StringConverter

val doublePattern = "-?\\d+\\.?\\d*".toRegex()

internal class Vector2dStringConverter : StringConverter<Vector2d>() {
    override fun toString(`object`: Vector2d?) =
        String.format("(%.1f, %.1f)", `object`?.x, `object`?.y)

    override fun fromString(string: String?): Vector2d {
        if (string == null) return Vector2d()
        val doubles = doublePattern.findAll(string).map {
            it.value.toDouble()
        }.toList()
        return Vector2d(
            doubles.getOrElse(0) { 0.0 },
            doubles.getOrElse(1) { 0.0 })
    }
}

internal class Pose2dStringConverter : StringConverter<Pose2d>() {
    private val angleConverter = AngleStringConverter()
    override fun toString(`object`: Pose2d?) =
        String.format(
            "(%.1f, %.1f, %s)", `object`?.x, `object`?.y,
            angleConverter.toString(`object`?.heading)
        )

    override fun fromString(string: String?): Pose2d {
        if (string == null) return Pose2d()
        val doubles = doublePattern.findAll(string).map {
            it.value.toDouble()
        }.toList()
        return Pose2d(
            doubles.getOrElse(0) { 0.0 },
            doubles.getOrElse(1) { 0.0 },
            Angle(doubles.getOrElse(2) { 0.0 })
        )
    }
}

internal class AngleStringConverter : StringConverter<Angle>() {
    override fun toString(`object`: Angle?) = when (Angle.defaultUnits) {
        AngleUnit.Degrees -> String.format("%.1f°", `object`?.degrees)
        AngleUnit.Radians -> String.format("%.3f", `object`?.radians)
    }

    override fun fromString(string: String?): Angle {
        if (string == null) return Angle()
        return Angle(doublePattern.find(string)?.value?.toDouble() ?: 0.0)
    }
}

internal class DoubleStringConverter : StringConverter<Number>() {
    override fun toString(`object`: Number?) = String.format("%.1f", `object`)
    override fun fromString(string: String?) =
        string?.let { doublePattern.find(it)?.value?.toDouble() }
}