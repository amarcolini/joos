package com.amarcolini.joos.kinematics

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.util.rad
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Mecanum drive kinematic equations. All wheel positions and velocities are given starting with front left and
 * proceeding counter-clockwise (i.e., front left, back left, back right, front right). Robot poses are specified in a
 * coordinate system with positive x pointing forward, positive y pointing left, and positive heading measured
 * counter-clockwise from the x-axis.
 *
 * [This paper](http://www.chiefdelphi.com/media/papers/download/2722) provides a motivated derivation.
 */
@JsExport
object MecanumKinematics {

    /**
     * Computes the wheel velocities corresponding to [robotVel] given the provided [trackWidth] and
     * [wheelBase].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     * @param lateralMultiplier multiplicative gain to adjust for systematic, proportional lateral error (gain greater
     * than 1.0 corresponds to overcompensation).
     */
    @JvmStatic
    @JvmOverloads
    fun robotToWheelVelocities(
        robotVel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth,
        lateralMultiplier: Double = 1.0
    ): List<Double> {
        val k = (trackWidth + wheelBase) / 2.0
        return listOf(
            robotVel.x - lateralMultiplier * robotVel.y - k * robotVel.heading.radians,
            robotVel.x + lateralMultiplier * robotVel.y - k * robotVel.heading.radians,
            robotVel.x - lateralMultiplier * robotVel.y + k * robotVel.heading.radians,
            robotVel.x + lateralMultiplier * robotVel.y + k * robotVel.heading.radians
        )
    }

    /**
     * Computes the wheel accelerations corresponding to [robotAccel] given the provided [trackWidth] and
     * [wheelBase].
     *
     * @param robotAccel acceleration of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     * @param lateralMultiplier multiplicative gain to adjust for systematic, proportional lateral error (gain greater
     * than 1.0 corresponds to overcompensation).
     */
    @JvmStatic
    @JvmOverloads
    // follows from linearity of the derivative
    fun robotToWheelAccelerations(
        robotAccel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth,
        lateralMultiplier: Double = 1.0
    ) =
        robotToWheelVelocities(
            robotAccel,
            trackWidth,
            wheelBase,
            lateralMultiplier
        )

    /**
     * Computes the robot velocity corresponding to [wheelVelocities] and the given drive parameters.
     *
     * @param wheelVelocities wheel velocities (or wheel position deltas)
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     * @param lateralMultiplier multiplicative gain to adjust for systematic, proportional lateral error (gain greater
     * than 1.0 corresponds to overcompensation).
     */
    @JvmStatic
    @JvmOverloads
    fun wheelToRobotVelocities(
        wheelVelocities: List<Double>,
        trackWidth: Double,
        wheelBase: Double = trackWidth,
        lateralMultiplier: Double = 1.0
    ): Pose2d {
        val k = (trackWidth + wheelBase) / 2.0
        val (frontLeft, backLeft, backRight, frontRight) = wheelVelocities
        return Pose2d(
            wheelVelocities.sum(),
            (backLeft + frontRight - frontLeft - backRight) / lateralMultiplier,
            ((backRight + frontRight - frontLeft - backLeft) / k).rad
        ) * 0.25
    }
}