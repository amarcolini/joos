package com.amarcolini.joos.followers

import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.util.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Time-varying, non-linear feedback controller for nonholonomic drives. See equation 5.12 of
 * [Ramsete01.pdf](https://www.dis.uniroma1.it/~labrob/pub/papers/Ramsete01.pdf).
 *
 * @param b b parameter (non-negative)
 * @param zeta zeta parameter (on (0, 1))
 * @param admissibleError admissible/satisfactory pose error at the end of each move
 * @param timeout max time to wait for the error to be admissible
 * @param clock clock
 */
@JsExport
class RamseteFollower @JvmOverloads constructor(
    private val b: Double = 0.051,
    private val zeta: Double = 0.018,
    admissibleError: Pose2d = Pose2d(),
    timeout: Double = 0.0,
    clock: NanoClock = NanoClock.system
) : TrajectoryFollower(admissibleError, timeout, clock) {
    override var lastError: Pose2d = Pose2d()

    private fun sinc(x: Double) =
        if (x epsilonEquals 0.0) {
            1.0 - x * x / 6.0
        } else {
            sin(x) / x
        }

    override fun internalUpdate(currentPose: Pose2d, currentRobotVel: Pose2d?): DriveSignal {
        val t = elapsedTime()

        val targetPose = trajectory[t]
        val targetVel = trajectory.velocity(t)

        val targetRobotVel = Kinematics.fieldToRobotVelocity(targetPose, targetVel)

        val targetV = targetRobotVel.x
        val targetOmega = targetRobotVel.heading.radians

        val error = Kinematics.calculateFieldPoseError(targetPose, currentPose)

        val k1 = 2 * zeta * sqrt(targetOmega * targetOmega + b * targetV * targetV)
        val k3 = k1
        val k2 = b

        val v = targetV * cos(error.heading) +
                k1 * (cos(currentPose.heading) * error.x + sin(currentPose.heading) * error.y)
        val omega = targetOmega + k2 * targetV * sinc(error.heading.radians) *
                (cos(currentPose.heading) * error.y - sin(currentPose.heading) * error.x) +
                k3 * error.heading.radians

        lastError = Kinematics.calculateRobotPoseError(targetPose, currentPose)

        // TODO: is Ramsete acceleration FF worth?
        return DriveSignal(Pose2d(v, 0.0, omega.rad))
    }
}