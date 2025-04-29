package com.amarcolini.joos.drive

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.localization.Localizer
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.abs

/**
 * Abstraction for generic robot drive motion and localization. Robot poses are specified in a coordinate system with
 * positive x pointing forward, positive y pointing left, and positive heading measured counter-clockwise from the
 * x-axis.
 */
interface Drive {
    companion object {
        /**
         * Scales [drivePower] so that the commanded powers are all within `[-1.0, 1.0]`.
         */
        @JvmOverloads
        @JvmStatic
        fun setWeightedDrivePower(
            drive: Drive,
            drivePower: Pose2d,
            xWeight: Double = 1.0,
            yWeight: Double = 1.0,
            headingWeight: Double = 1.0
        ) {
            var vel = drivePower

            if (abs(vel.x) + abs(vel.y) + abs(vel.heading.value) > 1) {
                val denom =
                    xWeight * abs(vel.x) + yWeight * abs(vel.y) + headingWeight * abs(vel.heading.value)
                vel = Pose2d(vel.x * xWeight, vel.y * yWeight, vel.heading * headingWeight) / denom
            }

            drive.setDrivePower(vel)
        }
    }

    /**
     * Localizer used to determine the evolution of the robot pose.
     */
    var localizer: Localizer

    /**
     * Sets the current commanded drive state of the robot. Feedforward is applied to [driveSignal] before it reaches
     * the motors.
     */
    fun setDriveSignal(driveSignal: DriveSignal)

    /**
     * Sets the current commanded drive state of the robot. Feedforward is *not* applied to [drivePower].
     */
    fun setDrivePower(drivePower: Pose2d)
}

var Drive.poseEstimate
    get() = localizer.poseEstimate
    set(value) {
        localizer.poseEstimate = value
    }

val Drive.poseVelocity get() = localizer.poseVelocity

fun Drive.setWeightedDrivePower(
    drivePower: Pose2d,
    xWeight: Double = 1.0,
    yWeight: Double = 1.0,
    headingWeight: Double = 1.0
) = Drive.setWeightedDrivePower(this, drivePower, xWeight, yWeight, headingWeight)