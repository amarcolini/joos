package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.localization.ThreeTrackingWheelLocalizer
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.rad

/**
 * A utility class for creating 3-wheel tracking localizers with standard configurations.
 */
class Standard3WheelLocalizer(
    @JvmField val encoders: List<Motor.Encoder>,
    encoderPositions: List<Pose2d>
) : ThreeTrackingWheelLocalizer(encoderPositions) {
    constructor(
        left: Motor.Encoder,
        right: Motor.Encoder,
        perpendicular: Motor.Encoder,
        lateralDistance: Double,
        forwardOffset: Double
    ) : this(
        listOf(
            left, right, perpendicular
        ), listOf(
            Pose2d(0.0, lateralDistance / 2, 0.rad),
            Pose2d(0.0, -lateralDistance / 2, 0.rad),
            Pose2d(forwardOffset, 0.0, 90.deg)
        )
    )

    override fun getWheelPositions(): List<Double> = encoders.map { it.distance }

    override fun getWheelVelocities(): List<Double> = encoders.map { it.distanceVelocity }
}