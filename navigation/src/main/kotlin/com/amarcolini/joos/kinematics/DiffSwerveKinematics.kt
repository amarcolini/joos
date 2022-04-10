package com.amarcolini.joos.kinematics

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.AngleUnit
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.cos
import com.amarcolini.joos.util.sin

/**
 * Differential swerve drive kinematic equations. All wheel positions and velocities are given in (left, right) tuples.
 * Robot poses are specified in a coordinate system with positive x pointing forward, positive y pointing left,
 * and positive heading measured counter-clockwise from the x-axis. Note that for the differential, the wheel velocity
 * is half the difference between the top and bottom gear velocities, and the angular velocity is the half the sum
 * (assuming all gears are the same size).
 */
object DiffSwerveKinematics {

    /**
     * Computes the wheel velocity vectors corresponding to [robotVel] given the provided [trackWidth].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     */
    @JvmStatic
    fun robotToModuleVelocityVectors(
        robotVel: Pose2d,
        trackWidth: Double,
    ): List<Vector2d> {
        val y = trackWidth / 2

        val vx = robotVel.x
        val vy = robotVel.y
        val omega = robotVel.heading.radians

        return listOf(
            Vector2d(vx - omega * y, vy),
            Vector2d(vx + omega * y, vy),
        )
    }

    /**
     * Computes the wheel velocities corresponding to [robotVel] given the provided [trackWidth].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     */
    @JvmStatic
    fun robotToWheelVelocities(
        robotVel: Pose2d,
        trackWidth: Double
    ): List<Double> =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth
        ).map(Vector2d::norm)

    /**
     * Computes the module orientations corresponding to [robotVel] given the provided [trackWidth].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     */
    @JvmStatic
    fun robotToModuleOrientations(
        robotVel: Pose2d,
        trackWidth: Double,
    ): List<Angle> =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth
        ).map(Vector2d::angle)

    /**
     * Computes the acceleration vectors corresponding to [robotAccel] given the provided [trackWidth].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     */
    @JvmStatic
    fun robotToModuleAccelerationVectors(
        robotAccel: Pose2d,
        trackWidth: Double,
    ): List<Vector2d> {
        val y = trackWidth / 2

        val ax = robotAccel.x
        val ay = robotAccel.y
        val alpha = robotAccel.heading.radians

        return listOf(
            Vector2d(ax - alpha * y, ay),
            Vector2d(ax + alpha * y, ay)
        )
    }

    /**
     * Computes the wheel accelerations corresponding to [robotAccel] given the provided [trackWidth].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     */
    @JvmStatic
    fun robotToWheelAccelerations(
        robotVel: Pose2d,
        robotAccel: Pose2d,
        trackWidth: Double,
    ): List<Double> =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth
        ).zip(
            robotToModuleAccelerationVectors(
                robotAccel,
                trackWidth
            )
        ).map { (vel, accel) ->
            (vel.x * accel.x + vel.y * accel.y) / vel.norm()
        }

    /**
     * Computes the module angular velocities corresponding to [robotAccel] given the provided [trackWidth].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     */
    @JvmStatic
    fun robotToModuleAngularVelocities(
        robotVel: Pose2d,
        robotAccel: Pose2d,
        trackWidth: Double
    ): List<Angle> =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth
        ).zip(
            robotToModuleAccelerationVectors(
                robotAccel,
                trackWidth
            )
        ).map { (vel, accel) ->
            Angle((vel.x * accel.y - vel.y * accel.x) / (vel.x * vel.x + vel.y * vel.y), AngleUnit.Radians)
        }

    /**
     * Computes the robot velocities corresponding to [wheelVelocities], [moduleOrientations], and [trackWidth].
     *
     * @param wheelVelocities wheel velocities (or wheel position deltas)
     * @param moduleOrientations wheel orientations
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     */
    @JvmStatic
    fun wheelToRobotVelocities(
        wheelVelocities: List<Double>,
        moduleOrientations: List<Angle>,
        trackWidth: Double
    ): Pose2d {
        val vectors = wheelVelocities
            .zip(moduleOrientations)
            .map { (vel, orientation) ->
                Vector2d(
                    vel * cos(orientation),
                    vel * sin(orientation)
                )
            }

        val (left, right) = vectors
        val vel = (left + right) * 0.5
        val omega = Angle((right.x - left.x) / trackWidth, AngleUnit.Radians)

        return Pose2d(vel.x, vel.y, omega)
    }

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
        return wheelToRobotVelocities(
            listOf(leftVel, rightVel),
            listOf(leftOrientation, rightOrientation),
            trackWidth
        )
    }
}