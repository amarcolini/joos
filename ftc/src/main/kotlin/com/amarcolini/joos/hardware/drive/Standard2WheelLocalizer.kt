package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.localization.TwoTrackingWheelLocalizer

/**
 * A utility class for creating 2-wheel tracking localizers with standard configurations.
 */
class Standard2WheelLocalizer(
    @JvmField val encoders: List<Motor.Encoder>,
    encoderPositions: List<Pose2d>,
    headingSensor: AngleSensor,
) : TwoTrackingWheelLocalizer(headingSensor, encoderPositions) {
    constructor(
        parallelEncoder: Motor.Encoder,
        parallelOffset: Double,
        perpendicularEncoder: Motor.Encoder,
        perpendicularOffset: Double,
        externalHeadingSensor: AngleSensor,
    ) : this(
        listOf(parallelEncoder, perpendicularEncoder),
        listOf(
            Pose2d(0.0, parallelOffset),
            Pose2d(perpendicularOffset, 0.0, Angle.quarterCircle)
        ),
        externalHeadingSensor,
    )

    override fun getWheelPositions(): List<Double> = encoders.map { it.distance }

    override fun getWheelVelocities(): List<Double> = encoders.map { it.distanceVelocity }
}