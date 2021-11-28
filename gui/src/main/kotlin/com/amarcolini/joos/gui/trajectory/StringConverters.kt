package com.amarcolini.joos.gui.trajectory

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
    override fun toString(`object`: Pose2d?) =
        String.format("(%.1f, %.1f, %.1f°)", `object`?.x, `object`?.y,
            `object`?.heading?.let { Math.toDegrees(it) })

    override fun fromString(string: String?): Pose2d {
        if (string == null) return Pose2d()
        val doubles = doublePattern.findAll(string).map {
            it.value.toDouble()
        }.toList()
        return Pose2d(
            doubles.getOrElse(0) { 0.0 },
            doubles.getOrElse(1) { 0.0 },
            Math.toRadians(doubles.getOrElse(2) { 0.0 })
        )
    }
}

internal class Degree(value: Double = 0.0, inDegrees: Boolean = true) {
    val radians = if (inDegrees) Math.toRadians(value) else value
    val value = if (inDegrees) value else Math.toDegrees(value)
    override fun toString() = String.format("%.1f°", value)
}

internal class DegreeStringConverter : StringConverter<Degree>() {
    override fun toString(`object`: Degree?) = `object`.toString()
    override fun fromString(string: String?): Degree {
        if (string == null) return Degree()
        return Degree(doublePattern.find(string)?.value?.toDouble() ?: 0.0)
    }
}

internal class DoubleStringConverter : StringConverter<Number>() {
    override fun toString(`object`: Number?) = String.format("%.1f", `object`)
    override fun fromString(string: String?) =
        string?.let { doublePattern.find(it)?.value?.toDouble() }
}