package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.OpMode

/**
 * An OpMode that uses a [CommandScheduler]. If you're using a robot, try [RobotOpMode].
 */
abstract class CommandOpMode : OpMode(), CommandInterface {
    /**
     * The global [SuperTelemetry] instance.
     */
    @JvmField
    protected val telem: SuperTelemetry = CommandScheduler.telemetry

    /**
     * The FtcDashboard instance.
     */
    @JvmField
    protected val dashboard: FtcDashboard = FtcDashboard.getInstance()

    /**
     * A handy [MultipleGamepad].
     */
    @JvmField
    protected val gamepad: MultipleGamepad = MultipleGamepad(gamepad1, gamepad2)

    override fun internalPostInitLoop() = CommandScheduler.update()

    override fun internalPostLoop() = CommandScheduler.update()

    abstract override fun start()

    override fun loop() {}
}