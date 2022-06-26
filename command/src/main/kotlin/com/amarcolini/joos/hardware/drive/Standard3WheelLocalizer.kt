package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.localization.ThreeTrackingWheelLocalizer
import com.amarcolini.joos.util.deg

/**
 * A utility class for creating 3-wheel tracking localizers with standard configurations.
 */
class Standard3WheelLocalizer @JvmOverloads constructor(
    private val left: Motor.Encoder,
    private val right: Motor.Encoder,
    private val top: Motor.Encoder,
    @JvmField var lateralDistance: Double,
    @JvmField var forwardOffset: Double,
    @JvmField var xMultiplier: Double = 1.0,
    @JvmField var yMultiplier: Double = 1.0
) : ThreeTrackingWheelLocalizer(
    listOf(
        Pose2d(0.0, lateralDistance / 2, 0.0),
        Pose2d(0.0, -lateralDistance / 2, 0.0),
        Pose2d(forwardOffset, 0.0, 90.deg)
    )
) {
    @JvmOverloads
    constructor(
        encoders: List<Motor.Encoder>, lateralDistance: Double, forwardOffset: Double,
        xMultiplier: Double = 1.0, yMultiplier: Double = 1.0
    ) : this(
        encoders[0], encoders[1], encoders[2], lateralDistance, forwardOffset, xMultiplier, yMultiplier
    )

    override fun getWheelPositions(): List<Double> = listOf(
        left.distance * xMultiplier, right.distance * xMultiplier, top.distance * yMultiplier
    )

    override fun getWheelVelocities(): List<Double> = listOf(
        left.distanceVelocity * xMultiplier, right.distanceVelocity * xMultiplier, top.distanceVelocity * yMultiplier
    )
}