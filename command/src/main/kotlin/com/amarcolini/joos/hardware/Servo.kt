package com.amarcolini.joos.hardware

import com.amarcolini.joos.command.Command
import com.amarcolini.joos.command.Component
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.profile.MotionProfile
import com.amarcolini.joos.profile.MotionProfileGenerator
import com.amarcolini.joos.profile.MotionState
import com.amarcolini.joos.util.*
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import kotlin.math.max
import kotlin.math.min

/**
 * A wrapper for the [Servo] object in the FTC SDK.
 *
 * @param servo the servo for this wrapper to use
 * @param range the range of the servo
 */
class Servo @JvmOverloads constructor(
    private val servo: Servo,
    @JvmField
    val range: Angle = 300.deg,
    private val clock: NanoClock = NanoClock.system()
) : Component {
    /**
     * @param servo the servo for this wrapper to use
     * @param range the range of the servo in [Angle.defaultUnits]
     */
    @JvmOverloads
    constructor(servo: Servo, range: Double, clock: NanoClock = NanoClock.system()) : this(servo, Angle(range), clock)

    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param range the range of the servo
     */
    @JvmOverloads
    constructor(
        hMap: HardwareMap,
        id: String,
        range: Angle = 180.deg,
        clock: NanoClock = NanoClock.system()
    ) : this(
        hMap.get(Servo::class.java, id),
        range, clock
    )

    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param range the range of the servo in [Angle.defaultUnits]
     */
    constructor(
        hMap: HardwareMap,
        id: String,
        range: Double,
        clock: NanoClock = NanoClock.system()
    ) : this(
        hMap.get(Servo::class.java, id),
        range, clock
    )

    /**
     * Whether this servo is reversed.
     */
    var reversed: Boolean = false
        @JvmName("setReversed")
        set(value) {
            servo.direction =
                if (value) Servo.Direction.REVERSE else Servo.Direction.FORWARD
            field = value
        }
        @JvmName("isReversed") get

    /**
     * Angular representation of the servo's position in the range `[0.0, `[range]`]`.
     * Setting the angle sets [position] and vice versa. Note that the angle of the servo is independent of
     * [scaleRange], but the available angle range is not.
     *
     * @see position
     * @see scaleRange
     */
    var angle: Angle = 0.deg
        get() = range * servo.position
        set(value) {
            val actual = value.coerceIn(currentAngleRange.first, currentAngleRange.second)
            field = actual
            servo.position = actual / range
        }

    /**
     * The currently available movement range of the servo. Affected by [scaleRange].
     * @see scaleRange
     */
    var currentAngleRange: Pair<Angle, Angle> = 0.deg to range
        private set

    /**
     * The currently available movement range of the servo. Affected by [scaleRange].
     * @see scaleRange
     */
    var currentRange: Pair<Double, Double> = 0.0 to 1.0
        private set

    /**
     * Scales the available movement range of the servo to be a subset of its maximum range. Subsequent
     * positioning calls will operate within that subset range. This is useful if your servo has only a limited
     * useful range of movement due to the physical hardware that it is manipulating (as is often the case) but
     * you don't want to have to manually scale and adjust the input to [position] each time.
     * For example, if `scaleRange(0.2, 0.8)` is set; then servo positions will be scaled to fit in that range:
     *
     * `setPosition(0.0)` scales to `0.2`
     *
     * `setPosition(1.0)` scales to `0.8`
     *
     * `setPosition(0.5)` scales to `0.5`
     *
     * `setPosition(0.25)` scales to `0.35`
     *
     * `setPosition(0.75)` scales to `0.65`
     *
     * Note the parameters passed here are relative to the underlying full range of motion of the servo,
     * not its currently scaled range, if any. Thus, scaleRange(0.0, 1.0) will reset the servo to
     * its full range of movement.
     * @see position
     */
    fun scaleRange(min: Double, max: Double) {
        val correctedMin = min.coerceIn(0.0, 1.0)
        val correctedMax = max.coerceIn(0.0, 1.0)
        val actualMin = min(correctedMin, correctedMax)
        val actualMax = max(correctedMin, correctedMax)
        currentRange = actualMin to actualMax
        currentAngleRange = range * actualMin to range * actualMax
    }

    /**
     * Scales the available movement range of the servo to be a subset of its maximum range. Subsequent
     * positioning calls will operate within that subset range. This is useful if your servo has only a limited
     * useful range of movement due to the physical hardware that it is manipulating (as is often the case) but
     * you don't want to have to manually adjust the input to [position] each time.
     * For example, if the range of the servo is 180°, and `scaleRange(30°, 90°)` is set; then servo 
     * positions will be clamped to fit in that range:
     *
     * `setPosition(10°)` is clamped to `30°`
     *
     * `setPosition(135°)` is clamped to `90°`
     *
     * `setPosition(60°)` still becomes `60°`
     *
     * And scaleRange(0°, 180°) would reset the servo to its full range of movement.
     * Note that this also scales the position range as well.
     * @see position
     */
    fun scaleRange(min: Angle, max: Angle) {
        val correctedMin = min.coerceIn(0.deg, range)
        val correctedMax = max.coerceIn(0.deg, range)
        val actualMin = min(correctedMin, correctedMax)
        val actualMax = max(correctedMin, correctedMax)
        currentRange = actualMin / range to actualMax / range
        currentAngleRange = actualMin to actualMax
    }

    /**
     * The position of this servo in the range `[0.0, 1.0]`.
     * @see angle
     */
    var position: Double = 0.0
        set(value) {
            field = value.coerceIn(0.0, 1.0)
            servo.position = field * (currentRange.second - currentRange.first) + currentRange.first
        }

    /**
     * Returns a command that uses [range] and [speed] (specified in units per second) to go to the specified position
     * at the desired speed. Note that since there is no feedback from the servo, it may or may not
     * actually achieve the desired speed.
     */
    fun goToPosition(position: Double, speed: Angle): Command {
        val actualSpeed = speed / range
        lateinit var profile: MotionProfile
        var start = Double.NaN
        return Command.of {
            val t = clock.seconds() - start
            this.position = profile[t].x
        }
            .onInit {
                profile = MotionProfileGenerator.generateSimpleMotionProfile(
                    MotionState(this.position, actualSpeed),
                    MotionState(position, actualSpeed),
                    actualSpeed, 0.0
                )
                start = clock.seconds()
            }
            .runUntil { this.position == position }
            .requires(this)
    }

    /**
     * Returns a command that uses [range] and [RPM] to go to the specified position
     * at the desired speed. Note that since there is no feedback from the servo, it may or may not
     * actually achieve the desired speed.
     */
    fun goToPosition(position: Double, RPM: Double): Command = goToPosition(position, RPM / 60 * 360.deg)

    /**
     * Returns a command that uses [range] and [speed] (specified in units per second) to go to the specified angle
     * at the desired speed. Note that since there is no feedback from the servo, it may or may not
     * actually achieve the desired speed.
     */
    fun goToAngle(angle: Angle, speed: Angle): Command = goToPosition(angle / range, speed)

    /**
     * Returns a command that uses [range] and [RPM] to go to the specified angle
     * at the desired speed. Note that since there is no feedback from the servo, it may or may not
     * actually achieve the desired speed.
     */
    fun goToAngle(angle: Angle, RPM: Double): Command = goToPosition(angle / range, RPM / 60 * 360.deg)
}