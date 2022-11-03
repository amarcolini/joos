package com.amarcolini.joos.hardware

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.localization.Localizer
import com.amarcolini.joos.util.rad
import com.qualcomm.hardware.bosch.BNO055IMU
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.Velocity
import kotlin.math.abs
import kotlin.math.sign

/**
 * A wrapper class for the [BNO055IMU] object in the FTC SDK.
 *
 * @param imu the IMU for this wrapper to use
 */
//TODO: Test Imu on actual hardware to see if it works.
class Imu constructor(val imu: BNO055IMU) {

    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     */
    constructor(hMap: HardwareMap, id: String) : this(
        hMap.get(BNO055IMU::class.java, id)
    )

    /**
     * The axes of the imu, where the Y axis points towards the motor ports,
     * the X axis points towards the USB port(s), and the Z axis points up out of the REV Hub/Control Hub.
     *
     *                           | Z axis
     *                           |
     *     (Motor Port Side)     |   / X axis
     *                       ____|__/____
     *          Y axis     / *   | /    /|   (IO Side)
     *          _________ /______|/    //      I2C
     *                   /___________ //     Digital
     *                  |____________|/      Analog
     *
     *                 (Servo Port Side)
     */
    enum class Axis {
        X, Y, Z
    }

    init {
        val parameters = BNO055IMU.Parameters()
        parameters.angleUnit = BNO055IMU.AngleUnit.RADIANS
        imu.initialize(parameters)
    }

    /**
     * A localizer that only uses the IMU to calculate position and orientation.
     */
    val localizer: Localizer by lazy {
        imu.startAccelerationIntegration(Position(), Velocity(), 100)
        object : Localizer {
            private var offset = Pose2d()
            override var poseEstimate: Pose2d = Pose2d()
                get() {
                    val position = imu.position.toUnit(DistanceUnit.INCH)
                    return Pose2d(
                        Vector2d(position.x, position.y),
                        heading
                    ) + offset
                }
                set(value) {
                    offset = value - poseEstimate
                    field = value
                }
            override val poseVelocity: Pose2d
                get() {
                    val velocity = imu.velocity.toUnit(DistanceUnit.INCH)
                    return Pose2d(
                        Vector2d(velocity.xVeloc, velocity.yVeloc),
                        headingVelocity
                    ) + offset
                }

            override fun update() {}
        }
    }

    /**
     * The axis to be used as heading.
     */
    var axis: Axis = Axis.Z

    /**
     * Whether the direction of the IMU should be reversed.
     */
    @JvmField
    var reversed: Boolean = false

    /**
     * Returns the heading of the IMU using the gyroscope.
     */
    val heading: Angle get() = imu.angularOrientation.firstAngle.toDouble().rad * if (reversed) -1.0 else 1.0

    /**
     * Returns the heading velocity of IMU using the gyroscope.
     */
    val headingVelocity: Angle
        get() {
            val velocity = imu.angularVelocity.toAngleUnit(AngleUnit.RADIANS)
            return (when (axis) {
                Axis.X -> velocity.xRotationRate
                Axis.Y -> velocity.yRotationRate
                Axis.Z -> velocity.zRotationRate
            } * if (reversed) -1.0 else 1.0).rad
        }

    /**
     * Automatically sets the up axis of the IMU using the accelerometer.
     *
     * @return the computed up axis, or `null` if it failed
     */
    fun autoDetectUpAxis(): Axis? {
        val gravity = imu.gravity
        val result = listOf(gravity.xAccel, gravity.yAccel, gravity.zAccel).zip(Axis.values())
            .maxByOrNull { abs(it.first) } ?: return null
        if (result.first.sign < 0) reversed = true
        return result.second
    }
}