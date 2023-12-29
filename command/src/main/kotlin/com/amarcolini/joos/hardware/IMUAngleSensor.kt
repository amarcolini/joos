package com.amarcolini.joos.hardware

import com.amarcolini.joos.extensions.invoke
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.rad
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.LogoFacingDirection
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.UsbFacingDirection
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.IMU
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference

/**
 * [AngleSensor] representation of [IMU].
 */
class IMUAngleSensor(@JvmField val imu: IMU) : AngleSensor() {
    constructor(imu: IMU, logoFacingDirection: LogoFacingDirection, usbFacingDirection: UsbFacingDirection) : this(
        imu.apply {
            initialize(IMU.Parameters(RevHubOrientationOnRobot(logoFacingDirection, usbFacingDirection)))
        }
    )

    constructor(hMap: HardwareMap, id: String) : this(hMap(id))

    constructor(
        hMap: HardwareMap,
        id: String,
        logoFacingDirection: LogoFacingDirection,
        usbFacingDirection: UsbFacingDirection
    ) :
            this(hMap(id), logoFacingDirection, usbFacingDirection)

    override fun getRawAngle(): Angle =
        imu.getRobotOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS).firstAngle.rad

    override fun getAngularVelocity(): Angle =
        //We have to get the angular velocity in degrees because the SDK wraps the value in radians for no reason.
        imu.getRobotAngularVelocity(AngleUnit.DEGREES).zRotationRate.deg
}