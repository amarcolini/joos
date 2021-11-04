package com.griffinrobotics.lib.hardware

import com.griffinrobotics.lib.command.CommandScheduler
import com.griffinrobotics.lib.command.Component
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import java.lang.Double.max
import java.lang.Double.min

/**
 * A wrapper for the [Servo] object in the FTC SDK.
 *
 * @param servo the servo for this wrapper to use
 * @param startAngle the angle this servo starts at, excluding the gear ratio (in radians)
 * @param endAngle the angle this servo ends at, excluding the gear ratio (in radians)
 * @param RPM the rpm of the servo. This is used to estimate how long it will take to turn to a certain position.
 * @param gearRatio the gear ratio of this servo
 */
//TODO: Update Servo to have turn estimation and motion profiling.
class Servo @JvmOverloads constructor(
    val servo: Servo,
    val startAngle: Double = 0.0,
    val endAngle: Double = 180.0,
    val RPM: Double = 90.0,
    val gearRatio: Double = 1.0
) : Component {
    /**
     * @param hMap the hardware map from the OpMode
     * @param id the device id from the RC config
     * @param startAngle the angle this servo starts at, excluding the gear ratio (in radians)
     * @param endAngle the angle this servo ends at, excluding the gear ratio (in radians)
     * @param gearRatio the gear ratio of this servo
     */
    @JvmOverloads
    constructor(
        hMap: HardwareMap,
        id: String,
        startAngle: Double = 0.0,
        endAngle: Double = 180.0,
        gearRatio: Double = 1.0
    ) : this(
        hMap.get(Servo::class.java, id),
        startAngle, endAngle, gearRatio
    )

    /**
     * The logical direction in which this servo operates.
     */
    var direction: Direction = Direction.FORWARD
        set(value) {
            servo.direction =
                if (value == Direction.FORWARD) Servo.Direction.FORWARD else Servo.Direction.REVERSE
            field = value
        }

    /**
     * Angular representation of the servo's position in the range `[`[startAngle], [endAngle]`]`.
     * Setting the angle sets [position] and vice versa. Note that the angle of the servo is independent of
     * [scaleRange].
     * @see position
     */
    var angle: Double = startAngle
        get() {
            val (start, end) = currentRange
            val actual = start + (end - start) * position
            val (startAngle, endAngle) = currentAngleRange
            return (startAngle + (startAngle - endAngle) * actual) / gearRatio
        }
        set(value) {
            val (start, end) = currentAngleRange
            field = max(min(start, value * gearRatio), end)
            position = (angle - start) / (end - start)
        }

    /**
     * The currently available movement range of the servo in radians. Affected by [scaleRange].
     * @see scaleRange
     */
    var currentAngleRange: Pair<Double, Double> = startAngle to endAngle
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
    For example, if `scaleRange(0.2, 0.8)` is set; then servo positions will be scaled to fit in that range:

    `setPosition(0.0)` scales to `0.2`

    `setPosition(1.0)` scales to `0.8`

    `setPosition(0.5)` scales to `0.5`

    `setPosition(0.25)` scales to `0.35`

    `setPosition(0.75)` scales to `0.65`

    Note the parameters passed here are relative to the underlying full range of motion of the servo, not its currently scaled range, if any. Thus, scaleRange(0.0, 1.0) will reset the servo to its full range of movement.
     * @see position
     */
    fun scaleRange(min: Double, max: Double) {
        servo.scaleRange(min, max)
        currentRange = min to max
        val delta = endAngle - startAngle
        currentAngleRange = startAngle + min * delta to startAngle + max * delta
    }

    /**
     * The position of this servo in the range `[0.0, 1.0]`.
     * @see angle
     */
    var position: Double = 0.0
        set(value) {
            field = max(min(0.0, value), 1.0)
            servo.position = field
        }
}