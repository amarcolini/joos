package com.griffinrobotics.lib.gui.trajectory

import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.geometry.Vector2d
import com.griffinrobotics.lib.trajectory.TrajectoryBuilder
import com.griffinrobotics.lib.trajectory.config.TrajectoryConstraints

class WaypointBuilder(
    startPose: Pose2d = Pose2d(),
    startTangent: Double = startPose.heading,
) {
    private val waypoints: ArrayList<Waypoint> =
        arrayListOf(Start(startPose, Degree(startTangent, false)))

    fun splineTo(pos: Vector2d, tangent: Double): WaypointBuilder {
        waypoints += SplineTo(pos, Degree(tangent, false))
        return this
    }

    fun splineToConstantHeading(pos: Vector2d, tangent: Double): WaypointBuilder {
        waypoints += SplineToConstantHeading(pos, Degree(tangent, false))
        return this
    }

    fun splineToSplineHeading(pose: Pose2d, tangent: Double): WaypointBuilder {
        waypoints += SplineToSplineHeading(pose, Degree(tangent, false))
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
        waypoints += Turn(Degree(angle, false))
        return this
    }

    fun wait(seconds: Double): WaypointBuilder {
        waypoints += Wait(seconds)
        return this
    }

    fun build() = Waypoints(waypoints)
}