package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.DCMotorFeedforward
import com.amarcolini.joos.control.Feedforward
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.drive.AbstractDiffSwerveDrive
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.kinematics.DiffSwerveKinematics
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.trajectory.constraints.DiffSwerveConstraints

/**
 * A [Component] implementation of a differential swerve drive.
 */
open class DiffSwerveDrive @JvmOverloads constructor(
    private val leftModule: Pair<Motor, Motor>,
    private val rightModule: Pair<Motor, Motor>,
    moduleOrientationPID: PIDCoefficients,
    trackWidth: Double = 1.0,
    feedforward: Feedforward = DCMotorFeedforward(1.0),
    externalHeadingSensor: AngleSensor? = null,
) : AbstractDiffSwerveDrive(moduleOrientationPID, trackWidth, feedforward, externalHeadingSensor), DriveComponent {
    /**
     * Constructs a differential swerve drive from two [MotorGroup]s containing the motors of each module.
     */
    @JvmOverloads
    constructor(
        leftModule: MotorGroup,
        rightModule: MotorGroup,
        moduleOrientationPID: PIDCoefficients,
        trackWidth: Double = 1.0,
        feedforward: Feedforward = DCMotorFeedforward(1.0),
        externalHeadingSensor: AngleSensor? = null,
    ) : this(
        leftModule[0] to leftModule[1],
        rightModule[0] to rightModule[1],
        moduleOrientationPID, trackWidth, feedforward, externalHeadingSensor
    )

    /**
     * Constructs a differential swerve drive using [constraints].
     */
    @JvmOverloads
    constructor(
        leftModule: MotorGroup,
        rightModule: MotorGroup,
        moduleOrientationPID: PIDCoefficients,
        constraints: DiffSwerveConstraints,
        feedforward: Feedforward = DCMotorFeedforward(1.0),
        externalHeadingSensor: AngleSensor? = null,
    ) : this(
        leftModule[0] to leftModule[1],
        rightModule[0] to rightModule[1],
        moduleOrientationPID, constraints.trackWidth, feedforward, externalHeadingSensor
    )

    /**
     * All the motors in this drive.
     */
    @JvmField
    protected val motors: MotorGroup = MotorGroup(leftModule.first, leftModule.second, rightModule.first, rightModule.second)

    override fun setRunMode(runMode: Motor.RunMode): Unit = this.motors.forEach { it.runMode = runMode }

    override fun setZeroPowerBehavior(zeroPowerBehavior: Motor.ZeroPowerBehavior): Unit =
        motors.forEach { it.zeroPowerBehavior = zeroPowerBehavior }

    override fun getModuleOrientations(): Pair<Angle, Angle> = Pair(
        DiffSwerveKinematics.gearToModuleOrientation(
            leftModule.first.rotation,
            leftModule.second.rotation
        ),
        DiffSwerveKinematics.gearToModuleOrientation(
            rightModule.first.rotation,
            rightModule.second.rotation
        )
    )

    override fun setMotorPowers(topLeft: Double, bottomLeft: Double, topRight: Double, bottomRight: Double) {
        motors.zip(listOf(topLeft, bottomLeft, topRight, bottomRight)).forEach { (gear, power) ->
            gear.power = power
        }
    }

    override fun getGearRotations(): List<Angle> = motors.map { it.rotation }

    override fun getGearPositions(): List<Double> = motors.map { it.distance }

    override fun update() {
        updatePoseEstimate()
        motors.forEach { it.update() }
    }
}