package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.MecanumKinematics
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
    override fun get(s: Double, pose: Pose2d, deriv: Pose2d, baseRobotVel: Pose2d): Double {
        val wheel0 = MecanumKinematics.robotToWheelVelocities(
            baseRobotVel,
            trackWidth,
            wheelBase,
            lateralMultiplier
        )
        if (wheel0.map(::abs).maxOrNull()!! >= maxWheelVel) {
            throw UnsatisfiableConstraint()
        }

        val robotDeriv = Kinematics.fieldToRobotVelocity(pose, deriv)

        val wheel = MecanumKinematics.robotToWheelVelocities(
            robotDeriv,
            trackWidth,
            wheelBase,
            lateralMultiplier
        )
        return wheel0.zip(wheel).map {
            max(
                (maxWheelVel - it.first) / it.second,
                (-maxWheelVel - it.first) / it.second
            )
        }.minOrNull()!!
    }
}
