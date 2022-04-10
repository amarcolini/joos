package com.amarcolini.joos.command

import com.qualcomm.robotcore.eventloop.opmode.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * An OpMode made for [Robot]s.
 */
abstract class RobotOpMode<T : Robot> : OpMode() {
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
     * This method is called on initialization. Any commands scheduled here will be
     * run in the init loop. [initialize] **must** be called here.
     */
    abstract fun preInit()

    /**
     * This method is called on start. Any commands scheduled here will be run for the
     * duration of the OpMode.
     */
    abstract fun preStart()

    final override fun init() = preInit()
    final override fun init_loop() = robot.update()
    final override fun start() {
        robot.start()
        preStart()
    }

    final override fun loop() = robot.update()
    override fun stop() = robot.reset()

    /**
     * Initializes the given robot with this OpMode. This method **must** be called
     * in order for this OpMode to work correctly.
     */
    protected fun initialize(robot: T) {
        this.robot = robot
        robot.init()
    }

    /**
     * Initializes the given robot with this OpMode. This method **must** be called
     * in order for this OpMode to work correctly.
     */
    protected fun initialize(robot: (OpMode) -> T) {
        initialize(robot(this))
    }

    /**
     * Initializes the given robot with this OpMode. This method **must** be called
     * in order for this OpMode to work correctly.
     */
    protected inline fun <reified B : T> initialize() {
        initialize(B::class.primaryConstructor?.call(this) ?: return)
    }
}