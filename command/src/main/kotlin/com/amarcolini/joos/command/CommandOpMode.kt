package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.amarcolini.joos.dashboard.SuperTelemetry
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.starProjectedType

/**
 * An OpMode that uses a [CommandScheduler], and optionally, a [Robot].
 */
abstract class CommandOpMode : OpMode(), CommandInterface {
    /**
     * Whether this OpMode is a teleop OpMode.
     */
    @JvmField
    protected val isTeleOp: Boolean = this::class.hasAnnotation<TeleOp>()

    /**
     * Whether this OpMode is an autonomous OpMode.
     */
    @JvmField
    protected val isAutonomous: Boolean = this::class.hasAnnotation<Autonomous>()

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

    override fun internalPostInitLoop() {
        if (isStartOverridden && initLoop) CommandScheduler.update()
    }

    override fun internalPostLoop(): Unit = CommandScheduler.update()

    abstract fun preInit()
    open fun preStart() {}

    private var robot: Robot? = null
    private val isStartOverridden get() = this::preStart.instanceParameter?.type != this::class.starProjectedType

    /**
     * Whether the [CommandScheduler] should update in the init loop. Note that if [preStart] is not overridden,
     * [CommandScheduler] will not update in the init loop regardless.
     */
    protected var initLoop: Boolean = true

    /**
     * Whether all commands scheduled in [preInit] should be cancelled before starting the OpMode. Note that if
     * [preStart] is not overridden, [CommandScheduler] will not cancel them regardless.
     */
    protected var cancelBeforeStart: Boolean = true

    final override fun init() {
        preInit()
    }

    final override fun start() {
        if (isStartOverridden && cancelBeforeStart) cancelAll()
        robot?.start()
        preStart()
    }

    fun <T : Robot> registerRobot(robot: T): T {
        if (this.robot != null)
            throw IllegalArgumentException("Only one Robot is allowed to be registered with a CommandOpMode.")
        this.robot = robot
        robot.init()
        return robot
    }

    override fun loop() {}
}