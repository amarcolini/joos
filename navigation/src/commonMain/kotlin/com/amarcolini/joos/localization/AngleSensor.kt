package com.amarcolini.joos.localization

import com.amarcolini.joos.geometry.Angle
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A class describing an abstract angle measuring device. This includes
 * the IMU in the Control/Expansion Hub as well as any other similar sensors.
 */
abstract class AngleSensor {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun from(getRawAngle: () -> Angle, getAngularVelocity: () -> Angle? = { null }) = object : AngleSensor() {
            override fun getRawAngle(): Angle = getRawAngle()

            override fun getAngularVelocity(): Angle? = getAngularVelocity()
        }
    }

    private var offset: Angle = Angle()

    /**
     * The raw angle measurement.
     */
    protected abstract fun getRawAngle(): Angle

    fun getAngle() = getRawAngle() + offset

    /**
     * Creates an offset so that [getAngle] returns [angle].
     */
    fun setAngle(angle: Angle) {
        offset = -getRawAngle() + angle
    }

    open fun getAngularVelocity(): Angle? = null
}