package com.amarcolini.joos.localization

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.util.rad
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A class describing an abstract angle measuring device.
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

    var offset: Angle = 0.rad

    var reversed: Boolean = false

    /**
     * The raw angle measurement.
     */
    protected abstract fun getRawAngle(): Angle

    fun getAngle() = getRawAngle() * (if (reversed) -1.0 else 1.0) + offset

    /**
     * Sets [offset] so that [getAngle] returns [angle].
     */
    fun setAngle(angle: Angle) {
        offset = -getRawAngle() * (if (reversed) -1.0 else 1.0) + angle
    }

    open fun getAngularVelocity(): Angle? = null
}