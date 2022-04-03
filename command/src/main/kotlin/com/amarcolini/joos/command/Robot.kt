package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlin.reflect.full.hasAnnotation

/**
 * A class that makes any command-based robot code a lot smoother
 * and easier to understand.
 */
abstract class Robot(opMode: OpMode) : CommandScheduler() {
    /**
     * A [MultipleGamepad] storing both OpMode gamepads for convenience.
     */
    @JvmField
    val gamepad: MultipleGamepad = MultipleGamepad(opMode.gamepad1, opMode.gamepad2)

    /**
     * Whether the current OpMode is a teleop OpMode.
     */
    @JvmField
    val isInTeleOp: Boolean = opMode::class.hasAnnotation<TeleOp>()

    /**
     * Whether the current OpMode is an autonomous OpMode.
     */
    @JvmField
    val isInAutonomous: Boolean = opMode::class.hasAnnotation<Autonomous>()

    /**
     * The hardware map obtained from the OpMode.
     */
    @JvmField
    val hMap: HardwareMap = opMode.hardwareMap

    @JvmField
    val dashboard: FtcDashboard = FtcDashboard.getInstance()

    init {
        register(gamepad)
        telemetry.register(opMode.telemetry)
    }

    /**
     * This method is runs when the current OpMode initializes.
     */
    abstract fun init()

    /**
     * This method is run as soon as the current OpMode starts.
     */
    abstract fun start()
}