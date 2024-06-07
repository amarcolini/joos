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
    private val left: MotorGroup,
    private val right: MotorGroup,
    trackWidth: Double = 1.0,
    externalHeadingSensor: AngleSensor? = null,
) : AbstractTankDrive(trackWidth, externalHeadingSensor), DriveComponent {
    @JvmOverloads
    constructor(
        left: MotorGroup,
        right: MotorGroup,
        constraints: TankConstraints,
        externalHeadingSensor: AngleSensor? = null
    ) : this(left, right, constraints.trackWidth, externalHeadingSensor)

    override val motors: MotorGroup = MotorGroup(left, right)

    override fun setMotorPowers(left: Double, right: Double) {
        this.left.setPower(left)
        this.right.setPower(right)
    }

    override fun setWheelVelocities(velocities: List<Double>, accelerations: List<Double>) {
        left.setDistanceVelocity(velocities[0], accelerations[0])
        right.setDistanceVelocity(velocities[1], accelerations[1])
    }

    override fun getWheelPositions(): List<Double> = listOf(
        left.currentPosition, right.currentPosition
    )

    override fun update() {
        updatePoseEstimate()
        motors.forEach { it.update() }
    }
}