package com.amarcolini.joos.gui.trajectory

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.trajectory.config.GenericConstraints
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints
import javafx.beans.property.SimpleObjectProperty

sealed class Waypoint {
    val error: SimpleObjectProperty<String?> = SimpleObjectProperty<String?>(null)
}

class WaypointTrajectory(
    val waypoints: List<Waypoint> = listOf(Start()),
    val constraints: TrajectoryConstraints = GenericConstraints()
) {
    constructor(
        constraints: TrajectoryConstraints = GenericConstraints(),
        vararg waypoints: Waypoint = arrayOf(Start())
    ) : this(waypoints.toList(), constraints)

    val trajectory by lazy {
        waypoints.toTrajectory(constraints)
    }
}

internal data class Start(var pose: Pose2d = Pose2d(), var tangent: Degree = Degree()) : Waypoint()
internal sealed class Spline : Waypoint() {
    abstract var tangent: Degree
}

internal data class SplineTo(
    var pos: Vector2d = Vector2d(),
    override var tangent: Degree = Degree()
) :
    Spline()

internal data class SplineToConstantHeading(
    var pos: Vector2d = Vector2d(),
    override var tangent: Degree = Degree()
) : Spline()

internal data class SplineToLinearHeading(
    var pose: Pose2d = Pose2d(),
    override var tangent: Degree = Degree()
) : Spline()

internal data class SplineToSplineHeading(
    var pose: Pose2d = Pose2d(),
    override var tangent: Degree = Degree()
) : Spline()

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