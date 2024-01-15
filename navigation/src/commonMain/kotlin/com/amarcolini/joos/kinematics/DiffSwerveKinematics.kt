package com.amarcolini.joos.kinematics

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.cos
import com.amarcolini.joos.util.rad
import com.amarcolini.joos.util.sin
import kotlin.jvm.JvmStatic

/**
 * Differential swerve drive kinematic equations. All wheel positions and velocities are given in (left, right) tuples.
 * Robot poses are specified in a coordinate system with positive x pointing forward, positive y pointing left,
 * and positive heading measured counter-clockwise from the x-axis. Note that for the differential, the wheel velocity
 * is half the difference between the top and bottom gear velocities, and the angular velocity is the half the sum
 * (assuming all gears are the same size).
 */
object DiffSwerveKinematics {
    /**
     * Computes a module's orientation using the total rotation of the top and bottom gears.
     */
    @JvmStatic
    fun gearToModuleOrientation(
        topGearRotation: Angle,
        bottomGearRotation: Angle
    ): Angle = (topGearRotation + bottomGearRotation) * 0.5

    /**
     * Computes the gear velocities required to achieve the given wheel velocity (without changing module orientation).
     */
    @JvmStatic
    fun wheelToGearVelocities(wheelVelocity: Double): List<Double> = listOf(
        wheelVelocity, -wheelVelocity
    )

    /**
     * Computes a module's wheel velocity using the corresponding gear velocities.
     */
    @JvmStatic
    fun gearToWheelVelocities(topGearVelocity: Double, bottomGearVelocity: Double): Double =
        (topGearVelocity - bottomGearVelocity) / 2

    /**
     * Computes the robot velocities corresponding to [gearRotations], [gearVelocities], and [trackWidth].
     *
     * @param gearRotations the total rotations of each gear
     * @param gearVelocities the current velocities of each gear, in linear distance units
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     */
    @JvmStatic
    fun gearToRobotVelocities(
        gearRotations: List<Angle>,
        gearVelocities: List<Double>,
        trackWidth: Double
    ): Pose2d {
        val leftOrientation = gearToModuleOrientation(gearRotations[0], gearRotations[1])
        val rightOrientation = gearToModuleOrientation(gearRotations[2], gearRotations[3])
        val leftVel = gearToWheelVelocities(gearVelocities[0], gearVelocities[1])
        val rightVel = gearToWheelVelocities(gearVelocities[2], gearVelocities[3])
        return SwerveKinematics.moduleToRobotVelocities(
            listOf(leftVel, rightVel),
            listOf(leftOrientation, rightOrientation),
            listOf(Vector2d(0.0, -trackWidth / 2), Vector2d(0.0, trackWidth / 2))
        )
    }
}