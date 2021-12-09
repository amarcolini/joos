package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.HardwareMap

/**
 * A class that makes any command-based robot code a lot smoother
 * and easier to understand.
 */
abstract class Robot(opMode: OpMode) : CommandScheduler() {
    @JvmField
    val gamepad: MultipleGamepad = MultipleGamepad(opMode.gamepad1, opMode.gamepad2)

    /**
     * The hardware map obtained from the opmode.
     */
    @JvmField
    val hMap: HardwareMap = opMode.hardwareMap

    @JvmField
    val dashboard: FtcDashboard = FtcDashboard.getInstance()

    /**
     * A telemetry for both FtcDashboard and the driver station. Automatically updates every update cycle.
     */
    @JvmField
    val telemetry = MultipleTelemetry(dashboard.telemetry, opMode.telemetry)

    init {
        register(Component.of {
            telemetry.update()
            telemetry.clear()
        }, gamepad)
    }
}