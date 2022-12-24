package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.amarcolini.joos.dashboard.SuperTelemetry
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import java.util.function.BooleanSupplier
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * An OpMode that uses a [CommandScheduler], and optionally, a [Robot].
 */
abstract class CommandOpMode : LinearOpMode(), CommandInterface {
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
    protected val dashboard: FtcDashboard? = FtcDashboard.getInstance()

    /**
     * A handy [MultipleGamepad].
     */
    protected lateinit var gamepad: MultipleGamepad
        private set
        @JvmName("gamepad") get

    abstract fun preInit()
    open fun preStart() {}

    private var robot: Robot? = null
    private val isStartOverridden get() = this::class.memberFunctions.first { it.name == "preStart" } in this::class.declaredFunctions

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

    private var hasInitialized = false
    final override fun runOpMode() {
        gamepad = MultipleGamepad(gamepad1, gamepad2)
        preInit()
        hasInitialized = true

        if (isStartOverridden && initLoop) while (!isStarted) CommandScheduler.update()
        else telemetry.update()

        waitForStart()

        if (isStartOverridden && cancelBeforeStart) CommandScheduler.cancelAll()
        robot?.start()
        preStart()
        while (opModeIsActive()) CommandScheduler.update()
    }

    fun <T : Robot> registerRobot(robot: T): T {
        if (this.robot != null)
            throw IllegalArgumentException("Only one Robot is allowed to be registered with a CommandOpMode.")
        if (hasInitialized)
            throw Exception("registerRobot() can only be called in preInit().")
        this.robot = robot
        robot.init()
        return robot
    }
}