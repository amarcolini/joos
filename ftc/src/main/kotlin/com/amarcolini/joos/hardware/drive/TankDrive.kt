package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.drive.AbstractTankDrive
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.trajectory.constraints.TankConstraints


/**
 * A [Component] implementation of a tank drive.
 */
open class TankDrive @JvmOverloads constructor(
    @JvmField val leftMotors: MotorGroup,
    @JvmField val rightMotors: MotorGroup,
    trackWidth: Double = 1.0,
    externalHeadingSensor: AngleSensor? = null,
) : AbstractTankDrive(trackWidth, externalHeadingSensor), Component {
    @JvmOverloads
    constructor(
        left: MotorGroup,
        right: MotorGroup,
        constraints: TankConstraints,
        externalHeadingSensor: AngleSensor? = null
    ) : this(left, right, constraints.trackWidth, externalHeadingSensor)

    val motors: MotorGroup = MotorGroup(leftMotors, rightMotors)

    final override fun setMotorPowers(left: Double, right: Double) {
        leftMotors.setPower(left)
        rightMotors.setPower(right)
    }

    final override fun setWheelVelocities(velocities: List<Double>, accelerations: List<Double>) {
        leftMotors.setDistanceVelocity(velocities[0], accelerations[0])
        rightMotors.setDistanceVelocity(velocities[1], accelerations[1])
    }

    final override fun getWheelPositions(): List<Double> = listOf(
        leftMotors.currentPosition, rightMotors.currentPosition
    )

    final override fun update() {
        localizer.update()
        motors.forEach { it.update() }
    }
}