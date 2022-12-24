package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.TankKinematics
import kotlin.math.abs
import kotlin.math.max

/**
 * Tank-specific drive constraints that also limit maximum wheel velocities.
 *
 * @param maxWheelVel maximum wheel velocity
 * @param trackWidth track width
 */
open class TankVelocityConstraint(
    val maxWheelVel: Double,
    val trackWidth: Double
) : TrajectoryVelocityConstraint {
    override fun get(pose: Pose2d, deriv: Pose2d, lastDeriv: Pose2d, ds: Double, baseRobotVel: Pose2d): Double {
        val wheel0 = TankKinematics.robotToWheelVelocities(baseRobotVel, trackWidth)
        if (wheel0.maxOf(::abs) >= maxWheelVel) {
            throw UnsatisfiableConstraint()
        }

        val robotDeriv = Kinematics.fieldToRobotVelocity(pose, deriv)
        val wheel = TankKinematics.robotToWheelVelocities(robotDeriv, trackWidth)
        return wheel0.zip(wheel).minOf {
            max(
                (maxWheelVel - it.first) / it.second,
                (-maxWheelVel - it.first) / it.second
            )
        }
    }
}