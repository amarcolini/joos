package com.amarcolini.joos.kinematics

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.doLinearRegression
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Swerve drive kinematic equations. All wheel positions and velocities are given starting with front left and
 * proceeding counter-clockwise (i.e., front left, back left, back right, front right). Robot poses are specified in a
 * coordinate system with positive x pointing forward, positive y pointing left, and positive heading measured
 * counter-clockwise from the x-axis.
 */
object SwerveKinematics {
    /**
     * Returns the module positions of a swerve drive with four modules in a rectangular configuration.
     * @param trackWidth the distance between the left and right modules
     * @param wheelBase the distance between the front and back modules
     */
    @JvmStatic
    fun getModulePositions(
        trackWidth: Double,
        wheelBase: Double
    ): List<Vector2d> {
        val x = wheelBase / 2
        val y = trackWidth / 2
        return listOf(
            Vector2d(x, y),
            Vector2d(-x, y),
            Vector2d(-x, -y),
            Vector2d(x, -y),
        )
    }

    /**
     * Returns the modules positions of a swerve drive with two modules spaced [trackWidth] apart.
     */
    @JvmStatic
    fun getModulePositions(
        trackWidth: Double
    ): List<Vector2d> {
        val y = trackWidth / 2
        return listOf(Vector2d(0.0, y), Vector2d(0.0, -y))
    }

    /**
     * Computes the module velocity vectors corresponding to [robotVel] given the provided [modulePositions].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param modulePositions the positions of all the modules relative to the center of rotation of the robot
     */
    @JvmStatic
    fun robotToModuleVelocityVectors(
        robotVel: Pose2d,
        modulePositions: List<Vector2d>
    ): List<Vector2d> {
        val vx = robotVel.x
        val vy = robotVel.y
        val omega = robotVel.heading.radians

        return modulePositions.map { Vector2d(vx - omega * it.y, vy + omega * it.x) }
    }

    /**
     * Computes the wheel velocities corresponding to [robotVel] given the provided [modulePositions].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param modulePositions the positions of all the modules relative to the center of rotation of the robot
     */
    @JvmStatic
    fun robotToWheelVelocities(
        robotVel: Pose2d,
        modulePositions: List<Vector2d>
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            modulePositions
        ).map(Vector2d::norm)

    /**
     * Computes the module orientations corresponding to [robotVel] given the provided
     * [modulePositions].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param modulePositions the positions of all the modules relative to the center of rotation of the robot
     */
    @JvmStatic
    fun robotToModuleOrientations(
        robotVel: Pose2d,
        modulePositions: List<Vector2d>
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            modulePositions
        ).map(Vector2d::angle)

    /**
     * Computes the acceleration vectors corresponding to [robotAccel] given the provided [modulePositions].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param modulePositions the positions of all the modules relative to the center of rotation of the robot
     */
    @JvmStatic
    fun robotToModuleAccelerationVectors(
        robotAccel: Pose2d,
        modulePositions: List<Vector2d>
    ): List<Vector2d> {
        val ax = robotAccel.x
        val ay = robotAccel.y
        val alpha = robotAccel.heading.radians

        return modulePositions.map { Vector2d(ax - alpha * it.y, ay - alpha * it.x) }
    }

    /**
     * Computes the wheel accelerations corresponding to [robotAccel] given the provided [modulePositions].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param modulePositions the positions of all the modules relative to the center of rotation of the robot
     */
    @JvmStatic
    fun robotToWheelAccelerations(
        robotVel: Pose2d,
        robotAccel: Pose2d,
        modulePositions: List<Vector2d>
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            modulePositions,
        ).zip(
            robotToModuleAccelerationVectors(
                robotAccel,
                modulePositions
            )
        )
            .map { (vel, accel) ->
                (vel dot accel) / vel.norm()
            }

    /**
     * Computes the module angular velocities corresponding to [robotAccel] given the provided [modulePositions].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param modulePositions the positions of all the modules relative to the center of rotation of the robot
     */
    @JvmStatic
    fun robotToModuleAngularVelocities(
        robotVel: Pose2d,
        robotAccel: Pose2d,
        modulePositions: List<Vector2d>,
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            modulePositions
        ).zip(
            robotToModuleAccelerationVectors(
                robotAccel,
                modulePositions
            )
        ).map { (vel, accel) ->
            ((vel cross accel) / (vel.squaredNorm())).rad
        }

    /**
     * Computes the robot velocities corresponding to [wheelVelocities], [moduleOrientations], and the drive parameters.
     *
     * @param wheelVelocities wheel velocities (or wheel position deltas)
     * @param moduleOrientations module orientations
     * @param trackWidth lateral distance between pairs of modules on different sides of the robot
     * @param wheelBase distance between pairs of modules on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun moduleToRobotVelocities(
        wheelVelocities: List<Double>,
        moduleOrientations: List<Angle>,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ): Pose2d {
        val x = wheelBase / 2
        val y = trackWidth / 2

        val vectors = wheelVelocities
            .zip(moduleOrientations)
            .map { (vel, orientation) ->
                Vector2d.polar(vel, orientation)
            }

        val vx = vectors.sumOf { it.x } / 4
        val vy = vectors.sumOf { it.y } / 4
        val (frontLeft, backLeft, backRight, frontRight) = vectors
        val omega = ((
                y * (backRight.x + frontRight.x - frontLeft.x - backLeft.x) +
                        x * (frontLeft.y + frontRight.y - backLeft.y - backRight.y)
                ) / (4 * (x * x + y * y))).rad
        return Pose2d(vx, vy, omega)
    }

    /**
     * Computes the robot velocities corresponding to [wheelVelocities], [moduleOrientations], and the drive parameters.
     *
     * @param wheelVelocities wheel velocities (or wheel position deltas)
     * @param moduleOrientations module orientations
     * @param modulePositions the positions of all the modules relative to the center of rotation of the robot
     */
    @JvmStatic
    fun moduleToRobotVelocities(
        wheelVelocities: List<Double>,
        moduleOrientations: List<Angle>,
        modulePositions: List<Vector2d>
    ): Pose2d {
        val vectors = wheelVelocities
            .zip(moduleOrientations)
            .map { (vel, orientation) ->
                Vector2d.polar(vel, orientation)
            }
        val x = vectors.map { it.x }
        val y = vectors.map { it.y }
        val px = modulePositions.map { it.x }
        val py = modulePositions.map { it.y }
        val (omega1, vx) = doLinearRegression(py, x)
        val (omega2, vy) = doLinearRegression(px, y)
        return Pose2d(vx, vy, ((omega2 + omega1) / 2).rad)
    }
}