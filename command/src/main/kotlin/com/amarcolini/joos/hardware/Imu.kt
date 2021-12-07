package com.amarcolini.joos.hardware

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.localization.Localizer
import com.qualcomm.hardware.bosch.BNO055IMU
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import kotlin.math.abs

/**
 * A wrapper class for the [BNO055IMU] object in the FTC SDK.
 *
 * @param imu the IMU for this wrapper to use
 */
//TODO: Test Imu on actual hardware to test if it works.
class Imu constructor(val imu: BNO055IMU) {

    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     */
    constructor(hMap: HardwareMap, id: String) : this(
        hMap.get(BNO055IMU::class.java, id)
    )

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
    val localizer = object : Localizer {
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

    var axis: Axis = Axis.Y

    /**
     * Returns the heading of the IMU using the gyroscope.
     */
    val heading: Double
        get() {
            val orientation = imu.getAngularOrientation(
                AxesReference.INTRINSIC,
                AxesOrder.XYZ,
                AngleUnit.RADIANS
            )
            return when (axis) {
                Axis.X -> orientation.firstAngle
                Axis.Y -> orientation.secondAngle
                Axis.Z -> orientation.thirdAngle
            }.toDouble()
        }

    /**
     * Returns the heading velocity of IMU using the gyroscope.
     */
    val headingVelocity: Double
        get() {
            val velocity = imu.angularVelocity.toAngleUnit(AngleUnit.RADIANS)
            return when (axis) {
                Axis.X -> velocity.xRotationRate
                Axis.Y -> velocity.yRotationRate
                Axis.Z -> velocity.zRotationRate
            }.toDouble()
        }

    /**
     * Automatically sets the up axis of the IMU using the accelerometer.
     *
     * @return the computed up axis, or `null` if it failed
     */
    fun autoDetectUpAxis(): Axis? {
        val gravity = imu.gravity
        return when (listOf(gravity.xAccel, gravity.yAccel, gravity.zAccel).zip(Axis.values())
            .maxByOrNull { abs(it.first) }?.second) {
            Axis.X -> Axis.Y
            Axis.Y -> Axis.Z
            Axis.Z -> Axis.X
            null -> null
        }
    }
}
