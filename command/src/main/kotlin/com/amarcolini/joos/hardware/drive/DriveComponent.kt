package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.hardware.MotorGroup

/**
 * An interface representing a [Component] implementation of [Drive].
 */
interface DriveComponent : Component {
    var poseEstimate: Pose2d
    val poseVelocity: Pose2d?

    /**
     * All the motors in this drive.
     */
    val motors: MotorGroup

    fun setDriveSignal(driveSignal: DriveSignal)
    fun setDrivePower(drivePower: Pose2d)
    fun setRunMode(runMode: Motor.RunMode)
    fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior)
}