package com.amarcolini.joos.gui.trajectory

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.AngleUnit
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.WaitSegment
import com.amarcolini.joos.trajectory.config.TrajectoryConfig
import com.amarcolini.joos.trajectory.constraints.TrajectoryConstraints
import com.amarcolini.joos.util.deg
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

internal fun Pose2d.toKotlin(): String =
    String.format("Pose2d(%.2f, %.2f, %s)", x, y, heading.toKotlin())

internal fun Pose2d.toJava(): String =
    String.format("Pose2d(%.2f, %.2f, %.2f)", x, y, heading.defaultValue)

internal fun Vector2d.toKotlin(): String = String.format("Vector2d(%.2f, %.2f)", x, y)
internal fun Vector2d.toJava(): String = String.format("new Vector2d(%.2f, %.2f)", x, y)
internal fun Angle.toKotlin(): String = String.format(
    "%.0f.%s", defaultValue, when (Angle.defaultUnits) {
        AngleUnit.Degrees -> "deg"
        AngleUnit.Radians -> "rad"
    }
)

internal fun Angle.toJava(): String = String.format("%.2f", defaultValue)
internal fun Waypoint.toKotlin(): String {
    var string = (this::class.simpleName?.replaceFirstChar { it.lowercase() } ?: "Waypoint") + "("
    this::class.declaredMemberProperties.filter { it.visibility == KVisibility.PUBLIC }.forEach { prop ->
        when (val value = prop.call(this)) {
            is Pose2d -> string += value.toKotlin() + ", "
            is Vector2d -> string += value.toKotlin() + ", "
            is Angle -> string += value.toKotlin() + ", "
            is Double -> string += String.format("%.2f, ", value)
        }
    }
    string = string.dropLast(2) + ")"
    return string
}

internal fun TrajectoryConstraints.toKotlin(): String {
    var string = (this::class.simpleName ?: "TrajectoryConstraints") + "("
    this::class.declaredMemberProperties.forEach { prop ->
        when (val value = prop.call(this)) {
            is Double -> string += String.format("%.2f, ", value)
            is Angle -> string += value.toKotlin() + ", "
        }
    }
    string = string.dropLast(2) + ")"
    return string
}

internal fun TrajectoryConstraints.toJava(): String {
    var string = "new" + (this::class.simpleName ?: "TrajectoryConstraints") + "("
    this::class.declaredMemberProperties.forEach { prop ->
        when (val value = prop.call(this)) {
            is Angle -> string += value.toJava() + ", "
            is Double -> string += String.format("%.2f, ", value)
        }
    }
    string = string.dropLast(2) + ")"
    return string
}

internal fun Waypoint.toJava(): String {
    var string = (this::class.simpleName?.replaceFirstChar { it.lowercase() } ?: "Waypoint") + "("
    this::class.declaredMemberProperties.filter { it.visibility == KVisibility.PUBLIC }.forEach { prop ->
        when (val value = prop.call(this)) {
            is Pose2d -> string += value.toJava() + ", "
            is Vector2d -> string += value.toJava() + ", "
            is Angle -> string += value.toJava() + ", "
            is Double -> string += String.format("%.2f, ", value)
        }
    }
    string = string.dropLast(2) + ")"
    return string
}

fun TrajectoryConfig.toWaypoints(): List<Waypoint> {
    val list = ArrayList<Waypoint>()
    list.add(Start(this.startPose, this.startTangent))
    this.waypoints.forEach {
        when (it) {
            is TrajectoryConfig.Back -> list += Back(it.back)
            is TrajectoryConfig.Forward -> list += Forward(it.forward)
            is TrajectoryConfig.Line -> when (it.lineTo.interpolationType) {
                TrajectoryConfig.HeadingInterpolationType.Tangent -> list += LineTo(it.lineTo.pose.vec())
                TrajectoryConfig.HeadingInterpolationType.Constant -> list += LineToConstantHeading(
                    it.lineTo.pose.vec()
                )
                TrajectoryConfig.HeadingInterpolationType.Linear -> list += LineToLinearHeading(
                    it.lineTo.pose
                )
                TrajectoryConfig.HeadingInterpolationType.Spline -> list += LineToSplineHeading(
                    it.lineTo.pose
                )
            }
            is TrajectoryConfig.Spline -> when (it.splineTo.interpolationType) {
                TrajectoryConfig.HeadingInterpolationType.Tangent -> list += SplineTo(
                    it.splineTo.pose.vec(), it.splineTo.tangent
                )
                TrajectoryConfig.HeadingInterpolationType.Constant -> list += SplineToConstantHeading(
                    it.splineTo.pose.vec(), it.splineTo.tangent
                )
                TrajectoryConfig.HeadingInterpolationType.Linear -> list += SplineToLinearHeading(
                    it.splineTo.pose, it.splineTo.tangent
                )
                TrajectoryConfig.HeadingInterpolationType.Spline -> list += SplineToSplineHeading(
                    it.splineTo.pose, it.splineTo.tangent
                )
            }
            is TrajectoryConfig.StrafeLeft -> list += StrafeLeft(it.strafeLeft)
            is TrajectoryConfig.StrafeRight -> list += StrafeRight(it.strafeRight)
            is TrajectoryConfig.Turn -> list += Turn(it.turn)
            is TrajectoryConfig.Wait -> list += Wait(it.wait)
        }
    }
    return list
}

fun Collection<Waypoint>.toConfig(constraints: TrajectoryConstraints): TrajectoryConfig {
    val start = this.first() as Start
    return TrajectoryConfig(
        start.pose, start.tangent,
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
                        TrajectoryConfig.HeadingInterpolationType.Constant
                    )
                )
                is LineToConstantHeading -> TrajectoryConfig.Line(
                    TrajectoryConfig.LineData(
                        Pose2d(it.pos),
                        TrajectoryConfig.HeadingInterpolationType.Constant
                    )
                )
                is LineToLinearHeading -> TrajectoryConfig.Line(
                    TrajectoryConfig.LineData(
                        it.pose,
                        TrajectoryConfig.HeadingInterpolationType.Linear
                    )
                )
                is LineToSplineHeading -> TrajectoryConfig.Line(
                    TrajectoryConfig.LineData(
                        it.pose,
                        TrajectoryConfig.HeadingInterpolationType.Spline
                    )
                )
                is SplineTo -> TrajectoryConfig.Spline(
                    TrajectoryConfig.SplineData(
                        Pose2d(it.pos),
                        it.tangent,
                    )
                )
                is SplineToConstantHeading -> TrajectoryConfig.Spline(
                    TrajectoryConfig.SplineData(
                        Pose2d(it.pos),
                        it.tangent,
                        TrajectoryConfig.HeadingInterpolationType.Constant
                    )
                )
                is SplineToLinearHeading -> TrajectoryConfig.Spline(
                    TrajectoryConfig.SplineData(
                        it.pose,
                        it.tangent,
                        TrajectoryConfig.HeadingInterpolationType.Linear
                    )
                )
                is SplineToSplineHeading -> TrajectoryConfig.Spline(
                    TrajectoryConfig.SplineData(
                        it.pose,
                        it.tangent,
                        TrajectoryConfig.HeadingInterpolationType.Spline
                    )
                )
                is Wait -> TrajectoryConfig.Wait(it.seconds)
                is Turn -> TrajectoryConfig.Turn(it.angle)
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
        } else Start()
    val builder = TrajectoryBuilder(
        start.pose, start.tangent, constraints, resolution
    )

    if (this.size > 1) {
        this.forEach {
            try {
                when (it) {
                    is Turn -> builder.turn(it.angle)
                    is Wait -> builder.wait(it.seconds)
                    is LineTo -> builder.lineTo(it.pos)
                    is LineToConstantHeading -> builder.lineToConstantHeading(it.pos)
                    is LineToLinearHeading -> builder.lineToLinearHeading(it.pose)
                    is LineToSplineHeading -> builder.lineToSplineHeading(it.pose)
                    is SplineTo -> builder.splineTo(it.pos, it.tangent)
                    is SplineToConstantHeading -> builder.splineToConstantHeading(
                        it.pos,
                        it.tangent
                    )
                    is SplineToLinearHeading -> builder.splineToLinearHeading(
                        it.pose,
                        it.tangent
                    )
                    is SplineToSplineHeading -> builder.splineToSplineHeading(
                        it.pose,
                        it.tangent
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
    } else return Trajectory(WaitSegment(start.pose, 0.0))
}

internal fun Collection<Waypoint>.mapPose(): List<Pair<Waypoint, Pair<Pose2d, Angle>>> {
    val first = this.firstOrNull()
    val start =
        if (first is Start) {
            first
        } else Start()
    var pose = start.pose
    var tangent = start.tangent

    val list = ArrayList<Pair<Waypoint, Pair<Pose2d, Angle>>>()
    list += start to (pose to tangent)

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
                pose = Pose2d(it.pos, pose.heading + (it.tangent - tangent))
                tangent = it.tangent
            }
            is SplineToConstantHeading -> {
                pose = Pose2d(it.pos, pose.heading)
                tangent = it.tangent
            }
            is SplineToLinearHeading -> {
                pose = it.pose
                tangent = it.tangent
            }
            is SplineToSplineHeading -> {
                pose = it.pose
                tangent = it.tangent
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
                    pose.vec() + Vector2d.polar(it.distance, pose.heading + 90.deg),
                    pose.heading
                )
                tangent = pose.heading
            }
            is StrafeRight -> {
                pose = Pose2d(
                    pose.vec() + Vector2d.polar(-it.distance, pose.heading + 90.deg),
                    pose.heading
                )
                tangent = pose.heading
            }
            is StrafeTo -> {
                pose = Pose2d(it.pos, pose.heading)
                tangent = pose.heading
            }
            is Turn -> {
                pose = Pose2d(pose.vec(), pose.heading + it.angle)
                tangent = pose.heading
            }
            else -> {
            }
        }
        list += it to (pose to tangent)
    }
    return list
}