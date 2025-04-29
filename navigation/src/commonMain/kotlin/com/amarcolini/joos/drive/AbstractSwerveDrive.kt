package com.amarcolini.joos.drive

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.kinematics.SwerveKinematics
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.localization.SwerveLocalizer
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmOverloads
import kotlin.math.sqrt

/**
 * This class provides the basic functionality of a swerve drive using [SwerveKinematics].
 *
 * @param modules the modules of the swerve drive, starting with the front left and moving counter-clockwise.
 * @param modulePositions the positions of all the modules relative to the center of rotation of the robot
 */
abstract class AbstractSwerveDrive @JvmOverloads constructor(
    val modules: List<SwerveModule>,
    protected val modulePositions: List<Vector2d>,
    protected val externalHeadingSensor: AngleSensor? = null
) : Drive {
    @JvmOverloads
    constructor(
        frontLeft: SwerveModule,
        backLeft: SwerveModule,
        backRight: SwerveModule,
        frontRight: SwerveModule,
        trackWidth: Double,
        wheelBase: Double = trackWidth,
        externalHeadingSensor: AngleSensor? = null
    ) : this(
        listOf(frontLeft, backLeft, backRight, frontRight),
        SwerveKinematics.getModulePositions(trackWidth, wheelBase),
        externalHeadingSensor
    )

    @JvmOverloads
    constructor(
        left: SwerveModule,
        right: SwerveModule,
        trackWidth: Double,
        externalHeadingSensor: AngleSensor? = null
    ) : this(
        listOf(left, right),
        listOf(Vector2d(0.0, -trackWidth / 2), Vector2d(0.0, trackWidth / 2)),
        externalHeadingSensor
    )

    init {
        require(modulePositions.size >= modules.size) {
            "All modules must have corresponding module positions."
        }
    }

    final override var localizer: Localizer = SwerveLocalizer(
        modules,
        modulePositions
    ).let { if (externalHeadingSensor != null) it.addHeadingSensor(externalHeadingSensor) else it }

    final override fun setDriveSignal(driveSignal: DriveSignal) {
        val vectors = SwerveKinematics.robotToModuleVelocityVectors(
            driveSignal.vel,
            modulePositions
        )
        val accelerations = SwerveKinematics.robotToWheelAccelerations(
            driveSignal.vel,
            driveSignal.accel,
            modulePositions
        )
        modules.mapIndexed { i, module ->
            val vec = vectors[i]
            module.setModuleOrientation(vec.angle())
            module.setWheelVelocity(vec.norm(), accelerations[i])
        }
    }

    final override fun setDrivePower(drivePower: Pose2d) {
        val actualDrivePower = drivePower.copy(heading = drivePower.heading.value.rad)
        val avg = sqrt(modulePositions.maxOf { it.squaredNorm() })
        val vectors =
            SwerveKinematics.robotToModuleVelocityVectors(actualDrivePower, modulePositions.map { it / avg })
        modules.zip(vectors) { module, vector ->
            module.setModuleOrientation(vector.angle())
            module.setDrivePower(vector.norm())
        }
    }

    fun setModuleOrientations(orientations: List<Angle>) {
        modules.zip(orientations) { module, orientation ->
            module.setModuleOrientation(orientation)
        }
    }

    fun getModuleOrientations(): List<Angle> = modules.map { it.getModuleOrientation() }
}