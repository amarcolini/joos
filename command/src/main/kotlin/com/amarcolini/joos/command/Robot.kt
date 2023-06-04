package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegistrar
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop
import kotlin.reflect.full.hasAnnotation

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
    val hMap: HardwareMap = opMode.hardwareMap

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