package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.OpMode

/**
 * A class that makes any command-based robot code a lot smoother
 * and easier to understand.
 */
abstract class Robot(opMode: OpMode) : CommandScheduler() {
    @JvmField
    val gamepad: MultipleGamepad = MultipleGamepad(opMode.gamepad1, opMode.gamepad2)

    @JvmField
    val hardwareMap = opMode.hardwareMap

    @JvmField
    val dashboard = FtcDashboard.getInstance()

    /**
     * The telemetry packet to use with FtcDashboard. Is automatically sent every update cycle.
     */
    var packet: TelemetryPacket = TelemetryPacket()
        private set

    @JvmField
    val telemetry = MultipleTelemetry(dashboard.telemetry, opMode.telemetry)

    init {
        register(object : Component {
            override fun update() {
                telemetry.update()
                dashboard.sendTelemetryPacket(packet)
                packet = TelemetryPacket()
            }
        })
    }
}