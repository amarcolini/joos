package com.griffinrobotics.lib.gui.trajectory

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.geometry.Vector2d
import com.griffinrobotics.lib.trajectory.config.TrajectoryConfig
import javafx.beans.property.SimpleBooleanProperty

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(Start::class),
    JsonSubTypes.Type(Spline::class),
    JsonSubTypes.Type(SplineTo::class),
    JsonSubTypes.Type(SplineToConstantHeading::class),
    JsonSubTypes.Type(SplineToLinearHeading::class),
    JsonSubTypes.Type(SplineToSplineHeading::class),
    JsonSubTypes.Type(StrafeTo::class),
    JsonSubTypes.Type(Forward::class),
    JsonSubTypes.Type(Back::class),
    JsonSubTypes.Type(StrafeLeft::class),
    JsonSubTypes.Type(StrafeRight::class),
    JsonSubTypes.Type(LineTo::class),
    JsonSubTypes.Type(LineToConstantHeading::class),
    JsonSubTypes.Type(LineToLinearHeading::class),
    JsonSubTypes.Type(LineToSplineHeading::class),
    JsonSubTypes.Type(Turn::class),
    JsonSubTypes.Type(Wait::class),
)
sealed class Waypoint {
    val isBad = SimpleBooleanProperty()
}

class Waypoints(val waypoints: List<Waypoint>) {
    constructor(vararg waypoints: Waypoint) : this(waypoints.toList())
}

internal data class Start(var pose: Pose2d = Pose2d(), var tangent: Degree = Degree()) : Waypoint()
internal sealed class Spline : Waypoint()
internal data class SplineTo(var pos: Vector2d = Vector2d(), var tangent: Degree = Degree()) : Spline()
internal data class SplineToConstantHeading(var pos: Vector2d = Vector2d(), var tangent: Degree = Degree()) :
    Spline()

internal data class SplineToLinearHeading(var pose: Pose2d = Pose2d(), var tangent: Degree = Degree()) :
    Spline()

internal data class SplineToSplineHeading(var pose: Pose2d = Pose2d(), var tangent: Degree = Degree()) :
    Spline()

internal sealed class Line : Waypoint()
internal data class StrafeTo(var pos: Vector2d = Vector2d()) : Line()
internal data class Forward(var distance: Double = 0.0) : Line()
internal data class Back(var distance: Double = 0.0) : Line()
internal data class StrafeLeft(var distance: Double = 0.0) : Line()
internal data class StrafeRight(var distance: Double = 0.0) : Line()
internal data class LineTo(var pos: Vector2d = Vector2d()) : Line()
internal data class LineToConstantHeading(var pos: Vector2d = Vector2d()) : Line()
internal data class LineToLinearHeading(var pose: Pose2d = Pose2d()) : Line()
internal data class LineToSplineHeading(var pose: Pose2d = Pose2d()) : Line()
internal data class Turn(var angle: Degree = Degree()) : Waypoint()
internal data class Wait(var seconds: Double = 0.0) : Waypoint()