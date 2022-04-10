package com.amarcolini.joos.gui.trajectory

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.trajectory.config.GenericConstraints
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints

/**
 * A class that makes interacting with the GUI just that much easier.
 */
class WaypointBuilder @JvmOverloads constructor(
    startPose: Pose2d = Pose2d(),
    startTangent: Angle = startPose.heading,
    private val constraints: TrajectoryConstraints = GenericConstraints()
) {
    private val waypoints: ArrayList<Waypoint> =
        arrayListOf(Start(startPose, startTangent))

    fun splineTo(pos: Vector2d, tangent: Angle): WaypointBuilder {
        waypoints += SplineTo(pos, tangent)
        return this
    }

    /**
     * @param tangent tangent in degrees or radians as specified by [Angle.defaultUnits]
     */
    fun splineTo(pos: Vector2d, tangent: Double): WaypointBuilder = splineTo(pos, Angle(tangent))

    fun splineToConstantHeading(pos: Vector2d, tangent: Angle): WaypointBuilder {
        waypoints += SplineToConstantHeading(pos, tangent)
        return this
    }

    /**
     * @param tangent tangent in degrees or radians as specified by [Angle.defaultUnits]
     */
    fun splineToConstantHeading(pos: Vector2d, tangent: Double): WaypointBuilder =
        splineToConstantHeading(pos, Angle(tangent))

    fun splineToSplineHeading(pose: Pose2d, tangent: Angle): WaypointBuilder {
        waypoints += SplineToSplineHeading(pose, tangent)
        return this
    }

    /**
     * @param tangent tangent in degrees or radians as specified by [Angle.defaultUnits]
     */
    fun splineToSplineHeading(pose: Pose2d, tangent: Double): WaypointBuilder =
        splineToSplineHeading(pose, Angle(tangent))

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

    fun turn(angle: Angle): WaypointBuilder {
        waypoints += Turn(angle)
        return this
    }

    /**
     * @param angle angle to turn in degrees or radians as specified by [Angle.defaultUnits]
     */
    fun turn(angle: Double): WaypointBuilder = turn(Angle(angle))

    fun wait(seconds: Double): WaypointBuilder {
        waypoints += Wait(seconds)
        return this
    }

    fun build() = WaypointTrajectory(waypoints, constraints)
}