package com.amarcolini.joos.kinematics

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.doLinearRegression
import com.amarcolini.joos.util.rad
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.PI


/**
 * Swerve drive kinematic equations. All wheel positions and velocities are given starting with front left and
 * proceeding counter-clockwise (i.e., front left, back left, back right, front right). Robot poses are specified in a
 * coordinate system with positive x pointing forward, positive y pointing left, and positive heading measured
 * counter-clockwise from the x-axis.
 */
@JsExport
object SwerveKinematics {

    /**
     * Computes the wheel velocity vectors corresponding to [robotVel] given the provided [trackWidth] and
     * [wheelBase].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToModuleVelocityVectors(
        robotVel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ): List<Vector2d> {
        val x = wheelBase / 2
        val y = trackWidth / 2

        val vx = robotVel.x
        val vy = robotVel.y
        val omega = robotVel.heading.radians

        return listOf(
            Vector2d(vx - omega * y, vy + omega * x),
            Vector2d(vx - omega * y, vy - omega * x),
            Vector2d(vx + omega * y, vy - omega * x),
            Vector2d(vx + omega * y, vy + omega * x)
        )
    }

    /**
     * Computes the wheel velocities corresponding to [robotVel] given the provided [trackWidth] and
     * [wheelBase].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToWheelVelocities(
        robotVel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth,
            wheelBase
        ).map(Vector2d::norm)

    /**
     * Computes the module orientations corresponding to [robotVel] given the provided
     * [trackWidth] and [wheelBase].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToModuleOrientations(
        robotVel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth,
            wheelBase
        ).map(Vector2d::angle)

    /**
     * Computes the acceleration vectors corresponding to [robotAccel] given the provided [trackWidth] and
     * [wheelBase].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToModuleAccelerationVectors(
        robotAccel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ): List<Vector2d> {
        val x = wheelBase / 2
        val y = trackWidth / 2

        val ax = robotAccel.x
        val ay = robotAccel.y
        val alpha = robotAccel.heading.radians

        return listOf(
            Vector2d(ax - alpha * y, ay + alpha * x),
            Vector2d(ax - alpha * y, ay - alpha * x),
            Vector2d(ax + alpha * y, ay - alpha * x),
            Vector2d(ax + alpha * y, ay + alpha * x)
        )
    }

    /**
     * Computes the wheel accelerations corresponding to [robotAccel] given the provided [trackWidth] and
     * [wheelBase].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToWheelAccelerations(
        robotVel: Pose2d,
        robotAccel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth,
            wheelBase
        ).zip(
            robotToModuleAccelerationVectors(
                robotAccel,
                trackWidth,
                wheelBase
            )
        )
            .map { (vel, accel) ->
                (vel dot accel) / vel.norm()
            }

    /**
     * Computes the module angular velocities corresponding to [robotAccel] given the provided [trackWidth]
     * and [wheelBase].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToModuleAngularVelocities(
        robotVel: Pose2d,
        robotAccel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth,
            wheelBase
        ).zip(
            robotToModuleAccelerationVectors(
                robotAccel,
                trackWidth,
                wheelBase
            )
        ).map { (vel, accel) ->
            ((vel cross accel) / (vel.squaredNorm())).rad
        }

    /**
     * Computes the robot velocities corresponding to [wheelVelocities], [moduleOrientations], and the drive parameters.
     *
     * @param wheelVelocities wheel velocities (or wheel position deltas)
     * @param moduleOrientations wheel orientations (in radians)
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun wheelToRobotVelocities(
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

    fun wheelToRobotVelocities2(
        wheelVelocities: List<Double>,
        moduleOrientations: List<Angle>,
        wheelPositions: List<Vector2d>
    ): Pose2d {
        val vectors = wheelVelocities
            .zip(moduleOrientations)
            .map { (vel, orientation) ->
                Vector2d.polar(vel, orientation)
            }
        val x = vectors.map { it.x }
        val y = vectors.map { it.y }
        val px = wheelPositions.map { it.x }
        val py = wheelPositions.map { it.y }
        val (omega1, vx) = doLinearRegression(py, x)
        val (omega2, vy) = doLinearRegression(px, y)
        return Pose2d(vx, vy, ((omega2 + omega1) / 2).rad)
    }

    fun <T> getPairs(inputArray: Array<T>): List<List<T>> {
        val pairs: MutableList<List<T>> = ArrayList()
        for (i in inputArray.indices) {
            for (j in i + 1 until inputArray.size) {
                val pair: MutableList<T> = ArrayList()
                pair.add(inputArray[i])
                pair.add(inputArray[j])
                pairs.add(pair)
            }
        }
        return pairs
    }

    fun wheelToRobotVelocities3(
        wheelVelocities: List<Double>,
        moduleOrientations: List<Angle>,
        wheelPositions: List<Vector2d>
    ): Pose2d {
        val modules = wheelVelocities
            .zip(moduleOrientations)
            .map { (vel, orientation) ->
                Vector2d.polar(vel, orientation)
            }
        // to account for errors, we will take the average of all the module pairs
        val modulePairs: List<List<Vector2d>> = getPairs(modules.toTypedArray())

        // lists to be averaged over
        val rotationValues: MutableList<Double> = ArrayList()
        val movementVectors: MutableList<Vector2d> = ArrayList()

        // for each pair of modules, calculate the movement vector and rotation speed
        for (pair in modulePairs) {
            // make it easier to access modules
            val module1: Vector2d = pair[0]
            val module2: Vector2d = pair[1]
            // also calculate the rotation vectors (needed for calculations)
            val module1RotationVector: Vector2d =
                wheelPositions[modules.indexOf(module1)].rotated(-Angle.quarterCircle)
            val module2RotationVector: Vector2d =
                wheelPositions[modules.indexOf(module2)].rotated((-PI / 2).rad)

            // calculate the difference between the module vectors
            val moduleDifference: Vector2d = module1 - module2
            // calculate the difference between the rotation vectors
            val rotationDifference: Vector2d = module1RotationVector - module2RotationVector

            // calculate the rotation speed
            val rotationSpeed: Double = moduleDifference.x / rotationDifference.x
            rotationValues.add(rotationSpeed)
            // calculate the movement vector using substitution
            movementVectors.add(module1 - module1RotationVector * rotationSpeed)
        }

        // average the rotation values
        val rotation: Double = rotationValues.average()

        // calculate the sum of the movement vectors
        var translation: Vector2d = Vector2d(0.0, 0.0)
        for (movementVector in movementVectors) {
            translation += movementVector
        }

        // average the movement vectors
        translation *= (1.0 / movementVectors.size)
        return Pose2d(translation, rotation.rad)
    }
    
    fun wheelToRobotVelocities4(
        wheelVelocities: List<Double>,
        moduleOrientations: List<Angle>,
        wheelPositions: List<Vector2d>
    ): Pose2d {
        val modules = wheelVelocities
            .zip(moduleOrientations)
            .map { (vel, orientation) ->
                Vector2d.polar(vel, orientation)
            }

        // take average of all modules (this is the robot's movement vector)
        var averageModulePosition: Vector2d = Vector2d(0.0, 0.0)
        for (module in modules) {
            averageModulePosition += module
        }
        averageModulePosition *= (1.0 / modules.size)


        // calculate the rotation speed
        var rotation = 0.0
        for (i in modules.indices) {
            val module: Vector2d = modules[i]
            val moduleVector: Vector2d = wheelPositions[i]
            // inverse of inverse (forwards) kinematics: undo the addition of rotation + movement
            val scaledRotationVector: Vector2d = module - (averageModulePosition)
            // add the scalar value
            rotation += scaledRotationVector.x / moduleVector.x
        }

        // average the rotation speed
        rotation /= modules.size
        return Pose2d(averageModulePosition, rotation.rad)
    }
}