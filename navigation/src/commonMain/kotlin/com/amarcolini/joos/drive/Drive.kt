package com.amarcolini.joos.drive

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.localization.Localizer
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Abstraction for generic robot drive motion and localization. Robot poses are specified in a coordinate system with
 * positive x pointing forward, positive y pointing left, and positive heading measured counter-clockwise from the
 * x-axis.
 */
@JsExport
abstract class Drive {
    /**
     * Localizer used to determine the evolution of [poseEstimate].
     */
    abstract var localizer: Localizer

    /**
     * The robot's current pose estimate.
     */
    var poseEstimate: Pose2d
        get() = localizer.poseEstimate
        set(value) {
            localizer.poseEstimate = value
        }

    /**
     *  Current robot pose velocity (optional)
     */
    val poseVelocity: Pose2d?
        get() = localizer.poseVelocity

    /**
     * Updates [poseEstimate] with the most recent positional change.
     */
    fun updatePoseEstimate() {
        localizer.update()
    }

    /**
     * Sets the current commanded drive state of the robot. Feedforward is applied to [driveSignal] before it reaches
     * the motors.
     */
    abstract fun setDriveSignal(driveSignal: DriveSignal)

    /**
     * Sets the current commanded drive state of the robot. Feedforward is *not* applied to [drivePower].
     */
    abstract fun setDrivePower(drivePower: Pose2d)

    /**
     * The heading velocity used to determine pose velocity in some cases
     */
    open fun getExternalHeadingVelocity(): Angle? = null
}