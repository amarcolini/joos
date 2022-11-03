package com.amarcolini.joos.gui.trajectory

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.constraints.GenericConstraints
import com.amarcolini.joos.trajectory.constraints.TrajectoryConstraints
import javafx.beans.property.SimpleObjectProperty

sealed class Waypoint {
    val error: SimpleObjectProperty<String?> = SimpleObjectProperty<String?>(null)
}

class WaypointTrajectory(
    val waypoints: List<Waypoint> = listOf(Start()),
    val constraints: TrajectoryConstraints = GenericConstraints(),
    resolution: Double = 0.25
) {
    constructor(
        constraints: TrajectoryConstraints = GenericConstraints(),
        vararg waypoints: Waypoint = arrayOf(Start())
    ) : this(waypoints.toList(), constraints)

    val trajectory: Trajectory? by lazy {
        waypoints.toTrajectory(constraints, resolution)
    }
}

internal sealed class Line : Waypoint() {
    internal abstract var pos: Vector2d
}

internal sealed class Spline : Line() {
    abstract var tangent: Angle
}

internal sealed interface CustomHeading {
    var pose: Pose2d
}

internal data class Start(override var pose: Pose2d = Pose2d(), override var tangent: Angle = Angle()) : Spline(),
    CustomHeading {
    override var pos: Vector2d
        get() = pose.vec()
        set(value) {
            pose = Pose2d(value, pose.heading)
        }
}

internal data class SplineTo(
    public override var pos: Vector2d = Vector2d(),
    override var tangent: Angle = Angle()
) : Spline()

internal data class SplineToConstantHeading(
    public override var pos: Vector2d = Vector2d(),
    override var tangent: Angle = Angle()
) : Spline()

internal data class SplineToLinearHeading(
    override var pose: Pose2d = Pose2d(),
    override var tangent: Angle = Angle()
) : Spline(), CustomHeading {
    override var pos: Vector2d
        get() = pose.vec()
        set(value) {
            pose = Pose2d(value, pose.heading)
        }
}

internal data class SplineToSplineHeading(
    override var pose: Pose2d = Pose2d(),
    override var tangent: Angle = Angle()
) : Spline(), CustomHeading {
    override var pos: Vector2d
        get() = pose.vec()
        set(value) {
            pose = Pose2d(value, pose.heading)
        }
}

internal data class StrafeTo(public override var pos: Vector2d = Vector2d()) : Line()
internal data class Forward(var distance: Double = 0.0) : Waypoint()
internal data class Back(var distance: Double = 0.0) : Waypoint()
internal data class StrafeLeft(var distance: Double = 0.0) : Waypoint()
internal data class StrafeRight(var distance: Double = 0.0) : Waypoint()
internal data class LineTo(public override var pos: Vector2d = Vector2d()) : Line()
internal data class LineToConstantHeading(public override var pos: Vector2d = Vector2d()) : Line()
internal data class LineToLinearHeading(override var pose: Pose2d = Pose2d()) : Line(), CustomHeading {
    override var pos: Vector2d
        get() = pose.vec()
        set(value) {
            pose = Pose2d(value, pose.heading)
        }
}

internal data class LineToSplineHeading(override var pose: Pose2d = Pose2d()) : Line(), CustomHeading {
    override var pos: Vector2d
        get() = pose.vec()
        set(value) {
            pose = Pose2d(value, pose.heading)
        }
}

internal data class Turn(var angle: Angle = Angle()) : Waypoint()
internal data class Wait(var seconds: Double = 0.0) : Waypoint()