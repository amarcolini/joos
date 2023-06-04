package com.amarcolini.joos.hardware

import com.amarcolini.joos.command.CommandScheduler
import com.qualcomm.robotcore.eventloop.opmode.OpMode

val OpMode.hMap get() = hardwareMap

val OpMode.telem get() = CommandScheduler.telemetry