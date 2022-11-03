package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.util.*
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

/**
 * Configuration describing constraints and other robot-specific parameters.
 */
//@JsonIgnoreProperties("velConstraint", "accelConstraint", "type")
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
//@JsonSubTypes(
//    JsonSubTypes.Type(GenericConstraints::class),
//    JsonSubTypes.Type(MecanumConstraints::class),
//    JsonSubTypes.Type(TankConstraints::class),
//    JsonSubTypes.Type(SwerveConstraints::class),
//    JsonSubTypes.Type(DiffSwerveConstraints::class)
//)
interface TrajectoryConstraints {
    /**
     * Type of drivetrain.
     */
    enum class DriveType(val clazz: KClass<out TrajectoryConstraints>) {
        GENERIC(GenericConstraints::class),
        MECANUM(MecanumConstraints::class),
        SWERVE(SwerveConstraints::class),
        DIFF_SWERVE(DiffSwerveConstraints::class),
        TANK(TankConstraints::class)
    }

    val type: DriveType

    val velConstraint: TrajectoryVelocityConstraint
    val accelConstraint: TrajectoryAccelerationConstraint

    val maxAngVel: Angle
    val maxAngAccel: Angle
    val maxAngJerk: Angle
}

data class MecanumConstraints @JvmOverloads constructor(
    @JvmField val maxWheelVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val wheelBase: Double,
    @JvmField val lateralMultiplier: Double,
    @JvmField val maxVel: Double,
    @JvmField val maxAccel: Double,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    @JvmOverloads
    constructor(
        trackWidth: Double = 1.0,
        wheelBase: Double = trackWidth,
        lateralMultiplier: Double = 1.0,
        maxVel: Double = 30.0,
        maxAccel: Double = 30.0,
        maxAngVel: Angle = 180.deg,
        maxAngAccel: Angle = 180.deg,
        maxAngJerk: Angle = 0.deg
    ) : this(
        0.0,
        trackWidth,
        wheelBase,
        lateralMultiplier,
        maxVel,
        maxAccel,
        maxAngVel,
        maxAngAccel,
        maxAngJerk
    )

    override val velConstraint = MinVelocityConstraint(
        listOf(
            MecanumVelocityConstraint(maxWheelVel, trackWidth, wheelBase, lateralMultiplier),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel),
            AngularAccelerationConstraint(maxAngAccel)
        )
    )
    override val type = TrajectoryConstraints.DriveType.MECANUM
}

data class GenericConstraints @JvmOverloads constructor(
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    override val velConstraint = MinVelocityConstraint(
        listOf(
            TranslationalVelocityConstraint(maxVel),
            AngularVelocityConstraint(maxAngVel)
        )
    )
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel),
            AngularAccelerationConstraint(maxAngAccel)
        )
    )
    override val type = TrajectoryConstraints.DriveType.GENERIC
}

data class TankConstraints @JvmOverloads constructor(
    @JvmField val maxWheelVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val maxVel: Double,
    @JvmField val maxAccel: Double,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    @JvmOverloads
    constructor(
        trackWidth: Double = 1.0,
        maxVel: Double = 30.0,
        maxAccel: Double = 30.0,
        maxAngVel: Angle = 180.deg,
        maxAngAccel: Angle = 180.deg,
        maxAngJerk: Angle = 0.deg
    ) : this(
        0.0,
        trackWidth,
        maxVel,
        maxAccel,
        maxAngVel,
        maxAngAccel,
        maxAngJerk
    )

    override val velConstraint = MinVelocityConstraint(
        listOf(
            TankVelocityConstraint(maxWheelVel, trackWidth),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel),
            AngularAccelerationConstraint(maxAngAccel)
        )
    )
    override val type = TrajectoryConstraints.DriveType.TANK
}

data class SwerveConstraints @JvmOverloads constructor(
    @JvmField val maxWheelVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val wheelBase: Double,
    @JvmField val maxVel: Double,
    @JvmField val maxAccel: Double,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    @JvmOverloads
    constructor(
        trackWidth: Double = 1.0,
        wheelBase: Double = trackWidth,
        maxVel: Double = 30.0,
        maxAccel: Double = 30.0,
        maxAngVel: Angle = 180.deg,
        maxAngAccel: Angle = 180.deg,
        maxAngJerk: Angle = 0.deg
    ) : this(
        0.0,
        trackWidth,
        wheelBase,
        maxVel,
        maxAccel,
        maxAngVel,
        maxAngAccel,
        maxAngJerk
    )

    override val velConstraint = MinVelocityConstraint(
        listOf(
            SwerveVelocityConstraint(maxWheelVel, trackWidth, wheelBase),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel),
            AngularAccelerationConstraint(maxAngAccel)
        )
    )
    override val type = TrajectoryConstraints.DriveType.SWERVE
}

data class DiffSwerveConstraints @JvmOverloads constructor(
    @JvmField val maxGearVel: Double,
    @JvmField val trackWidth: Double,
    @JvmField val maxVel: Double,
    @JvmField val maxAccel: Double,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    @JvmOverloads
    constructor(
        trackWidth: Double = 1.0,
        maxVel: Double = 30.0,
        maxAccel: Double = 30.0,
        maxAngVel: Angle = 180.deg,
        maxAngAccel: Angle = 180.deg,
        maxAngJerk: Angle = 0.deg
    ) : this(
        0.0,
        trackWidth,
        maxVel,
        maxAccel,
        maxAngVel,
        maxAngAccel,
        maxAngJerk
    )

    override val type = TrajectoryConstraints.DriveType.DIFF_SWERVE

    override val velConstraint: TrajectoryVelocityConstraint = MinVelocityConstraint(
        listOf(
            DiffSwerveVelocityConstraint(maxGearVel, trackWidth),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
        )
    )

    override val accelConstraint: TrajectoryAccelerationConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel),
            AngularAccelerationConstraint(maxAngAccel)
        )
    )
}