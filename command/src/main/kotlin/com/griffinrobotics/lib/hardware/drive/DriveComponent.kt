package com.griffinrobotics.lib.hardware.drive

import com.griffinrobotics.lib.command.*
import com.griffinrobotics.lib.drive.Drive
import com.griffinrobotics.lib.drive.DriveSignal
import com.griffinrobotics.lib.followers.TrajectoryFollower
import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.hardware.Imu
import com.griffinrobotics.lib.trajectory.PathTrajectorySegment
import com.griffinrobotics.lib.trajectory.Trajectory
import com.griffinrobotics.lib.trajectory.TrajectoryBuilder
import com.griffinrobotics.lib.trajectory.config.TrajectoryConfig
import com.griffinrobotics.lib.trajectory.config.TrajectoryConstraints
import com.qualcomm.hardware.bosch.BNO055IMU
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder
import kotlin.Double.Companion.NaN

/**
 * A [Component] implementation of [Drive].
 */
abstract class DriveComponent : Drive(), Component {
    protected abstract val trajectoryFollower: TrajectoryFollower
    abstract val constants: TrajectoryConstraints
    protected abstract val imu: Imu?

    override fun update(scheduler: CommandScheduler) {
        updatePoseEstimate()
    }

    fun trajectoryBuilder(
        startPose: Pose2d = poseEstimate,
        startHeading: Double = startPose.heading
    ) = TrajectoryBuilder(
        startPose,
        startHeading,
        constants.velConstraint,
        constants.accelConstraint,
        constants.maxAngVel, constants.maxAngAccel, constants.maxAngJerk
    )

    fun trajectoryBuilder(
        startPose: Pose2d = poseEstimate,
        reversed: Boolean
    ) = TrajectoryBuilder(
        startPose,
        reversed,
        constants.velConstraint,
        constants.accelConstraint,
        constants.maxAngVel, constants.maxAngAccel, constants.maxAngJerk
    )

    fun followTrajectory(trajectory: Trajectory): Command {
        return if (!trajectoryFollower.isFollowing()) {
            FunctionalCommand(
                init = { trajectoryFollower.followTrajectory(trajectory) },
                execute = { setDriveSignal(trajectoryFollower.update(poseEstimate, poseVelocity)) },
                isFinished = { !trajectoryFollower.isFollowing() },
                end = { setDriveSignal(DriveSignal()) },
                requirements = setOf(this)
            )
        } else Command.empty()
    }

    final override val rawExternalHeading: Double
        get() = imu?.heading ?: NaN

    final override fun getExternalHeadingVelocity() = imu?.headingVelocity
}