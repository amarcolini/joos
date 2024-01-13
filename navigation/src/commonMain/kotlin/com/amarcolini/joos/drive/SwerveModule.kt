package com.amarcolini.joos.drive

import com.amarcolini.joos.geometry.Angle

abstract class SwerveModule {
    /**
     * Sets the wheel motor power (normalized voltage) on the interval `[-1.0, 1.0]`.
     */
    abstract fun setDrivePower(power: Double)

    /**
     * Sets the wheel velocity (and acceleration) of the wheel motor.
     */
    abstract fun setWheelVelocity(velocities: List<Double>, accelerations: List<Double>)

    /**
     * The orientation of the module.
     */
    abstract var moduleOrientation: Angle

    /**
     * Returns the positions of the wheel in linear distance units.
     */
    abstract fun getWheelPosition(): Double

    /**
     * Returns the velocity of the wheel in linear distance units.
     */
    open fun getWheelVelocity(): Double? = null
}