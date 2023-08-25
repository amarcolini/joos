package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.amarcolini.joos.dashboard.SuperTelemetry
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * An OpMode that uses a [CommandScheduler], and optionally, a [Robot].
 */
abstract class CommandOpMode : LinearOpMode(), CommandInterface {
    private val hardwareDelegates = ArrayList<HardwareDelegate<*>>()

    class HardwareDelegate<T : Any> constructor(
        private val type: KClass<T>,
        private val deviceName: String
    ) {
        private var device: T? = null

        fun init(hMap: HardwareMap) {
            device = hMap.get(type.java, deviceName)
        }

        fun reset() {
            device = null
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return device ?: throw IllegalStateException("Device $deviceName was accessed too early!")
        }
    }

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
    protected var dashboard: FtcDashboard? = FtcDashboard.getInstance()

    /**
     * A handy [MultipleGamepad].
     */
    protected val gamepad: MultipleGamepad @JvmName("gamepad") get() = CommandScheduler.gamepad!!

    protected inline fun <reified T : Any> hMap(deviceName: String) =
        HardwareDelegate(T::class, deviceName)

    abstract fun preInit()
    open fun preStart() {}
    open fun postStop() {}

    private var robot: Robot? = null
    private val isStartOverridden get() = this::class.memberFunctions.first { it.name == "preStart" } in this::class.declaredFunctions

    /**
     * Whether the [CommandScheduler] should update in the init loop. Note that if [preStart] is overridden,
     * this is automatically set to true, otherwise false.
     */
    protected var initLoop: Boolean = true

    /**
     * Whether all commands scheduled in [preInit] should be cancelled before starting the OpMode. `false` by default.
     */
    protected var cancelBeforeStart: Boolean = true

    private var hasInitialized = false
    final override fun runOpMode() {
        robot = null
        cancelBeforeStart = false
        initLoop = isStartOverridden
        dashboard = FtcDashboard.getInstance()
        hardwareDelegates.forEach { it.init(hardwareMap) }
        preInit()
        hasInitialized = true

        val finalInitLoop = initLoop
        if (finalInitLoop) while (opModeInInit()) CommandScheduler.update()
        else {
            telemetry.update()
            waitForStart()
        }

        if (isStopRequested) return

        if (cancelBeforeStart) CommandScheduler.cancelAll()
        robot?.start()
        preStart()
        while (opModeIsActive()) CommandScheduler.update()
        postStop()
        robot?.stop()
        robot = null
        hardwareDelegates.forEach { it.reset() }
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