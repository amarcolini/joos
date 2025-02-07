package com.amarcolini.joos.hardware

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.control.*
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.rad
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign


/**
 * A wrapper for the [DcMotorEx] object in the FTC SDK.
 *
 * @param motor The motor for the wrapper to use.
 * @param maxRPM The maximum revolutions per minute of the motor.
 * @param TPR The ticks per revolution of the motor.
 */
class Motor @JvmOverloads constructor(
    motor: DcMotorEx,
    @JvmField
    val maxRPM: Double,
    @JvmField
    val TPR: Double = 1.0,
    clock: NanoClock = NanoClock.system
) : Component {
    @JvmField
    val internal = motor

    /**
     * @param hMap The hardware map from the OpMode.
     * @param id The device id from the RC config.
     * @param maxRPM The maximum revolutions per minute of the motor.
     * @param TPR The ticks per revolution of the motor.
     */
    @JvmOverloads
    constructor(
        hMap: HardwareMap,
        id: String,
        maxRPM: Double,
        TPR: Double = 1.0,
        clock: NanoClock = NanoClock.system
    ) : this(
        hMap.get(DcMotorEx::class.java, id),
        maxRPM,
        TPR,
        clock
    )

    /**
     * @param hMap The hardware map from the OpMode.
     * @param id The device id from the RC config.
     * @param type The type of the motor.
     */
    @JvmOverloads
    constructor(
        hMap: HardwareMap,
        id: String,
        type: Motor.Type,
        clock: NanoClock = NanoClock.system
    ) : this(hMap.get(DcMotorEx::class.java, id), type.maxRPM, type.TPR, clock)

    /**
     * @param motor The motor for the wrapper to use.
     * @param type The type of the motor.
     */
    @JvmOverloads
    constructor(
        motor: DcMotorEx,
        type: Motor.Type,
        clock: NanoClock = NanoClock.system
    ) : this(motor, type.maxRPM, type.TPR, clock)

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
    }

    enum class ZeroPowerBehavior {
        BRAKE, FLOAT
    }

    /**
     * A class with the specs of many motors so you don't have to find them.
     */
    enum class Type(val maxRPM: Double, val TPR: Double) {
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
     * @param getPosition the position supplier which points to the
     * current position of the motor in ticks
     * @param getVelocity the position supplier which points to the
     * current velocity of the motor in ticks per second
     */
    class Encoder internal constructor(
        private val getPosition: () -> Int,
        private val getVelocity: () -> Double,
        private val clock: NanoClock = NanoClock.system
    ) {
        /**
         * Constructs an encoder from the provided [id] of the corresponding motor.
         *
         * @param hMap the hardware map from the OpMode
         * @param id the device id from the RC config
         */
        constructor(hMap: HardwareMap, id: String) : this(
            hMap.get(DcMotorEx::class.java, id)::getCurrentPosition,
            hMap.get(DcMotorEx::class.java, id)::getVelocity,
        )

        companion object {
            /**
             * Utility method for constructing multiple encoders with the same specifications.
             */
            @JvmStatic
            fun multiple(hMap: HardwareMap, vararg ids: String): List<Encoder> = ids.map {
                Encoder(hMap, it)
            }

            /**
             * Utility method for constructing multiple encoders with the same specifications.
             * @param ids the ids of the encoders and whether they are reversed
             */
            @JvmStatic
            @SafeVarargs
            fun multiple(hMap: HardwareMap, vararg ids: Pair<String, Boolean>): List<Encoder> = ids.map {
                Encoder(hMap, it.first).apply { reversed = it.second }
            }
        }

        private var resetVal = 0
        private var lastPosition = 0

        /**
         * Whether the encoder is reversed. Independent of motor direction.
         */
        @JvmField
        var reversed: Boolean = false

        fun setReversed(reversed: Boolean): Encoder {
            this.reversed = reversed
            return this
        }

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
        var distancePerTick = 0.0

        /**
         * The current position of the encoder in ticks.
         */
        val position: Int
            get() {
                val currentPosition = (if (reversed) -1 else 1) * (getPosition() - resetVal)
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
         * The distance traveled by the encoder computed using [distancePerTick].
         */
        val distance: Double
            get() {
                if (distancePerTick == 0.0) throw IllegalStateException("distancePerTick is unset!")
                return distancePerTick * position
            }

        /**
         * The velocity of the encoder in units per second computed using [distancePerTick].
         */
        val distanceVelocity: Double
            get() {
                if (distancePerTick == 0.0) throw IllegalStateException("distancePerTick is unset!")
                return distancePerTick * velocity
            }

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
                var real = getVelocity() * (if (reversed) -1 else 1)
                while (abs(veloEstimate - real) > 0x10000 / 2.0) {
                    real += sign(veloEstimate - real) * 0x10000
                }
                return real
            }
    }

    @JvmField
    val encoder: Encoder = Encoder(internal::getCurrentPosition, internal::getVelocity, clock)

    /**
     * The maximum achievable ticks per second of the motor. Computed using [maxRPM] and [TPR].
     */
    @JvmField
    val maxTPS = maxRPM * TPR / 60

    var runMode: RunMode = RunMode.RUN_WITHOUT_ENCODER
        set(value) {
            if (value == RunMode.RUN_USING_ENCODER && field == RunMode.RUN_WITHOUT_ENCODER) {
                veloController.setTarget(0.0, 0.0)
            }
            field = value
        }
    var zeroPowerBehavior: ZeroPowerBehavior = ZeroPowerBehavior.FLOAT
        set(value) {
            internal.zeroPowerBehavior = when (value) {
                ZeroPowerBehavior.BRAKE -> DcMotor.ZeroPowerBehavior.BRAKE
                ZeroPowerBehavior.FLOAT -> DcMotor.ZeroPowerBehavior.FLOAT
            }
            field = value
        }
        get() = when (internal.zeroPowerBehavior) {
            DcMotor.ZeroPowerBehavior.BRAKE -> ZeroPowerBehavior.BRAKE
            else -> ZeroPowerBehavior.FLOAT
        }

    /**
     * PID coefficients used in [RunMode.RUN_USING_ENCODER].
     */
    var veloCoefficients: PIDCoefficients = PIDCoefficients(1.0)
        set(value) {
            veloController.pid = value
            field = value
        }

    /**
     * Feedforward used in [RunMode.RUN_USING_ENCODER] and optionally [RunMode.RUN_WITHOUT_ENCODER].
     * Note that these coefficients are applied to desired encoder tick velocity. This must be tuned in order for
     * [setTickVelocity], [setDistanceVelocity], and [setRPM] to work.
     */
    var feedforward: Feedforward = DCMotorFeedforward(1.0 / maxTPS)

    private var targetVel: Double = 0.0
    private var targetAccel: Double = 0.0

    /**
     * Whether the motor is reversed.
     */
    var reversed: Boolean = false
        /**
         * Sets whether the direction of the motor is reversed.
         */
        @JvmSynthetic
        set(value) {
            encoder.reversed = value
            field = value
        }
        @JvmName("isReversed")
        get


    private val veloController = PIDController(veloCoefficients, clock = clock)

    /**
     * Reverses the direction of the motor.
     */
    fun reversed(): Motor {
        reversed = !reversed
        return this
    }

    fun setReversed(reversed: Boolean): Motor {
        this.reversed = reversed
        return this
    }

    /**
     * Sets the velocity of the motor in encoder ticks per second.
     *
     * @param velocity The velocity to set.
     * @param acceleration The acceleration to set.
     */
    @JvmOverloads
    fun setTickVelocity(velocity: Double, acceleration: Double = 0.0) {
        when (runMode) {
            RunMode.RUN_USING_ENCODER -> {
                veloController.setTarget(velocity)
                internal.power = feedforward.calculate(
                    velocity,
                    acceleration,
                    veloController.update(this.velocity)
                )
            }

            RunMode.RUN_WITHOUT_ENCODER -> {
                internal.power = feedforward.calculate(
                    velocity,
                    acceleration
                )
            }
        }
    }

    /**
     * Sets the velocity of the motor in distance units per second.
     *
     * @param velocity The velocity to set.
     * @param acceleration The acceleration to set.
     */
    @JvmOverloads
    fun setDistanceVelocity(velocity: Double, acceleration: Double = 0.0) =
        setTickVelocity(velocity / distancePerTick, acceleration / distancePerTick)

    /**
     * Sets the velocity of the motor in revolutions per second.
     *
     * @param rpm the revolutions per second to set
     */
    fun setRPM(rpm: Double) =
        setTickVelocity(rpm * TPR / 60)

    /**
     * The percentage of power to the motor in the range `[-1.0, 1.0]`. This uses no feedback control whatsoever.
     */
    var power: Double = 0.0
        set(value) {
            runMode = RunMode.RUN_WITHOUT_ENCODER
            internal.power = value * (if (reversed) -1.0 else 1.0)
            field = value
        }
        get() = internal.power * (if (reversed) -1.0 else 1.0)

    /**
     * Updates [RunMode.RUN_USING_ENCODER]. Running this method is
     * not necessary for [RunMode.RUN_WITHOUT_ENCODER].
     */
    override fun update() {
        if (runMode == RunMode.RUN_USING_ENCODER) {
            internal.power = feedforward.calculate(
                targetVel,
                targetAccel,
                veloController.update(this.velocity).also { println(it) }
            )
        }
    }

    /**
     * The distance per revolution travelled by the motor.
     * @see distance
     * @see distanceVelocity
     */
    var distancePerTick: Double = encoder.distancePerTick
        get() = encoder.distancePerTick
        set(value) {
            encoder.distancePerTick = value
            field = value
        }

    /**
     * The maximum achievable distance velocity of the motor, in units per second.
     * Computed using [maxTPS] and [distancePerTick].
     */
    val maxDistanceVelocity get() = distancePerTick * maxTPS

    /**
     * The velocity of the motor, in ticks per second. Computed using the encoder.
     */
    val velocity
        get() = encoder.velocity

    /**
     * The total rotation of the motor. Note that this value is positive or negative according to motor direction.
     */
    val rotation: Angle get() = (encoder.position / TPR * 2 * PI).rad

    /**
     * The target velocity of [RunMode.RUN_USING_ENCODER] in ticks per second.
     */
    val targetVelocity: Double get() = veloController.targetPosition

    /**
     * The current position of the encoder (in ticks).
     */
    val currentPosition
        get() = encoder.position

    /**
     * Resets the encoder.
     */
    fun resetEncoder() = encoder.reset()

    /**
     * The distance travelled by the motor. Computed using the encoder and [distancePerTick].
     * @see distanceVelocity
     * @see distancePerTick
     */
    val distance
        get() = encoder.distance

    /**
     * The velocity of the motor in distance per second. Computed using the encoder and [distancePerTick].
     * @see distance
     * @see distancePerTick
     */
    val distanceVelocity
        get() = encoder.distanceVelocity
}