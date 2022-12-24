package com.amarcolini.joos.hardware

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.FeedforwardCoefficients
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.control.PIDFController
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.rad
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign


/**
 * A wrapper for the [DcMotorEx] object in the FTC SDK.
 *
 * @param motor the motor for the wrapper to use
 * @param maxRPM the revolutions per minute of the motor
 * @param TPR the ticks per revolution of the motor
 */
class Motor @JvmOverloads constructor(
    private val motor: DcMotorEx,
    @JvmField
    val maxRPM: Double,
    @JvmField
    val TPR: Double = 1.0,
    clock: NanoClock = NanoClock.system()
) : Component {
    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param maxRPM the maximum revolutions per minute of the motor
     * @param TPR the ticks per revolution of the motor
     */
    @JvmOverloads
    constructor(
        hMap: HardwareMap,
        id: String,
        maxRPM: Double,
        TPR: Double = 1.0,
        clock: NanoClock = NanoClock.system()
    ) : this(
        hMap.get(DcMotorEx::class.java, id),
        maxRPM,
        TPR,
        clock
    )

    /**
     * @param motor the motor for the wrapper to use
     * @param maxRPM the revolutions per minute of the motor
     * @param TPR the ticks per revolution of the motor
     * @param wheelRadius the radius of the wheel the motor is turning
     * @param gearRatio the gear ratio from the output shaft to the wheel the motor is turning
     */
    @JvmOverloads
    constructor(
        motor: DcMotorEx,
        maxRPM: Double,
        TPR: Double,
        wheelRadius: Double,
        gearRatio: Double,
        clock: NanoClock = NanoClock.system()
    ) : this(motor, maxRPM, TPR, clock) {
        distancePerOutputRev = 2 * PI * wheelRadius
        this.gearRatio = gearRatio
    }

    /**
     * @param motor the motor for the wrapper to use
     * @param kind the kind of motor
     * @param wheelRadius the radius of the wheel the motor is turning
     * @param gearRatio the gear ratio from the output shaft to the wheel the motor is turning
     */
    @JvmOverloads
    constructor(
        motor: DcMotorEx,
        kind: Kind,
        wheelRadius: Double = 1.0,
        gearRatio: Double = 1.0,
        clock: NanoClock = NanoClock.system()
    ) : this(motor, kind.maxRPM, kind.TPR, wheelRadius, gearRatio, clock)

    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param kind the kind of motor
     * @param wheelRadius the radius of the wheel the motor is turning
     * @param gearRatio the gear ratio from the output shaft to the wheel the motor is turning
     */
    @JvmOverloads
    constructor(
        hMap: HardwareMap,
        id: String,
        kind: Kind,
        wheelRadius: Double = 1.0,
        gearRatio: Double = 1.0,
        clock: NanoClock = NanoClock.system()
    ) : this(hMap, id, kind.maxRPM, kind.TPR, wheelRadius, gearRatio, clock)

    /**
     * @param motor the motor for the wrapper to use
     * @param maxRPM the revolutions per minute of the motor
     * @param TPR the ticks per revolution of the motor
     * @param gearRatio the gear ratio from the output shaft to the wheel the motor is turning
     */
    @JvmOverloads
    constructor(
        motor: DcMotorEx,
        maxRPM: Double,
        TPR: Double,
        gearRatio: Double,
        clock: NanoClock = NanoClock.system()
    ) : this(motor, maxRPM, TPR, clock) {
        this.gearRatio = gearRatio
    }

    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param maxRPM the revolutions per minute of the motor
     * @param TPR the ticks per revolution of the motor
     * @param wheelRadius the radius of the wheel the motor is turning
     * @param gearRatio the gear ratio from the output shaft to the wheel the motor is turning
     */
    @JvmOverloads
    constructor(
        hMap: HardwareMap,
        id: String,
        maxRPM: Double,
        TPR: Double,
        wheelRadius: Double,
        gearRatio: Double,
        clock: NanoClock = NanoClock.system()
    ) : this(hMap.get(DcMotorEx::class.java, id), maxRPM, TPR, wheelRadius, gearRatio, clock)

    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param maxRPM the revolutions per minute of the motor
     * @param TPR the ticks per revolution of the motor
     * @param gearRatio the gear ratio from the output shaft to the wheel the motor is turning
     */
    @JvmOverloads
    constructor(
        hMap: HardwareMap,
        id: String,
        maxRPM: Double,
        TPR: Double,
        gearRatio: Double,
        clock: NanoClock = NanoClock.system()
    ) : this(hMap.get(DcMotorEx::class.java, id), maxRPM, TPR, gearRatio, clock)

    enum class RunMode {
        /**
         * Sets the raw voltage given to the motor, using feedforward if desired.
         *
         * To use feedforward, simply set [feedforwardCoefficients].
         */
        RUN_WITHOUT_ENCODER,

        /**
         * Controls the velocity of the motor using feedforward and encoders.
         *
         * *Note*: PID gains have not been finely tuned, and may need to be set to ensure
         * consistent motor movement.
         *
         * @see veloCoefficients
         * @see feedforwardCoefficients
         */
        RUN_USING_ENCODER,

        /**
         * Uses an encoder to move the motor to the desired setpoint. The speed of
         * the motor is still affected by calls to [Motor.power] and [Motor.setSpeed].
         *
         * *Note*: PID gains have not been finely tuned, and may need to be set to ensure
         * consistent motor movement. Position tolerance may need to be tuned as well.
         *
         * @see targetPosition
         * @see positionTolerance
         * @see goToPosition
         */
        RUN_TO_POSITION
    }

    enum class ZeroPowerBehavior {
        BRAKE, FLOAT
    }

    /**
     * A class representing the different ways to measure motor speed.
     */
    enum class RotationUnit {
        /**
         * Revolutions per minute.
         */
        RPM,

        /**
         * Encoder ticks per second.
         */
        TPS,

        /**
         * Distance units travelled per second.
         */
        UPS,

        /**
         * Degrees per second.
         */
        DPS,

        /**
         * Radians per second.
         */
        RPS
    }

    /**
     * A class with the specs of many motors so you don't have to find them.
     */
    enum class Kind(val maxRPM: Double, val TPR: Double) {
        GOBILDA_30(30.0, 5_281.1),
        GOBILDA_43(43.0, 3_895.9),
        GOBILDA_60(60.0, 2_786.2),
        GOBILDA_84(84.0, 1_992.6),
        GOBILDA_117(117.0, 1_425.1),
        GOBILDA_223(223.0, 751.8),
        GOBILDA_312(312.0, 537.7),
        GOBILDA_435(435.0, 384.5),
        GOBILDA_1150(1150.0, 145.1),
        GOBILDA_1620(1620.0, 103.8),
        GOBILDA_6000(5400.0, 28.0),
        GOBILDA_MATRIX(5800.0, 28.0),
        REV_HEX(6000.0, 28.0),
        REV_CORE_HEX(125.0, 288.0),
        REV_20_SPUR(300.0, 560.0),
        REV_40_SPUR(150.0, 1120.0),
        REV_20_PLANETARY(312.5, 537.6),
        NEVEREST_20(349.0, 537.6),
        NEVEREST_40(160.0, 1120.0),
        NEVEREST_60(105.0, 1680.0),
        NEVEREST_3_7(1780.0, 103.6),
        TETRIX_60(100.0, 1440.0),
        TETRIX_40(150.0, 960.0),
        TETRIX_20(480.0, 480.0)
    }

    /**
     * A wrapper for motor encoders in the FTC SDK.
     *
     * @param TPR the ticks per revolution of the encoder
     * @param getPosition the position supplier which points to the
     * current position of the motor in ticks
     * @param getVelocity the position supplier which points to the
     * current velocity of the motor in ticks per second
     */
    class Encoder internal constructor(
        private val TPR: Double,
        private val getPosition: () -> Int,
        private val getVelocity: () -> Double,
        private val clock: NanoClock = NanoClock.system()
    ) {
        /**
         * Constructs an encoder from the provided [id] of the corresponding motor.
         *
         * @param hMap the hardware map from the OpMode
         * @param id the device id from the RC config
         * @param TPR the ticks per revolution of the encoder
         */
        constructor(hMap: HardwareMap, id: String, TPR: Double) : this(
            TPR,
            hMap.get(DcMotorEx::class.java, id)::getCurrentPosition,
            hMap.get(DcMotorEx::class.java, id)::getVelocity,
        )

        /**
         * Constructs an encoder from the provided [id] of the corresponding motor.
         *
         * @param hMap the hardware map from the OpMode
         * @param id the device id from the RC config
         * @param TPR the ticks per revolution of the encoder
         * @param wheelRadius the radius of the wheel attached to the encoder
         * @param gearRatio the gear ratio of the encoder
         */
        @JvmOverloads
        constructor(hMap: HardwareMap, id: String, TPR: Double, wheelRadius: Double, gearRatio: Double = 1.0) : this(
            hMap, id, TPR
        ) {
            distancePerRev = gearRatio * 2 * PI * wheelRadius
        }

        companion object {
            /**
             * Utility method for constructing multiple encoders with the same specifications.
             */
            @JvmStatic
            fun multiple(hMap: HardwareMap, TPR: Double, vararg ids: String): List<Encoder> = ids.map {
                Encoder(hMap, it, TPR)
            }

            /**
             * Utility method for constructing multiple encoders with the same specifications.
             * @param ids the ids of the encoders and whether they are reversed
             */
            @JvmStatic
            @SafeVarargs
            fun multiple(hMap: HardwareMap, TPR: Double, vararg ids: Pair<String, Boolean>): List<Encoder> = ids.map {
                Encoder(hMap, it.first, TPR).apply { reversed = it.second }
            }

            /**
             * Utility method for constructing multiple encoders with the same specifications.
             */
            @JvmStatic
            @JvmOverloads
            fun multiple(
                hMap: HardwareMap,
                TPR: Double,
                wheelRadius: Double,
                gearRatio: Double = 1.0,
                vararg ids: String
            ): List<Encoder> = ids.map {
                Encoder(hMap, it, TPR, wheelRadius, gearRatio)
            }

            /**
             * Utility method for constructing multiple encoders with the same specifications.
             * @param ids the ids of the encoders and whether they are reversed
             */
            @JvmStatic
            @JvmOverloads
            @SafeVarargs
            fun multiple(
                hMap: HardwareMap,
                TPR: Double,
                wheelRadius: Double,
                gearRatio: Double = 1.0,
                vararg ids: Pair<String, Boolean>
            ): List<Encoder> = ids.map {
                Encoder(hMap, it.first, TPR, wheelRadius, gearRatio).apply { reversed = it.second }
            }
        }

        private var resetVal = 0
        private var lastPosition = 0

        /**
         * Whether the encoder is reversed. Independent of motor direction.
         */
        @JvmField
        var reversed: Boolean = false

        /**
         * Reverses the encoder.
         */
        fun reversed(): Encoder = this.apply { reversed = !reversed }

        private var lastTimeStamp: Double = clock.seconds()
        private var veloEstimate = 0.0

        /**
         * The distance per revolution of the encoder.
         */
        @JvmField
        var distancePerRev = 1.0

        /**
         * The current position of the encoder in ticks.
         */
        val position: Int
            get() {
                val currentPosition = if (reversed) -1 else 1 * (getPosition() - resetVal)
                if (currentPosition != lastPosition) {
                    val currentTime = clock.seconds()
                    val dt = currentTime - lastTimeStamp
                    veloEstimate = (currentPosition - lastPosition) / dt
                    lastPosition = currentPosition
                    lastTimeStamp = currentTime
                }
                return currentPosition
            }

        /**
         * The distance traveled by the encoder computed using [distancePerRev].
         */
        val distance: Double
            get() = distancePerRev * (position / TPR)

        /**
         * The velocity of the encoder in units per second computed using [distancePerRev].
         */
        val distanceVelocity: Double
            get() = distancePerRev * (velocity / TPR)

        /**
         * Resets the encoder without having to stop the corresponding motor.
         */
        fun reset() {
            resetVal = getPosition()
        }

        /**
         * The corrected velocity of the encoder in ticks per second, accounting for overflow.
         */
        val velocity: Double
            get() {
                var real = getVelocity() * if (reversed) -1 else 1
                while (abs(veloEstimate - real) > 0x10000 / 2.0) {
                    real += sign(veloEstimate - real) * 0x10000
                }
                return real
            }
    }

    @JvmField
    val encoder: Encoder = Encoder(TPR, motor::getCurrentPosition, motor::getVelocity, clock)

    /**
     * The maximum achievable distance velocity of the motor.
     */
    val maxDistanceVelocity: Double get() = rpmToDistanceVelocity(maxRPM)

    var runMode: RunMode = RunMode.RUN_WITHOUT_ENCODER
    var zeroPowerBehavior: ZeroPowerBehavior = ZeroPowerBehavior.FLOAT
        set(value) {
            motor.zeroPowerBehavior = when (value) {
                ZeroPowerBehavior.BRAKE -> DcMotor.ZeroPowerBehavior.BRAKE
                ZeroPowerBehavior.FLOAT -> DcMotor.ZeroPowerBehavior.FLOAT
            }
            field = value
        }
        get() = when (motor.zeroPowerBehavior) {
            DcMotor.ZeroPowerBehavior.BRAKE -> ZeroPowerBehavior.BRAKE
            else -> ZeroPowerBehavior.FLOAT
        }

    /**
     * PID coefficients used in [RunMode.RUN_USING_ENCODER].
     */
    var veloCoefficients: PIDCoefficients = PIDCoefficients(1.0)

    /**
     * PID coefficients used in [RunMode.RUN_TO_POSITION].
     */
    var positionCoefficients: PIDCoefficients = PIDCoefficients(1.0)

    /**
     * Feedforward coefficients used in both [RunMode.RUN_USING_ENCODER] and [RunMode.RUN_WITHOUT_ENCODER].
     * Note that these coefficients are applied to desired distance velocity, so not using feedforward means setting
     * kV to 1 / [maxDistanceVelocity].
     */
    var feedforwardCoefficients: FeedforwardCoefficients = FeedforwardCoefficients(1 / maxDistanceVelocity)

    /**
     * The target position used by [RunMode.RUN_TO_POSITION].
     */
    var targetPosition: Int = 0
        set(value) {
            positionController.targetPosition = value.toDouble()
            field = value
        }

    /**
     * Sets [targetPosition] with the target [angle].
     */
    fun setTargetAngle(angle: Angle) {
        targetPosition = (angle / (2 * PI).rad * TPR).roundToInt()
    }

    /**
     * Sets [targetPosition] with the target [angle], where [angle] is in [Angle.defaultUnits].
     */
    fun setTargetAngle(angle: Double) = setTargetAngle(Angle(angle))

    /**
     * Sets [targetPosition] with the target [distance].
     */
    fun setTargetDistance(distance: Double) {
        targetPosition = (distance / distancePerRev * TPR).roundToInt()
    }

    private var targetVel: Double = 0.0
    private var targetAccel: Double = 0.0

    /**
     * Whether the motor is reversed.
     */
    var reversed: Boolean = false
        /**
         * Sets whether the direction of the motor is reversed.
         */
        @JvmName("setReversed")
        set(value) {
            motor.direction =
                if (value) DcMotorSimple.Direction.REVERSE
                else DcMotorSimple.Direction.FORWARD
            encoder.reversed = value
            field = value
        }
        @JvmName("isReversed")
        get() = motor.direction == DcMotorSimple.Direction.REVERSE

    /**
     * The position error considered tolerable for [RunMode.RUN_TO_POSITION] to be considered at the set point.
     */
    var positionTolerance: Int = 10
        set(value) {
            positionController.tolerance = value.toDouble()
            field = value
        }


    private val veloController = PIDFController(veloCoefficients, clock = clock)
    private val positionController = PIDFController(positionCoefficients, clock = clock)

    init {
        veloController.setOutputBounds(-maxRPM, maxRPM)
        positionController.setOutputBounds(-1.0, 1.0)
    }

    private var speed: Double = 0.0

    /**
     * The maximum achievable ticks per second velocity of the motor.
     */
    @JvmField
    val maxTPS: Double = TPR * (maxRPM / 60)

    /**
     * Reverses the direction of the motor.
     */
    fun reversed(): Motor {
        reversed = !reversed
        return this
    }

    /**
     * Sets the speed of the motor.
     *
     * @param velocity the velocity to set
     * @param acceleration the acceleration to set
     * @param unit the units [velocity] and [acceleration] are expressed in (revolutions per minute by default).
     */
    @JvmOverloads
    fun setSpeed(
        velocity: Double,
        acceleration: Double = 0.0,
        unit: RotationUnit = RotationUnit.RPM
    ) {
        val multiplier = when (unit) {
            RotationUnit.RPM -> 1.0
            RotationUnit.TPS -> 60 / TPR
            RotationUnit.DPS -> maxRPM * 6
            RotationUnit.RPS -> (maxRPM / 60) * 2 * Math.PI
            RotationUnit.UPS -> 60 / distancePerRev
        }
        val vel = velocity * multiplier
        val accel = acceleration * multiplier
        speed = (vel / maxRPM).coerceIn(-1.0, 1.0)
        targetVel = rpmToDistanceVelocity(vel)
        targetAccel = rpmToDistanceVelocity(accel)
        when (runMode) {
            RunMode.RUN_USING_ENCODER -> {
                veloController.pid = veloCoefficients
                veloController.setTarget(vel)
                motor.power = feedforwardCoefficients.calculate(
                    targetVel,
                    targetAccel,
                    veloController.update(this.velocity) / maxRPM
                )
            }
            RunMode.RUN_TO_POSITION -> {
                positionController.pid = positionCoefficients
                motor.power =
                    speed * positionController.update(encoder.position.toDouble(), encoder.velocity)
            }
            RunMode.RUN_WITHOUT_ENCODER -> {
                motor.power = feedforwardCoefficients.calculate(
                    targetVel,
                    targetAccel
                )
            }
        }
    }

    /**
     * The percentage of power/velocity of the motor in the range `[-1.0, 1.0]`.
     */
    var power: Double = 0.0
        set(value) {
            setSpeed(value * maxRPM)
            field = value
        }
        get() = speed

    /**
     * Converts from revolutions per minute to units travelled per second.
     */
    fun rpmToDistanceVelocity(rpm: Double) = rpm / 60 * distancePerRev

    /**
     * Converts from units travelled per second to revolutions per minute.
     */
    fun distanceVelocityToRPM(distanceVelocity: Double) = distanceVelocity / distancePerRev * 60

    /**
     * Converts from encoder ticks per second to units travelled per second.
     */
    fun ticksToDistanceVelocity(ticks: Int) = ticks / TPR * distancePerRev

    /**
     * Converts from units travelled per second to encoder ticks per second.
     */
    fun distanceVelocityToTicks(distanceVelocity: Double) = distanceVelocity / distancePerRev * TPR

    /**
     * Updates both [RunMode.RUN_USING_ENCODER] and [RunMode.RUN_TO_POSITION]. Running this method is
     * not necessary for [RunMode.RUN_WITHOUT_ENCODER].
     */
    override fun update() {
        when (runMode) {
            RunMode.RUN_USING_ENCODER -> {
                motor.power = feedforwardCoefficients.calculate(
                    targetVel,
                    targetAccel,
                    veloController.update(this.velocity) / maxRPM
                )
            }
            RunMode.RUN_TO_POSITION -> motor.power =
                speed * positionController.update(encoder.position.toDouble(), encoder.velocity)
            else -> return
        }
    }

    /**
     * Returns a command that runs the motor until it has reached the desired position.
     */
    fun goToPosition(position: Int): Command = Command.of {
        runMode = RunMode.RUN_TO_POSITION
        targetPosition = position
    }
        .onInit {
            runMode = RunMode.RUN_TO_POSITION
            targetPosition = position
        }
        .requires(this)
        .runUntil { !isBusy() }
        .onEnd { setSpeed(0.0) }

    /**
     * Returns a command that runs the motor until it has reached the desired [distance].
     */
    fun goToDistance(distance: Double): Command =
        goToPosition((distance / distancePerRev * TPR).roundToInt())

    /**
     * Returns a command that runs the motor until it has reached the desired [angle].
     */
    fun goToAngle(angle: Angle): Command =
        goToPosition((angle / (2 * PI).rad * gearRatio).roundToInt())

    /**
     * Returns a command that runs the motor until it has reached the desired [angle],
     * where [angle] is in [Angle.defaultUnits].
     */
    fun goToAngle(angle: Double): Command = goToAngle(Angle(angle))

    /**
     * The distance per revolution travelled by the motor.
     * @see distance
     * @see distanceVelocity
     */
    var distancePerRev: Double = encoder.distancePerRev
        get() = encoder.distancePerRev
        private set(value) {
            encoder.distancePerRev = value
            field = value
        }

    /**
     * The distance per revolution of the motor output, excluding any gear ratios (i.e.,
     * if the motor had a gear ratio of 2:1, and a wheel was attached to the output, this value would
     * be the circumference of the wheel).
     */
    var distancePerOutputRev: Double = 1.0
        set(value) {
            distancePerRev = value * gearRatio
            field = value
        }

    /**
     * The gear ratio on the motor.
     */
    var gearRatio: Double = 1.0
        set(value) {
            distancePerRev = value * distancePerOutputRev
            field = value
        }

    /**
     * Returns whether the motor is currently moving towards the desired setpoint using [RunMode.RUN_TO_POSITION].
     */
    fun isBusy() = runMode == RunMode.RUN_TO_POSITION && !positionController.isAtSetPoint()

    /**
     * The velocity of the motor in revolutions per minute computed using only the motor power and [maxRPM]. Note that this
     * does **not** use the encoder.
     * @see velocity
     */
    val rawVelocity
        get() = motor.power * maxRPM

    /**
     * The velocity of the motor, in revolutions per minute. Computed using the encoder.
     */
    val velocity
        get() = encoder.velocity * 60 / TPR

    /**
     * The total rotation of the motor. Note that this value is positive or negative according to motor direction.
     */
    val rotation: Angle get() = (encoder.position / TPR * 2 * PI).rad

    /**
     * The velocity of the motor in the specified [units]. Computed using the encoder.
     */
    fun getVelocity(units: RotationUnit): Double = when (units) {
        RotationUnit.RPM -> velocity
        RotationUnit.TPS -> encoder.velocity
        RotationUnit.UPS -> distanceVelocity
        RotationUnit.DPS -> encoder.velocity / TPR * 360
        RotationUnit.RPS -> encoder.velocity / TPR * 2 * PI
    }

    /**
     * The target velocity of [RunMode.RUN_USING_ENCODER] in revolutions per minute.
     */
    val targetVelocity: Double get() = veloController.targetPosition

    /**
     * The target velocity of [RunMode.RUN_USING_ENCODER] in [units].
     */
    fun getTargetVelocity(units: RotationUnit): Double = when (units) {
        RotationUnit.RPM -> targetVelocity
        RotationUnit.TPS -> targetVelocity * TPR / 60
        RotationUnit.UPS -> targetVelocity * distancePerRev / 60
        RotationUnit.DPS -> targetVelocity * 6
        RotationUnit.RPS -> targetVelocity * PI / 30
    }


    /**
     * The current position of the encoder.
     */
    val currentPosition
        get() = encoder.position

    /**
     * Resets the encoder.
     */
    fun resetEncoder() = encoder.reset()

    /**
     * The distance travelled by the motor. Computed using the encoder and [distancePerRev].
     * @see distanceVelocity
     * @see distancePerRev
     */
    val distance
        get() = encoder.distance

    /**
     * The velocity of the motor in units per second. Computed using the encoder and [distancePerRev].
     * @see distance
     * @see distancePerRev
     */
    val distanceVelocity
        get() = encoder.distanceVelocity
}