package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.MecanumKinematics
import com.amarcolini.joos.kinematics.SwerveKinematics
import com.amarcolini.joos.kinematics.TankKinematics
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.max

/**
 * Drive constraint that limits maximum wheel velocities.
 *
 * @param maxWheelVel maximum wheel velocity
 */
class DriveVelocityConstraint(
    val maxWheelVel: Double,
    private val robotToWheelVelocities: (Pose2d) -> List<Double>
) : TrajectoryVelocityConstraint {
    companion object {
        @JvmStatic
        fun forMecanum(
            maxWheelVel: Double,
            trackWidth: Double,
            wheelBase: Double = trackWidth,
            lateralMultiplier: Double = 1.0
        ) = DriveVelocityConstraint(maxWheelVel) {
            MecanumKinematics.robotToWheelVelocities(
                it, trackWidth, wheelBase, lateralMultiplier
            )
        }

        @JvmStatic
        fun forTank(
            maxWheelVel: Double,
            trackWidth: Double
        ) = DriveVelocityConstraint(maxWheelVel) {
            TankKinematics.robotToWheelVelocities(it, trackWidth)
        }

        @JvmStatic
        fun forSwerve(
            maxWheelVel: Double,
            modulePositions: List<Vector2d>
        ) = DriveVelocityConstraint(maxWheelVel) {
            SwerveKinematics.robotToWheelVelocities(it, modulePositions)
        }
    }

    override fun get(pose: Pose2d, deriv: Pose2d, lastDeriv: Pose2d, ds: Double, baseRobotVel: Pose2d): Double {
        val wheel0 = robotToWheelVelocities(baseRobotVel)
        if (wheel0.maxOf(::abs) >= maxWheelVel) {
            throw UnsatisfiableConstraint()
        }

        val robotDeriv = Kinematics.fieldToRobotVelocity(pose, deriv)

        val wheel = robotToWheelVelocities(robotDeriv)
        return wheel0.zip(wheel).minOf {
            max(
                (maxWheelVel - it.first) / it.second,
                (-maxWheelVel - it.first) / it.second
            )
        }
    }
}