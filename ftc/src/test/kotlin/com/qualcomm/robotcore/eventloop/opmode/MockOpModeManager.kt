package com.qualcomm.robotcore.eventloop.opmode

import com.amarcolini.joos.command.CommandScheduler
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl.ForceStopException
import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.robocol.TelemetryMessage
import mock.MockHardwareMap
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeServices

class MockOpModeManager(private val opMode: OpMode) :
    OpModeManagerNotifier, OpModeServices {
    var gamepad1 = Gamepad()
    var gamepad2 = Gamepad()

    private val listeners = mutableSetOf<OpModeManagerNotifier.Notifications>()
    override fun registerListener(listener: OpModeManagerNotifier.Notifications): OpMode {
        listeners.add(listener)
        return opMode
    }

    override fun unregisterListener(listener: OpModeManagerNotifier.Notifications?) {
        listeners.remove(listener)
    }

    val hMap = MockHardwareMap(this)

    init {
        registerListener(CommandScheduler)
    }

    fun setUpOpMode() {
        opMode.gamepad1 = gamepad1
        opMode.gamepad2 = gamepad2
        opMode.hardwareMap = hMap
        opMode.telemetry.clearAll()
        opMode.internalOpModeServices = this

        listeners.forEach {
            it.onOpModePreInit(opMode)
        }
        hMap.unsafeIterable().forEach {
            if (it is OpModeManagerNotifier.Notifications) it.onOpModePreInit(opMode)
        }
    }

    fun initOpMode() {
        opMode.internalInit()
    }

    fun startOpMode() {
        listeners.forEach {
            it.onOpModePreStart(opMode)
        }
        hMap.unsafeIterable().forEach {
            if (it is OpModeManagerNotifier.Notifications) it.onOpModePreStart(opMode)
        }

        try {
            opMode.internalThrowOpModeExceptionIfPresent()
        } catch (_: ForceStopException) {
        }

        opMode.internalStart()
    }

    fun stopOpMode() {
        try {
            opMode.internalThrowOpModeExceptionIfPresent()
        } catch (_: ForceStopException) {
        }

        opMode.internalStop()
        opMode.internalOpModeServices = null
//        try {
//            opMode.terminateOpModeNow()
//        } catch (e: Exception) {
//
//        }
        listeners.forEach {
            it.onOpModePostStop(opMode)
        }
        hMap.unsafeIterable().forEach {
            if (it is OpModeManagerNotifier.Notifications) it.onOpModePostStop(opMode)
        }
    }

    override fun refreshUserTelemetry(telemetry: TelemetryMessage?, sInterval: Double) {
        if (telemetry?.hasData() == true) {
            telemetry.clearData()
        }
    }

    override fun requestOpModeStop(opModeToStopIfActive: OpMode?) {
        if (opModeToStopIfActive == opMode) stopOpMode()
    }
}