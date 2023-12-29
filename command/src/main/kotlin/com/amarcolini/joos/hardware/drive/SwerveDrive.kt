package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.drive.AbstractSwerveDrive
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.hardware.Servo
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.trajectory.constraints.SwerveConstraints

/**
 * A [Component] implementation of a swerve drive.
 */
open class SwerveDrive @JvmOverloads constructor(
    frontLeft: Pair<Motor, Servo>,
    backLeft: Pair<Motor, Servo>,
    backRight: Pair<Motor, Servo>,
    frontRight: Pair<Motor, Servo>,
    trackWidth: Double = 1.0,
    wheelBase: Double = 1.0,
    externalHeadingSensor: AngleSensor? = null,
) : AbstractSwerveDrive(trackWidth, wheelBase, externalHeadingSensor), DriveComponent {
    @JvmOverloads
    constructor(
        frontLeft: Pair<Motor, Servo>,
        backLeft: Pair<Motor, Servo>,
        backRight: Pair<Motor, Servo>,
        frontRight: Pair<Motor, Servo>,
        constraints: SwerveConstraints,
        externalHeadingSensor: AngleSensor? = null,
    ) : this(
        frontLeft,
        backLeft,
        backRight,
        frontRight,
        constraints.trackWidth,
        constraints.wheelBase,
        externalHeadingSensor
    )

    /**
     * All the motors in this drive.
     */
    @JvmField
    protected val motors =
        MotorGroup(frontLeft.first, backLeft.first, backRight.first, frontRight.first)
    @JvmField
    protected val servos =
        listOf(frontLeft.second, backLeft.second, backRight.second, frontRight.second)

    override fun setRunMode(runMode: Motor.RunMode) {
        motors.forEach { it.runMode = runMode }
    }

    override fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior) {
        motors.forEach { it.zeroPowerBehavior = zeroPowerBehavior }
    }

    override fun setMotorPowers(frontLeft: Double, backLeft: Double, backRight: Double, frontRight: Double) {
        motors.zip(listOf(frontLeft, backLeft, backRight, frontRight)).forEach { (motor, power) ->
            motor.power = power
        }
    }

    override fun setWheelVelocities(velocities: List<Double>, accelerations: List<Double>) {
        motors.zip(velocities.zip(accelerations))
            .forEach { (motor, power) ->
                motor.setDistanceVelocity(power.first, power.second)
            }
    }

    override fun setModuleOrientations(frontLeft: Angle, backLeft: Angle, backRight: Angle, frontRight: Angle) {
        servos.zip(listOf(frontLeft, backLeft, backRight, frontRight)).forEach { (servo, angle) ->
            servo.angle = angle
        }
    }

    override fun getWheelPositions(): List<Double> = this.motors.map { it.distance }

    override fun getWheelVelocities(): List<Double> = this.motors.map { it.distanceVelocity }

    override fun getModuleOrientations(): List<Angle> = servos.map { it.angle }

    override fun update() {
        updatePoseEstimate()
        this.motors.forEach { it.update() }
    }
}