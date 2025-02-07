package com.amarcolini.joos.extensions

import com.amarcolini.joos.command.CommandScheduler
import com.amarcolini.joos.dashboard.SuperTelemetry
import com.amarcolini.joos.hardware.IMUAngleSensor
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.LogoFacingDirection
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot.UsbFacingDirection
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.IMU

val OpMode.hMap: HardwareMap get() = hardwareMap

val OpMode.telem: SuperTelemetry get() = CommandScheduler.telem

inline operator fun <reified T : Any> HardwareMap.invoke(id: String): T = this.get(T::class.java, id)

inline fun <reified T : Any> HardwareMap.getAll(): List<T> = this.getAll(T::class.java)

fun getIMUAngleSensor(
    hMap: HardwareMap,
    id: String,
    logoFacingDirection: LogoFacingDirection,
    usbFacingDirection: UsbFacingDirection
) =
    hMap<IMU>(id).getAngleSensor(logoFacingDirection, usbFacingDirection)

fun IMU.getAngleSensor(): IMUAngleSensor = IMUAngleSensor(this)

fun IMU.getAngleSensor(
    logoFacingDirection: LogoFacingDirection,
    usbFacingDirection: UsbFacingDirection
): IMUAngleSensor =
    IMUAngleSensor(this, logoFacingDirection, usbFacingDirection)