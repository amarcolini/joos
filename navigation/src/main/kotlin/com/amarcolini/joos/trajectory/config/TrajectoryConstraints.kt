package com.amarcolini.joos.trajectory.config

import com.amarcolini.joos.trajectory.constraints.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlin.reflect.KClass

/**
 * Configuration describing constraints and other robot-specific parameters that are shared by a group of trajectories.
 */
@JsonIgnoreProperties("velConstraint", "accelConstraint")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface TrajectoryConstraints {
    /**
     * Type of drivetrain.
     */
    enum class DriveType(val clazz: KClass<out TrajectoryConstraints>) {
        GENERIC(GenericConfig::class),
        MECANUM(MecanumConfig::class),
        SWERVE(SwerveConfig::class),
        TANK(TankConfig::class)
    }

    val type: DriveType

    val velConstraint: TrajectoryVelocityConstraint
    val accelConstraint: TrajectoryAccelerationConstraint

    val maxAngVel: Double
    val maxAngAccel: Double
    val maxAngJerk: Double
}

data class MecanumConfig(
    val maxRPM: Double = 0.0,
    val trackWidth: Double = 1.0,
    val wheelBase: Double = trackWidth,
    val lateralMultiplier: Double = 1.0,
    val maxVel: Double = 30.0,
    val maxAccel: Double = 30.0,
    override val maxAngVel: Double = Math.toRadians(180.0),
    override val maxAngAccel: Double = Math.toRadians(180.0),
    override val maxAngJerk: Double = 0.0
) : TrajectoryConstraints {
    override val velConstraint = MinVelocityConstraint(
        listOf(
            MecanumVelocityConstraint(maxRPM, trackWidth, wheelBase, lateralMultiplier),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )
    override val accelConstraint = ProfileAccelerationConstraint(maxAccel)
    override val type = TrajectoryConstraints.DriveType.MECANUM
}

data class GenericConfig(
    val maxVel: Double = 30.0,
    val maxAccel: Double = 30.0,
    override val maxAngVel: Double = Math.toRadians(180.0),
    override val maxAngAccel: Double = Math.toRadians(180.0),
    override val maxAngJerk: Double = 0.0
) : TrajectoryConstraints {
    override val velConstraint = TranslationalVelocityConstraint(maxVel)
    override val accelConstraint = ProfileAccelerationConstraint(maxAccel)
    override val type = TrajectoryConstraints.DriveType.GENERIC
}

data class TankConfig(
    val maxRPM: Double = 0.0,
    val trackWidth: Double = 1.0,
    val maxVel: Double = 30.0,
    val maxAccel: Double = 30.0,
    override val maxAngVel: Double = Math.toRadians(180.0),
    override val maxAngAccel: Double = Math.toRadians(180.0),
    override val maxAngJerk: Double = 0.0
) : TrajectoryConstraints {
    override val velConstraint = MinVelocityConstraint(
        listOf(
            TankVelocityConstraint(maxRPM, trackWidth),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )
    override val accelConstraint = ProfileAccelerationConstraint(maxAccel)
    override val type = TrajectoryConstraints.DriveType.TANK
}

data class SwerveConfig(
    val maxRPM: Double = 0.0,
    val trackWidth: Double = 1.0,
    val wheelBase: Double = trackWidth,
    val maxVel: Double = 30.0,
    val maxAccel: Double = 30.0,
    override val maxAngVel: Double = Math.toRadians(180.0),
    override val maxAngAccel: Double = Math.toRadians(180.0),
    override val maxAngJerk: Double = 0.0
) : TrajectoryConstraints {
    override val velConstraint = MinVelocityConstraint(
        listOf(
            SwerveVelocityConstraint(maxRPM, trackWidth, wheelBase),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )
    override val accelConstraint = ProfileAccelerationConstraint(maxAccel)
    override val type = TrajectoryConstraints.DriveType.SWERVE
}