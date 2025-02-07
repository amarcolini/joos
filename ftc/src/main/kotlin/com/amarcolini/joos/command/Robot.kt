package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.amarcolini.joos.dashboard.SuperTelemetry
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.HardwareMap

/**
 * A class that makes any command-based robot code a lot smoother
 * and easier to understand.
 */
abstract class Robot : CommandInterface {
    /**
     * The current OpMode.
     */
    @JvmField
    val opMode: OpMode = CommandScheduler.opMode
        ?: throw IllegalStateException("A Robot cannot be instantiated without an active OpMode.")

    /**
     * Whether the current OpMode is a teleop OpMode.
     */
    @JvmField
    val isInTeleOp: Boolean = CommandScheduler.isInTeleOp

    /**
     * Whether the current OpMode is an autonomous OpMode.
     */
    @JvmField
    val isInAutonomous: Boolean = CommandScheduler.isInAutonomous

    /**
     * The current hardware map.
     */
    @JvmField
    protected val hMap: HardwareMap = opMode.hardwareMap

    /**
     * [SuperTelemetry].
     */
    @JvmField
    protected val telem = SuperTelemetry

    val gamepad: MultipleGamepad = CommandScheduler.gamepad
        ?: throw IllegalStateException("A Robot cannot be instantiated without an active OpMode.")
        @JvmName("gamepad") get

    @JvmField
    val dashboard: FtcDashboard? = FtcDashboard.getInstance()

    /**
     * This method is run when the current OpMode initializes. Automatically called by [CommandOpMode] if [CommandOpMode.registerRobot] is called.
     */
    open fun init() {}

    /**
     * This method is run as soon as the current OpMode starts. Automatically called by [CommandOpMode] if [CommandOpMode.registerRobot] is called.
     */
    open fun start() {}

    /**
     * This method is run after the current OpMode ends. Automatically called by [CommandOpMode] if [CommandOpMode.registerRobot] is called.
     */
    open fun stop() {}
}