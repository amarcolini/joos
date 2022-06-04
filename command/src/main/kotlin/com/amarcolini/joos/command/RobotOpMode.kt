package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.amarcolini.joos.dashboard.SuperTelemetry
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.*
import kotlin.reflect.full.hasAnnotation

/**
 * An OpMode made for [Robot]s.
 */
abstract class RobotOpMode<T : Robot> : OpMode(),
    CommandInterface {
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

    /**
     * The singleton [Robot] instance of this OpMode.
     */
    protected lateinit var robot: T
        private set

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

    /**
     * This method is called on initialization. Any commands scheduled here will be
     * run in the init loop. [initialize] should be called here.
     */
    abstract fun preInit()

    /**
     * This method is called on start. Any commands scheduled here will be run for the
     * duration of the OpMode.
     */
    abstract fun preStart()

    final override fun init() {
        preInit()
    }

    final override fun start() {
        CommandScheduler.cancelAll()
        if (::robot.isInitialized) robot.start()
        preStart()
    }

    override fun internalPostInitLoop() = CommandScheduler.update()

    override fun internalPostLoop() = CommandScheduler.update()

    override fun loop() {}

    /**
     * Initializes the given robot with this OpMode. This method should be called in init.
     */
    protected fun initialize(robot: T) {
        this.robot = robot
        robot.init()
    }
}