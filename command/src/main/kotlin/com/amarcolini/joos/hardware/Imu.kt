package com.amarcolini.joos.hardware

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.localization.Localizer
import com.qualcomm.hardware.bosch.BNO055IMU
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.*
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
    val localizer: Localizer by lazy {
        imu.startAccelerationIntegration(Position(), Velocity(), 10)
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

    var axis: Axis = Axis.Z

    /**
     * Whether the direction of the IMU should be reversed.
     */
    @JvmField
    var reversed: Boolean = false

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
            } * if (reversed) -1.0 else 1.0
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
            } * if (reversed) -1.0 else 1.0
        }

    /**
     * Automatically sets the up axis of the IMU using the accelerometer.
     *
     * @return the computed up axis, or `null` if it failed
     */
    fun autoDetectUpAxis(): Axis? {
        val gravity = imu.gravity
        val autoAxis =
            when (listOf(gravity.xAccel, gravity.yAccel, gravity.zAccel).zip(Axis.values())
                .maxByOrNull { abs(it.first) }?.second) {
                Axis.X -> Axis.Y
                Axis.Y -> Axis.Z
                Axis.Z -> Axis.X
                null -> null
            }
        if (autoAxis != null) axis = autoAxis
        return autoAxis
    }
}
