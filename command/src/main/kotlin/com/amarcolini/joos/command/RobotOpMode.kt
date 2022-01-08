package com.amarcolini.joos.command

import com.qualcomm.robotcore.eventloop.opmode.*
import kotlin.reflect.full.hasAnnotation

/**
 * An OpMode made for [Robot]s.
 */
abstract class RobotOpMode : OpMode() {
    /**
     * Whether this OpMode is a teleop OpMode.
     */
    @JvmField
    protected val isTeleOp = this::class.hasAnnotation<TeleOp>()

    /**
     * Whether this OpMode is an autonomous OpMode.
     */
    @JvmField
    protected val isAutonomous = this::class.hasAnnotation<Autonomous>()

    private lateinit var robot: Robot
    final override fun init_loop() = robot.update()
    final override fun start() = robot.start()
    final override fun loop() = robot.update()
    override fun stop() = robot.reset()

    /**
     * Initializes the given robot with this OpMode. This method **must** be called
     * in order for this OpMode to work correctly.
     */
    protected fun initialize(robot: Robot) {
        this.robot = robot
        robot.init()
    }
}