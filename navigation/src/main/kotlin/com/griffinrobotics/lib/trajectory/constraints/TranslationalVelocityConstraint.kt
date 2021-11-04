package com.griffinrobotics.lib.trajectory.constraints

import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.kinematics.Kinematics
import kotlin.math.sqrt

/**
 * Constraint limiting translational velocity.
 */
class TranslationalVelocityConstraint(
    val maxTranslationalVel: Double
) : TrajectoryVelocityConstraint {
    override fun get(s: Double, pose: Pose2d, deriv: Pose2d, baseRobotVel: Pose2d): Double {
        val v0 = baseRobotVel.vec().norm()
        if (v0 >= maxTranslationalVel) {
            throw UnsatisfiableConstraint()
        }

        val robotDeriv = Kinematics.fieldToRobotVelocity(pose, deriv)
        val b = baseRobotVel.vec() dot robotDeriv.vec()
        return sqrt(b * b - v0 * v0 + maxTranslationalVel * maxTranslationalVel) - b
    }
}
