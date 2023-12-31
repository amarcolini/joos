package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Motor

/**
 * An interface representing a [Component] implementation of [Drive].
 */
interface DriveComponent : Component {
    var poseEstimate: Pose2d
    val poseVelocity: Pose2d?

    fun setDriveSignal(driveSignal: DriveSignal)
    fun setDrivePower(drivePower: Pose2d)
    fun setRunMode(runMode: Motor.RunMode)
    fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior)
}