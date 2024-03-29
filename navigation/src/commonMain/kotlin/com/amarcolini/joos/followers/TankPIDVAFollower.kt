package com.amarcolini.joos.followers

import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.control.PIDController
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.rad
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.math.sign

/**
 * Traditional PID controller with feedforward velocity and acceleration components to follow a trajectory. More
 * specifically, one feedback loop controls the path displacement (that is, x in the robot reference frame), and
 * another feedback loop to minimize cross track (lateral) error via heading correction (overall, very similar to
 * [HolonomicPIDVAFollower] except adjusted for the nonholonomic constraint). Feedforward is applied at the wheel level.
 *
 * @param axialCoeffs PID coefficients for the robot axial (robot X) controller
 * @param crossTrackCoeffs PID coefficients for the robot heading controller based on cross track error
 * @param admissibleError admissible/satisfactory pose error at the end of each move
 * @param timeout max time to wait for the error to be admissible
 * @param clock clock
 */
@JsExport
class TankPIDVAFollower @JvmOverloads constructor(
    axialCoeffs: PIDCoefficients,
    crossTrackCoeffs: PIDCoefficients,
    admissibleError: Pose2d = Pose2d(),
    timeout: Double = 0.0,
    clock: NanoClock = NanoClock.system
) : TrajectoryFollower(admissibleError, timeout, clock) {
    private val axialController = PIDController(axialCoeffs)
    private val crossTrackController = PIDController(crossTrackCoeffs)

    override var lastError: Pose2d = Pose2d()

    override fun followTrajectory(trajectory: Trajectory) {
        axialController.reset()
        crossTrackController.reset()

        super.followTrajectory(trajectory)
    }

    override fun internalUpdate(currentPose: Pose2d, currentRobotVel: Pose2d?): DriveSignal {
        val t = elapsedTime()

        val targetPose = trajectory[t]
        val targetVel = trajectory.velocity(t)
        val targetAccel = trajectory.acceleration(t)

        val targetRobotVel = Kinematics.fieldToRobotVelocity(targetPose, targetVel)
        val targetRobotAccel =
            Kinematics.fieldToRobotAcceleration(targetPose, targetVel, targetAccel)

        val poseError = Kinematics.calculateRobotPoseError(targetPose, currentPose)

        // you can pass the error directly to PIDFController by setting setpoint = error and measurement = 0
        axialController.targetPosition = poseError.x
        crossTrackController.targetPosition = poseError.y

        axialController.targetVelocity = targetRobotVel.x
        crossTrackController.targetVelocity = targetRobotVel.y

        // note: feedforward is processed at the wheel level
        val axialCorrection = axialController.update(0.0, currentRobotVel?.x)
        val headingCorrection = sign(targetVel.vec() dot currentPose.headingVec()) *
                crossTrackController.update(0.0, currentRobotVel?.y)

        val correctedVelocity = targetRobotVel + Pose2d(
            axialCorrection,
            0.0,
            headingCorrection.rad
        )

        lastError = poseError

        return DriveSignal(correctedVelocity, targetRobotAccel)
    }
}