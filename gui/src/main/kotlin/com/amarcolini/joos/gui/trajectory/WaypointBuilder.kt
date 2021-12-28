package com.amarcolini.joos.gui.trajectory

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.trajectory.config.GenericConstraints
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints

class WaypointBuilder(
    startPose: Pose2d = Pose2d(),
    startTangent: Double = startPose.heading,
    private val constraints: TrajectoryConstraints = GenericConstraints()
) {
    private val waypoints: ArrayList<Waypoint> =
        arrayListOf(Start(startPose, Degree(startTangent, true)))

    fun splineTo(pos: Vector2d, tangent: Double): WaypointBuilder {
        waypoints += SplineTo(pos, Degree(tangent, true))
        return this
    }

    fun splineToConstantHeading(pos: Vector2d, tangent: Double): WaypointBuilder {
        waypoints += SplineToConstantHeading(pos, Degree(tangent, true))
        return this
    }

    fun splineToSplineHeading(pose: Pose2d, tangent: Double): WaypointBuilder {
        waypoints += SplineToSplineHeading(pose, Degree(tangent, true))
        return this
    }

    fun lineTo(pos: Vector2d): WaypointBuilder {
        waypoints += LineTo(pos)
        return this
    }

    fun lineToConstantHeading(pos: Vector2d): WaypointBuilder {
        waypoints += LineToConstantHeading(pos)
        return this
    }

    fun lineToSplineHeading(pose: Pose2d): WaypointBuilder {
        waypoints += LineToSplineHeading(pose)
        return this
    }

    fun strafeTo(pos: Vector2d): WaypointBuilder {
        waypoints += StrafeTo(pos)
        return this
    }

    fun forward(distance: Double): WaypointBuilder {
        waypoints += Forward(distance)
        return this
    }

    fun back(distance: Double): WaypointBuilder {
        waypoints += Back(distance)
        return this
    }

    fun strafeLeft(distance: Double): WaypointBuilder {
        waypoints += StrafeLeft(distance)
        return this
    }

    fun strafeRight(distance: Double): WaypointBuilder {
        waypoints += StrafeRight(distance)
        return this
    }

    fun turn(angle: Double): WaypointBuilder {
        waypoints += Turn(Degree(angle, true))
        return this
    }

    fun wait(seconds: Double): WaypointBuilder {
        waypoints += Wait(seconds)
        return this
    }

    fun build() = WaypointTrajectory(waypoints, constraints)
}