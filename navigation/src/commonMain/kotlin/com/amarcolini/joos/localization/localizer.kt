package com.amarcolini.joos.localization

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import kotlin.js.JsExport

/**
 * Generic interface for estimating robot pose over time.
 */
@JsExport
interface Localizer {

    /**
     * Current robot pose estimate.
     */
    var poseEstimate: Pose2d

    /**
     * Current robot pose velocity (optional)
     */
    val poseVelocity: Pose2d?

    /**
     * Completes a single localization update.
     */
    fun update()
}

interface DeadReckoningLocalizer : Localizer {
    /**
     * The last computed change in relative robot position.
     */
    val lastRobotPoseDelta: Pose2d

    /**
     * Creates a [HeadingLocalizer] using the provided heading sensor.
     */
    fun addHeadingSensor(headingSensor: AngleSensor) = HeadingLocalizer(headingSensor, this)
}

/**
 * A localizer that uses a [DeadReckoningLocalizer] with an added heading sensor to increase accuracy.
 */
class HeadingLocalizer(
    private val headingSensor: AngleSensor,
    private val deadReckoningLocalizer: DeadReckoningLocalizer,
) : Localizer {
    override var poseEstimate: Pose2d
        set(value) {
            deadReckoningLocalizer.poseEstimate = value
            headingSensor.setAngle(value.heading)
            _poseEstimate = value
        }
        get() = _poseEstimate
    private var _poseEstimate: Pose2d = Pose2d()
    override var poseVelocity: Pose2d? = null
        private set

    override fun update() {
        deadReckoningLocalizer.update()
        val robotPoseDelta = deadReckoningLocalizer.lastRobotPoseDelta

        val heading = headingSensor.getAngle()
        _poseEstimate =
            Kinematics.relativeOdometryUpdate(
                _poseEstimate,
                robotPoseDelta.copy(heading = heading - poseEstimate.heading)
            ).copy(heading = heading)
        poseVelocity =
            headingSensor.getAngularVelocity()?.let { deadReckoningLocalizer.poseVelocity?.copy(heading = it) }
    }

}