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
    private val encoders: List<Motor.Encoder>,
    encoderPositions: List<Pose2d>
) : ThreeTrackingWheelLocalizer(encoderPositions) {
    constructor(
        encoders: List<Motor.Encoder>,
        lateralDistance: Double,
        forwardOffset: Double
    ) : this(encoders, listOf(
        Pose2d(0.0, lateralDistance / 2, 0.rad),
        Pose2d(0.0, -lateralDistance / 2, 0.rad),
        Pose2d(forwardOffset, 0.0, 90.deg)
    ))

    override fun getWheelPositions(): List<Double> = encoders.map { it.distance }

    override fun getWheelVelocities(): List<Double> = encoders.map { it.distanceVelocity }
}