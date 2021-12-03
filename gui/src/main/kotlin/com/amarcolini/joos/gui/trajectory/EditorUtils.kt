package com.amarcolini.joos.gui.trajectory

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.config.TrajectoryConfig
import com.amarcolini.joos.trajectory.config.TrajectoryConstraints
import kotlin.math.PI
import kotlin.reflect.full.declaredMemberProperties

internal fun Pose2d.toKotlin(): String = String.format("Pose2d(%.2f, %.2f, %.5f)", x, y, heading)
internal fun Pose2d.toJava(): String = String.format("new Pose2d(%.2f, %.2f, %.5f)", x, y, heading)
internal fun Vector2d.toKotlin(): String = String.format("Vector2d(%.2f, %.2f)", x, y)
internal fun Vector2d.toJava(): String = String.format("new Vector2d(%.2f, %.2f)", x, y)
internal fun Waypoint.toKotlin(): String {
    var string = (this::class.simpleName?.replaceFirstChar { it.lowercase() } ?: "") + "("
    this::class.declaredMemberProperties.forEach { prop ->
        when (val value = prop.call(this)) {
            is Pose2d -> string += value.toKotlin() + ", "
            is Vector2d -> string += value.toKotlin() + ", "
            is Degree -> string += "Math.toRadians(${value.value}), "
            is Double -> string += "$value, "
        }
    }
    string = string.dropLast(2) + ")"
    return string
}

internal fun TrajectoryConstraints.toKotlin() = toString().replace("\\w+=".toRegex(), "")
internal fun TrajectoryConstraints.toJava() = "new " + toString().replace("\\w+=".toRegex(), "")

internal fun Waypoint.toJava(): String {
    var string = (this::class.simpleName?.replaceFirstChar { it.lowercase() } ?: "") + "("
    this::class.declaredMemberProperties.forEach { prop ->
        when (val value = prop.call(this)) {
            is Pose2d -> string += value.toJava() + ", "
            is Vector2d -> string += value.toJava() + ", "
            is Degree -> string += "Math.toRadians(${value.value}), "
            is Double -> string += "$value, "
        }
    }
    string = string.dropLast(2) + ")"
    return string
}

fun TrajectoryConfig.toWaypoints(): List<Waypoint> {
    val list = ArrayList<Waypoint>()
    list.add(Start(this.startPose, Degree(this.startTangent, false)))
    this.waypoints.forEach {
        when (it) {
            is TrajectoryConfig.Back -> list += Back(it.back)
            is TrajectoryConfig.Forward -> list += Forward(it.forward)
            is TrajectoryConfig.Line -> when (it.lineTo.interpolationType) {
                TrajectoryConfig.HeadingInterpolationType.TANGENT -> list += LineTo(it.lineTo.pose.vec())
                TrajectoryConfig.HeadingInterpolationType.CONSTANT -> list += LineToConstantHeading(
                    it.lineTo.pose.vec()
                )
                TrajectoryConfig.HeadingInterpolationType.LINEAR -> list += LineToLinearHeading(
                    it.lineTo.pose
                )
                TrajectoryConfig.HeadingInterpolationType.SPLINE -> list += LineToSplineHeading(
                    it.lineTo.pose
                )
            }
            is TrajectoryConfig.Spline -> when (it.splineTo.interpolationType) {
                TrajectoryConfig.HeadingInterpolationType.TANGENT -> list += SplineTo(
                    it.splineTo.pose.vec(), Degree(it.splineTo.tangent, false)
                )
                TrajectoryConfig.HeadingInterpolationType.CONSTANT -> list += SplineToConstantHeading(
                    it.splineTo.pose.vec(), Degree(it.splineTo.tangent, false)
                )
                TrajectoryConfig.HeadingInterpolationType.LINEAR -> list += SplineToLinearHeading(
                    it.splineTo.pose, Degree(it.splineTo.tangent, false)
                )
                TrajectoryConfig.HeadingInterpolationType.SPLINE -> list += SplineToSplineHeading(
                    it.splineTo.pose, Degree(it.splineTo.tangent, false)
                )
            }
            is TrajectoryConfig.StrafeLeft -> list += StrafeLeft(it.strafeLeft)
            is TrajectoryConfig.StrafeRight -> list += StrafeRight(it.strafeRight)
            is TrajectoryConfig.Turn -> list += Turn(Degree(it.turn, false))
            is TrajectoryConfig.Wait -> list += Wait(it.wait)
        }
    }
    return list
}

fun Collection<Waypoint>.toConfig(constraints: TrajectoryConstraints): TrajectoryConfig {
    val start = this.first() as Start
    return TrajectoryConfig(
        start.pose, start.tangent.radians,
        this.filter { it !is Start }.map {
            when (it) {
                is StrafeLeft -> TrajectoryConfig.StrafeLeft(it.distance)
                is StrafeRight -> TrajectoryConfig.StrafeRight(it.distance)
                is Forward -> TrajectoryConfig.Forward(it.distance)
                is Back -> TrajectoryConfig.Back(it.distance)
                is LineTo -> TrajectoryConfig.Line(
                    TrajectoryConfig.LineData(Pose2d(it.pos))
                )
                is StrafeTo -> TrajectoryConfig.Line(
                    TrajectoryConfig.LineData(
                        Pose2d(it.pos),
                        TrajectoryConfig.HeadingInterpolationType.CONSTANT
                    )
                )
                is LineToConstantHeading -> TrajectoryConfig.Line(
                    TrajectoryConfig.LineData(
                        Pose2d(it.pos),
                        TrajectoryConfig.HeadingInterpolationType.CONSTANT
                    )
                )
                is LineToLinearHeading -> TrajectoryConfig.Line(
                    TrajectoryConfig.LineData(
                        it.pose,
                        TrajectoryConfig.HeadingInterpolationType.LINEAR
                    )
                )
                is LineToSplineHeading -> TrajectoryConfig.Line(
                    TrajectoryConfig.LineData(
                        it.pose,
                        TrajectoryConfig.HeadingInterpolationType.SPLINE
                    )
                )
                is SplineTo -> TrajectoryConfig.Spline(
                    TrajectoryConfig.SplineData(
                        Pose2d(it.pos),
                        it.tangent.radians,
                    )
                )
                is SplineToConstantHeading -> TrajectoryConfig.Spline(
                    TrajectoryConfig.SplineData(
                        Pose2d(it.pos),
                        it.tangent.radians,
                        TrajectoryConfig.HeadingInterpolationType.CONSTANT
                    )
                )
                is SplineToLinearHeading -> TrajectoryConfig.Spline(
                    TrajectoryConfig.SplineData(
                        it.pose,
                        it.tangent.radians,
                        TrajectoryConfig.HeadingInterpolationType.LINEAR
                    )
                )
                is SplineToSplineHeading -> TrajectoryConfig.Spline(
                    TrajectoryConfig.SplineData(
                        it.pose,
                        it.tangent.radians,
                        TrajectoryConfig.HeadingInterpolationType.SPLINE
                    )
                )
                is Wait -> TrajectoryConfig.Wait(it.seconds)
                is Turn -> TrajectoryConfig.Turn(it.angle.radians)
                else -> throw IllegalStateException()
            }
        }, constraints
    )
}

internal fun Collection<Waypoint>.toTrajectory(
    constraints: TrajectoryConstraints,
    resolution: Double = 0.25,
): Trajectory? {
    val first = this.firstOrNull()
    val start =
        if (first is Start) {
            first
        } else Start(Pose2d(), Degree(0.0))
    val builder = TrajectoryBuilder(
        start.pose, start.tangent.radians, constraints, resolution
    )
    if (size < 2)
        builder.wait(0.0)

    this.forEach {
        try {
            when (it) {
                is Turn -> builder.turn(it.angle.radians)
                is Wait -> builder.wait(it.seconds)
                is LineTo -> builder.lineTo(it.pos)
                is LineToConstantHeading -> builder.lineToConstantHeading(it.pos)
                is LineToLinearHeading -> builder.lineToLinearHeading(it.pose)
                is LineToSplineHeading -> builder.lineToSplineHeading(it.pose)
                is SplineTo -> builder.splineTo(it.pos, it.tangent.radians)
                is SplineToConstantHeading -> builder.splineToConstantHeading(
                    it.pos,
                    it.tangent.radians
                )
                is SplineToLinearHeading -> builder.splineToLinearHeading(
                    it.pose,
                    it.tangent.radians
                )
                is SplineToSplineHeading -> builder.splineToSplineHeading(
                    it.pose,
                    it.tangent.radians
                )
                is Back -> builder.back(it.distance)
                is Forward -> builder.forward(it.distance)
                is StrafeLeft -> builder.strafeLeft(it.distance)
                is StrafeRight -> builder.strafeRight(it.distance)
                is StrafeTo -> builder.strafeTo(it.pos)
                is Start -> {
                }
            }
            it.error.value = null
        } catch (e: Exception) {
            it.error.value = e.message ?: e::class.simpleName
            return try {
                builder.build()
            } catch (e: Exception) {
                it.error.value = e.message ?: e::class.simpleName
                null
            }
        }
    }
    return try {
        builder.build()
    } catch (e: Exception) {
        lastOrNull()?.error?.value = e.message ?: e::class.simpleName
        null
    }
}

internal fun Collection<Waypoint>.mapPose(): List<Pair<Waypoint, Pair<Pose2d, Degree>>> {
    val first = this.firstOrNull()
    val start =
        if (first is Start) {
            first
        } else Start(Pose2d(), Degree(0.0))
    var pose = start.pose
    var tangent = start.tangent.radians

    val list = ArrayList<Pair<Waypoint, Pair<Pose2d, Degree>>>()
    list += start to (pose to Degree(tangent))

    this.forEach {
        when (it) {
            is LineTo -> {
                pose = Pose2d(it.pos, pose.heading)
                tangent = pose.heading
            }
            is LineToConstantHeading -> {
                pose = Pose2d(it.pos, pose.heading)
                tangent = pose.heading
            }
            is LineToLinearHeading -> {
                pose = it.pose
                tangent = pose.heading
            }
            is LineToSplineHeading -> {
                pose = it.pose
                tangent = pose.heading
            }
            is SplineTo -> {
                pose = Pose2d(it.pos, it.tangent.radians)
                tangent = it.tangent.radians
            }
            is SplineToConstantHeading -> {
                pose = Pose2d(it.pos, pose.heading)
                tangent = it.tangent.radians
            }
            is SplineToLinearHeading -> {
                pose = it.pose
                tangent = it.tangent.radians
            }
            is SplineToSplineHeading -> {
                pose = it.pose
                tangent = it.tangent.radians
            }
            is Back -> {
                pose = Pose2d(pose.vec() + Vector2d.polar(-it.distance, pose.heading), pose.heading)
                tangent = pose.heading
            }
            is Forward -> {
                pose = Pose2d(pose.vec() + Vector2d.polar(it.distance, pose.heading), pose.heading)
                tangent = pose.heading
            }
            is StrafeLeft -> {
                pose = Pose2d(
                    pose.vec() + Vector2d.polar(it.distance, pose.heading + PI / 2),
                    pose.heading
                )
                tangent = pose.heading
            }
            is StrafeRight -> {
                pose = Pose2d(
                    pose.vec() + Vector2d.polar(-it.distance, pose.heading + PI / 2),
                    pose.heading
                )
                tangent = pose.heading
            }
            is StrafeTo -> {
                pose = Pose2d(it.pos, pose.heading)
                tangent = pose.heading
            }
            is Turn -> {
                pose = Pose2d(pose.vec(), pose.heading + it.angle.radians)
                tangent = pose.heading
            }
            else -> {
            }
        }
        list += it to (pose to Degree(tangent, false))
    }
    return list
}