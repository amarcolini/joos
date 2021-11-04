package com.griffinrobotics.lib.gui.trajectory

import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.geometry.Vector2d
import com.griffinrobotics.lib.path.LineSegment
import com.griffinrobotics.lib.path.QuinticSpline
import com.griffinrobotics.lib.path.heading.LinearInterpolator
import com.griffinrobotics.lib.path.heading.SplineInterpolator
import com.griffinrobotics.lib.path.heading.TangentInterpolator
import com.griffinrobotics.lib.trajectory.*
import com.griffinrobotics.lib.trajectory.config.GenericConfig
import com.griffinrobotics.lib.trajectory.config.TrajectoryConfig
import com.griffinrobotics.lib.trajectory.config.TrajectoryConstraints
import com.griffinrobotics.lib.util.Angle
import com.griffinrobotics.lib.util.NanoClock
import java.lang.IllegalStateException
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties

fun Pose2d.toKotlin(): String = String.format("Pose2d(%.2f, %.2f, %.5f)", x, y, heading)
fun Pose2d.toJava(): String = String.format("new Pose2d(%.2f, %.2f, %.5f)", x, y, heading)
fun Vector2d.toKotlin(): String = String.format("Vector2d(%.2f, %.2f)", x, y)
fun Vector2d.toJava(): String = String.format("new Vector2d(%.2f, %.2f)", x, y)
fun Waypoint.toKotlin(): String {
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

fun TrajectoryConstraints.toKotlin(): String {
    var string = this::class.simpleName + "("
    this::class.declaredMemberProperties.filter { it.returnType == Double::class.createType() }
        .forEach { prop ->
            string += prop.call(this).toString() + ", "
        }
    string = string.dropLast(2) + ")"
    return string
}

fun Waypoint.toJava(): String {
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

fun Collection<Waypoint>.toTrajectory(constraints: TrajectoryConstraints): Pair<Trajectory, Boolean> {
    val first = this.first()
    val start =
        if (first is Start) {
            first
        } else Start(Pose2d(), Degree(0.0))
    val builder = TrajectoryBuilder(
        start.pose, start.tangent.radians, constraints
    )

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
            it.isBad.set(false)
        } catch (e: Exception) {
            it.isBad.set(true)
            return try {
                builder.build() to false
            } catch (e: Exception) {
                builder.wait(0.0)
                builder.build() to false
            }
        }
    }
    return try {
        builder.build() to true
    } catch (e: Exception) {
        builder.wait(0.0)
        builder.build() to false
    }
}

//fun String.toWaypoints(): List<Waypoint> {
//
//}