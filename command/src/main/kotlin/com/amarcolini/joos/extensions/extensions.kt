@file:JvmName("HardwareUtil")

package com.amarcolini.joos.extensions

import com.amarcolini.joos.command.CommandScheduler
import com.amarcolini.joos.dashboard.SuperTelemetry
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.rad
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.LogoFacingDirection
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.UsbFacingDirection
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.IMU
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference

@get:JvmSynthetic
val OpMode.hMap: HardwareMap get() = hardwareMap

@get:JvmSynthetic
val OpMode.telem: SuperTelemetry get() = CommandScheduler.telem

@JvmSynthetic
inline operator fun <reified T : Any> HardwareMap.invoke(id: String): T = this.get(T::class.java, id)

@JvmSynthetic
inline fun <reified T : Any> HardwareMap.getAll(): List<T> = this.getAll(T::class.java)

fun IMU.getAngleSensor(): AngleSensor = AngleSensor.from(
    {
        this.getRobotOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.RADIANS).firstAngle.rad
    },
    {
        //We have to get the angular velocity in degrees because the SDK wraps the value in radians for no reason.
        this.getRobotAngularVelocity(AngleUnit.DEGREES).zRotationRate.deg
    }
)

fun IMU.getAngleSensor(logoFacingDirection: LogoFacingDirection, usbFacingDirection: UsbFacingDirection): AngleSensor {
    this.initialize(IMU.Parameters(RevHubOrientationOnRobot(logoFacingDirection, usbFacingDirection)))
    return this.getAngleSensor()
}