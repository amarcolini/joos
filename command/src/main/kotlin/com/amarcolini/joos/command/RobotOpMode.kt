package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.amarcolini.joos.dashboard.SuperTelemetry
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.*
import kotlin.reflect.full.hasAnnotation

/**
 * An OpMode made for [Robot]s.
 */
abstract class RobotOpMode<T : Robot>(private val constructor: () -> T) : OpMode(),
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
    protected val gamepad: MultipleGamepad by lazy {
        CommandScheduler.gamepad
            ?: throw RuntimeException("Gamepads cannot be null when calling gamepad(). Make sure that the OpMode has been initialized.")
    }
        @JvmName("gamepad") get

    /**
     * This method is called on initialization. Any commands scheduled here will be
     * run in the init loop.
     */
    abstract fun preInit(robot: T)

    private var initLoop: Boolean = true

    /**
     * Sets whether the CommandScheduler should run in the init loop.
     */
    fun setInitLoop(value: Boolean) {
        initLoop = value
    }

    /**
     * This method is called on start. Any commands scheduled here will be run for the
     * duration of the OpMode.
     */
    abstract fun preStart(robot: T)

    final override fun init() {
        robot = constructor()
        robot.init()
        preInit(robot)
        telem.update()
    }

    final override fun start() {
        CommandScheduler.cancelAll()
        robot.start()
        preStart(robot)
    }

    override fun internalPostInitLoop() {
        if (initLoop) CommandScheduler.update()
    }

    override fun internalPostLoop() = CommandScheduler.update()

    override fun loop() {}
}