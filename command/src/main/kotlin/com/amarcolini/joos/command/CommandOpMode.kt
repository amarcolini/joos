package com.amarcolini.joos.command

import com.acmerobotics.dashboard.FtcDashboard
import com.amarcolini.joos.dashboard.SuperTelemetry
import com.amarcolini.joos.gamepad.GamepadEx
import com.amarcolini.joos.gamepad.MultipleGamepad
import com.amarcolini.joos.gamepad.Toggleable
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import java.util.function.BiFunction
import java.util.function.BooleanSupplier
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

/**
 * An OpMode that uses a [CommandScheduler], and optionally, a [Robot].
 */
abstract class CommandOpMode : LinearOpMode(), CommandInterface {
    private val initializerDelegates = ArrayList<InitializerDelegate<*>>()

    @JvmField
    protected var hMap = hardwareMap

    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
    annotation class Hardware(val id: String)

    init {
        this::class.declaredMemberProperties.forEach {
            if (it !is KMutableProperty1<out CommandOpMode, *>) return@forEach
            if (it.isAbstract) return@forEach
            val id = it.annotations.filterIsInstance<Hardware>().firstOrNull()?.id ?: return@forEach
            it.isAccessible = true
            initializerDelegates += HardwareDelegate(it.returnType.jvmErasure, id) {
                it.setter.call(this@CommandOpMode, this)
            }
        }
    }

    open inner class InitializerDelegate<T> constructor(
        private val onInit: () -> T,
    ) {
        private var output: T? = null

        init {
            initializerDelegates += this
        }

        fun init() {
            output = onInit()
        }

        fun reset() {
            output = null
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return output ?: fallback()
        }

        protected open fun fallback(): T = throw IllegalStateException("Initializer delegate was accessed too early!")
    }

    private fun <Device : Any, T> replaceHardwareDelegate(
        delegate: HardwareDelegate<*, Device>,
        map: Device.() -> T,
    ): HardwareDelegate<T, Device> {
        initializerDelegates -= delegate
        return HardwareDelegate(delegate.deviceType, delegate.deviceName, map)
    }

    open inner class HardwareDelegate<T, Device : Any> constructor(
        val deviceType: KClass<Device>,
        val deviceName: String,
        private val map: Device.() -> T,
    ) : InitializerDelegate<T>({
        map(hardwareMap.get(deviceType.java, deviceName))
    }) {
        override fun fallback(): T = throw IllegalStateException("Device $deviceName was accessed too early!")

        fun init(init: Device.() -> Unit) = replaceHardwareDelegate(this) {
            map(this).also { init(this) }
        }

        fun <R> map(newMap: Device.() -> R) = replaceHardwareDelegate(this, newMap)
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
    protected val telem: SuperTelemetry = CommandScheduler.telem

    /**
     * The FtcDashboard instance.
     */
    @JvmField
    protected var dashboard: FtcDashboard? = FtcDashboard.getInstance()

    /**
     * A handy [MultipleGamepad].
     */
    protected val gamepad: MultipleGamepad @JvmName("gamepad") get() = CommandScheduler.gamepad!!

    fun <T> gamepad(buttons: BiFunction<GamepadEx, GamepadEx, T>): T = gamepad.get(buttons)

    protected inline fun <reified Device : Any> getHardware(deviceName: String) =
        HardwareDelegate(Device::class, deviceName) { this }

    protected fun <T : Any> onInit(init: () -> T) = InitializerDelegate(init)

    abstract fun preInit()
    open fun preStart() {}
    open fun postStop() {}

    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
    annotation class Register

    private var robot: Robot? = null
    private var initRobot: (() -> Unit)? = null
    private var resetRobot: (() -> Unit)? = null

    inner class RobotDelegate<T : Robot> constructor(private val type: KClass<T>) {
        private var bot: T? = null

        init {
            if (initRobot != null || resetRobot != null) throw IllegalStateException("Only one robot is allowed per CommandOpMode!")
            initRobot = {
                bot = try {
                    type.createInstance()
                } catch (e: Exception) {/*
                    The exception thrown by createInstance() appears to run on a different
                    thread and doesn't get caught by the robot controller, causing a silent crash.
                    It's also a reflection exception, which isn't very useful for debugging, so we throw
                    the exception that caused it instead.
                     */
                    e.cause?.let { cause -> throw cause } ?: throw e
                }
                robot = bot
            }
            resetRobot = {
                bot = null
            }
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return bot ?: throw IllegalStateException("Robot was accessed too early!")
        }
    }

    protected inline fun <reified T : Robot> robot() = RobotDelegate(T::class)

    init {
        this::class.declaredMemberProperties.forEach {
            if (it !is KMutableProperty1<out CommandOpMode, *>) return@forEach
            if (it.isAbstract) return@forEach
            if (it.annotations.filterIsInstance<Register>().isEmpty()) return@forEach
            if (!it.returnType.jvmErasure.isSubclassOf(Robot::class)) throw IllegalStateException("The Register annotation is reserved for Robots only.")
            if (initRobot != null || resetRobot != null) throw IllegalStateException("Only one robot is allowed per CommandOpMode!")
            it.isAccessible = true
            initRobot = {
                val bot = try {
                    it.returnType.jvmErasure.createInstance() as Robot
                } catch (e: Exception) {/*
                The exception thrown by createInstance() appears to run on a different
                thread and doesn't get caught by the robot controller, causing a silent crash.
                It's also a reflection exception, which isn't very useful for debugging, so we throw
                the exception that caused it instead.
                */
                    e.cause?.let { cause -> throw cause } ?: throw e
                }
                robot = bot
                it.setter.call(this, bot)
            }
            resetRobot = {
                robot = null
                it.setter.call(this, null)
            }
        }
    }

    private val isStartOverridden get() = this::class.memberFunctions.first { it.name == "preStart" } in this::class.declaredFunctions

    /**
     * Whether the [CommandScheduler] should update in the init loop. Note that if [preStart] is overridden,
     * this is automatically set to true, otherwise false.
     */
    protected var initLoop: Boolean = true

    /**
     * Whether all commands scheduled in [preInit] should be cancelled before starting the OpMode. `false` by default.
     */
    protected var cancelBeforeStart: Boolean = false

    private var hasInitialized = false
    final override fun runOpMode() {
        CommandScheduler.reset()
        hMap = hardwareMap

        robot = null
        initRobot?.invoke()
        CommandScheduler.isBusy = true
        robot?.init()
        cancelBeforeStart = false
        initLoop = isStartOverridden
        dashboard = FtcDashboard.getInstance()
        initializerDelegates.forEach { it.init() }
        preInit()
        CommandScheduler.isBusy = false
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
        CommandScheduler.cancelAll()
        postStop()
        robot?.stop()
        robot = null
        resetRobot?.invoke()
        initializerDelegates.forEach { it.reset() }
        CommandScheduler.reset()
    }
}