package com.amarcolini.joos.trajectory.config

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Configuration describing a basic trajectory.
 */
data class TrajectoryConfig(
    val startPose: Pose2d,
    val startTangent: Double = startPose.heading,
    val waypoints: List<Waypoint>,
    val constraints: TrajectoryConstraints,
    val resolution: Double = 0.25
) {
    // the file format changes relatively frequently
    // fortunately the contents are human-readable and can be manually translated when the format changes
    // TODO: major changes will trigger a version increment in 1.0.0+
    val version = 1

    /**
     * Heading interpolation for a specific trajectory configuration step.
     */
    enum class HeadingInterpolationType {
        Tangent,
        Constant,
        Linear,
        Spline
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes(
        JsonSubTypes.Type(Spline::class),
        JsonSubTypes.Type(Line::class),
        JsonSubTypes.Type(Turn::class),
        JsonSubTypes.Type(Wait::class)
    )
    /**
     * Description of a single segment of a composite trajectory.
     */
    sealed class Waypoint
    data class Spline(val splineTo: SplineData) : Waypoint()
    data class Line(val lineTo: LineData) : Waypoint()
    data class Forward(val forward: Double) : Waypoint()
    data class Back(val back: Double) : Waypoint()
    data class StrafeLeft(val strafeLeft: Double) : Waypoint()
    data class StrafeRight(val strafeRight: Double) : Waypoint()
    data class Turn(val turn: Double) : Waypoint()
    data class Wait(val wait: Double) : Waypoint()

    data class SplineData(
        val pose: Pose2d,
        val tangent: Double = pose.heading,
        val interpolationType: HeadingInterpolationType = HeadingInterpolationType.Tangent
    )

    data class LineData(
        val pose: Pose2d,
        val interpolationType: HeadingInterpolationType = HeadingInterpolationType.Tangent
    )

    @Suppress("ComplexMethod")
    fun toTrajectoryBuilder(): TrajectoryBuilder {
        val builder = TrajectoryBuilder(
            startPose,
            startTangent,
            constraints.velConstraint,
            constraints.accelConstraint,
            constraints.maxAngVel,
            constraints.maxAngAccel,
            constraints.maxAngJerk,
            resolution = resolution
        )

        for (i in waypoints.indices) {
            when (val waypoint = waypoints[i]) {
                is Spline -> {
                    val (pose, tangent, interpolationType) = waypoint.splineTo
                    when (interpolationType) {
                        HeadingInterpolationType.Tangent -> builder.splineTo(pose.vec(), tangent)
                        HeadingInterpolationType.Constant -> builder.splineToConstantHeading(
                            pose.vec(),
                            tangent
                        )
                        HeadingInterpolationType.Linear -> builder.splineToLinearHeading(
                            pose,
                            tangent
                        )
                        HeadingInterpolationType.Spline -> builder.splineToSplineHeading(
                            pose,
                            tangent
                        )
                    }
                }
                is Line -> {
                    val (pose, interpolationType) = waypoint.lineTo
                    when (interpolationType) {
                        HeadingInterpolationType.Tangent -> builder.lineTo(pose.vec())
                        HeadingInterpolationType.Constant -> builder.lineToConstantHeading(
                            pose.vec(),
                        )
                        HeadingInterpolationType.Linear -> builder.lineToLinearHeading(
                            pose,
                        )
                        HeadingInterpolationType.Spline -> builder.lineToSplineHeading(
                            pose
                        )
                    }
                }
                is Turn -> {
                    builder.turn(waypoint.turn)
                }
                is Wait -> {
                    builder.wait(waypoint.wait)
                }
                is Back -> builder.back(waypoint.back)
                is Forward -> builder.forward(waypoint.forward)
                is StrafeLeft -> builder.strafeLeft(waypoint.strafeLeft)
                is StrafeRight -> builder.strafeRight(waypoint.strafeRight)
            }
        }

        return builder
    }

    fun toTrajectory() = toTrajectoryBuilder().build()
}
