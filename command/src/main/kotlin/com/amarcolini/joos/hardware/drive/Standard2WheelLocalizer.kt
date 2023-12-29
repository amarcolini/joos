package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.localization.TwoTrackingWheelLocalizer
import com.amarcolini.joos.util.deg

/**
 * A utility class for creating 2-wheel tracking localizers with standard configurations.
 */
class Standard2WheelLocalizer(
    private val parallel: Motor.Encoder,
    parallelPosition: Vector2d,
    private val perpendicular: Motor.Encoder,
    perpendicularPosition: Vector2d,
    private val externalHeadingSensor: AngleSensor,
) : TwoTrackingWheelLocalizer(
    listOf(
        Pose2d(parallelPosition),
        Pose2d(perpendicularPosition, 90.deg)
    )
) {
    constructor(
        encoders: List<Motor.Encoder>,
        parallelPosition: Vector2d,
        perpendicularPosition: Vector2d,
        externalHeadingSensor: AngleSensor,
    ) : this(
        encoders[0],
        parallelPosition,
        encoders[1],
        perpendicularPosition,
        externalHeadingSensor,
    )

    override fun getWheelPositions(): List<Double> = listOf(
        parallel.distance, perpendicular.distance
    )

    override fun getWheelVelocities(): List<Double> = listOf(
        parallel.distanceVelocity, perpendicular.distanceVelocity
    )

    override fun getHeading(): Angle = externalHeadingSensor.getAngle()

    override fun getHeadingVelocity(): Angle? = externalHeadingSensor.getAngularVelocity()
}