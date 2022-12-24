package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.SwerveKinematics
import kotlin.jvm.JvmOverloads
import kotlin.math.abs
import kotlin.math.max

/**
 * Swerve-specific drive constraints that also limit maximum wheel velocities.
 *
 * @param maxWheelVel maximum wheel velocity
 * @param trackWidth track width
 * @param wheelBase wheel base
 */
open class SwerveVelocityConstraint @JvmOverloads constructor(
    val maxWheelVel: Double,
    val trackWidth: Double,
    val wheelBase: Double = trackWidth
) : TrajectoryVelocityConstraint {
    override fun get(pose: Pose2d, deriv: Pose2d, lastDeriv: Pose2d, ds: Double, baseRobotVel: Pose2d): Double {
        val wheel0 = SwerveKinematics.robotToWheelVelocities(baseRobotVel, trackWidth, wheelBase)
        if (wheel0.maxOf(::abs) >= maxWheelVel) {
            throw UnsatisfiableConstraint()
        }

        val robotDeriv = Kinematics.fieldToRobotVelocity(pose, deriv)

        val wheel = SwerveKinematics.robotToWheelVelocities(robotDeriv, trackWidth, wheelBase)
        return wheel0.zip(wheel).minOf {
            max(
                (maxWheelVel - it.first) / it.second,
                (-maxWheelVel - it.first) / it.second
            )
        }
    }
}