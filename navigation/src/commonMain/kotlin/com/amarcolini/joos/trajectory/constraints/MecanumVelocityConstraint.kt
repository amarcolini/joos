package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.MecanumKinematics
import kotlin.jvm.JvmOverloads
import kotlin.math.abs
import kotlin.math.max

/**
 * Mecanum-specific drive constraints that also limit maximum wheel velocities.
 *
 * @param maxWheelVel maximum wheel velocity
 * @param trackWidth track width
 * @param wheelBase wheel base
 * @param lateralMultiplier lateral multiplier
 */
open class MecanumVelocityConstraint @JvmOverloads constructor(
    val maxWheelVel: Double,
    val trackWidth: Double,
    val wheelBase: Double = trackWidth,
    val lateralMultiplier: Double = 1.0
) : TrajectoryVelocityConstraint {
    override fun get(pose: Pose2d, deriv: Pose2d, lastDeriv: Pose2d, ds: Double, baseRobotVel: Pose2d): Double {
        val wheel0 = MecanumKinematics.robotToWheelVelocities(
            baseRobotVel,
            trackWidth,
            wheelBase,
            lateralMultiplier
        )
        if (wheel0.maxOf(::abs) >= maxWheelVel) {
            throw UnsatisfiableConstraint()
        }

        val robotDeriv = Kinematics.fieldToRobotVelocity(pose, deriv)

        val wheel = MecanumKinematics.robotToWheelVelocities(
            robotDeriv,
            trackWidth,
            wheelBase,
            lateralMultiplier
        )
        return wheel0.zip(wheel).minOf {
            max(
                (maxWheelVel - it.first) / it.second,
                (-maxWheelVel - it.first) / it.second
            )
        }
    }
}