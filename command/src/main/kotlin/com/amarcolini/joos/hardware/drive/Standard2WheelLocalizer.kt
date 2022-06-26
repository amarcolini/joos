package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.hardware.Imu
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.localization.TwoTrackingWheelLocalizer
import com.amarcolini.joos.util.deg

/**
 * A utility class for creating 2-wheel tracking localizers with standard configurations.
 */
class Standard2WheelLocalizer @JvmOverloads constructor(
    private val parallel: Motor.Encoder,
    @JvmField var parallelPosition: Vector2d,
    private val perpendicular: Motor.Encoder,
    @JvmField var perpendicularPosition: Vector2d,
    private val imu: Imu,
    @JvmField var xMultiplier: Double = 1.0,
    @JvmField var yMultiplier: Double = 1.0
) : TwoTrackingWheelLocalizer(
    listOf(
        Pose2d(parallelPosition),
        Pose2d(perpendicularPosition, 90.deg)
    )
) {
    @JvmOverloads
    constructor(
        encoders: List<Motor.Encoder>,
        parallelPosition: Vector2d,
        perpendicularPosition: Vector2d,
        imu: Imu,
        xMultiplier: Double = 1.0,
        yMultiplier: Double = 1.0
    ) : this(encoders[0], parallelPosition, encoders[1], perpendicularPosition, imu, xMultiplier, yMultiplier)

    override fun getWheelPositions(): List<Double> = listOf(
        parallel.distance * xMultiplier, perpendicular.distance * yMultiplier
    )

    override fun getWheelVelocities(): List<Double> = listOf(
        parallel.distanceVelocity * xMultiplier, perpendicular.distanceVelocity * yMultiplier
    )

    override fun getHeading(): Angle = imu.heading

    override fun getHeadingVelocity(): Angle = imu.headingVelocity
}