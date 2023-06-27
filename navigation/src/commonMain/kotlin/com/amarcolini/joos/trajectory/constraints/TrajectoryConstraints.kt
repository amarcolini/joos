package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.util.deg
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads

/**
 * Configuration describing constraints and other robot-specific parameters.
 */
@JsExport
interface TrajectoryConstraints {
    @Transient
    val velConstraint: TrajectoryVelocityConstraint

    @Transient
    val accelConstraint: TrajectoryAccelerationConstraint

    val maxAngVel: Angle
    val maxAngAccel: Angle
    val maxAngJerk: Angle
}

@Serializable
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
        0.0, trackWidth, wheelBase, lateralMultiplier, maxVel, maxAccel, maxAngVel, maxAngAccel, maxAngJerk
    )

    @Transient
    override val velConstraint = MinVelocityConstraint(
        listOf(
            MecanumVelocityConstraint(maxWheelVel, trackWidth, wheelBase, lateralMultiplier),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
            AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
        )
    )

    @Transient
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}

@Serializable
@JsExport
data class GenericConstraints @JvmOverloads constructor(
    @JvmField val maxVel: Double = 30.0,
    @JvmField val maxAccel: Double = 30.0,
    override val maxAngVel: Angle = 180.deg,
    override val maxAngAccel: Angle = 180.deg,
    override val maxAngJerk: Angle = 0.deg
) : TrajectoryConstraints {
    @Transient
    override val velConstraint = MinVelocityConstraint(
        listOf(
            TranslationalVelocityConstraint(maxVel),
            AngularVelocityConstraint(maxAngVel),
            AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
        )
    )

    @Transient
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}

@Serializable
@JsExport
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
    @JsName("forMotors")
    constructor(
        trackWidth: Double = 1.0,
        maxVel: Double = 30.0,
        maxAccel: Double = 30.0,
        maxAngVel: Angle = 180.deg,
        maxAngAccel: Angle = 180.deg,
        maxAngJerk: Angle = 0.deg
    ) : this(
        0.0, trackWidth, maxVel, maxAccel, maxAngVel, maxAngAccel, maxAngJerk
    )

    @Transient
    override val velConstraint = MinVelocityConstraint(
        listOf(
            TankVelocityConstraint(maxWheelVel, trackWidth),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
            AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
        )
    )

    @Transient
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}

@Serializable
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
        0.0, trackWidth, wheelBase, maxVel, maxAccel, maxAngVel, maxAngAccel, maxAngJerk
    )

    @Transient
    override val velConstraint = MinVelocityConstraint(
        listOf(
            SwerveVelocityConstraint(maxWheelVel, trackWidth, wheelBase),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
            AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
        )
    )

    @Transient
    override val accelConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}

@Serializable
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
        0.0, trackWidth, maxVel, maxAccel, maxAngVel, maxAngAccel, maxAngJerk
    )

    override val velConstraint: TrajectoryVelocityConstraint = MinVelocityConstraint(
        listOf(
            DiffSwerveVelocityConstraint(maxGearVel, trackWidth),
            AngularVelocityConstraint(maxAngVel),
            TranslationalVelocityConstraint(maxVel),
            AngularAccelVelocityConstraint(maxAngAccel, maxAccel)
        )
    )

    override val accelConstraint: TrajectoryAccelerationConstraint = MinAccelerationConstraint(
        listOf(
            TranslationalAccelerationConstraint(maxAccel), AngularAccelerationConstraint(maxAngAccel)
        )
    )
}