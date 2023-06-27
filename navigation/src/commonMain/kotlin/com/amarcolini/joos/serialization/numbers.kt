package com.amarcolini.joos.serialization

import kotlin.math.abs
import kotlin.math.roundToLong

/*
Various multiplatform string tools for dealing with numbers.
Uses code from [this library](https://github.com/sergeych/mp_stools/blob/master/src/commonMain/kotlin/net/sergeych/sprintf/ExponentFormatter.kt).
 */


/**
 * Formats a double to the desired number of decimals or characters.
 */
fun Double.format(decimals: Int = -1, width: Int = -1): String {
    if (this.isNaN()) return "NaN"
    if (this == Double.POSITIVE_INFINITY) return "∞"
    if (this == Double.NEGATIVE_INFINITY) return "-∞"

    var value = this
    val result = StringBuilder()

    if (abs(value) >= 1) {
        val i = if (decimals == 0) value.roundToLong() else value.toLong()
        result.append(i)
        result.append('.')
        value -= i
    } else result.append((if (value < 0) "-0." else "0."))

    var fl = if (decimals < 0) {
        if (width < 0) 6
        else width - result.length
    } else decimals

    var rest = value * 10
    while (fl-- > 0) {
        val d = rest.toInt()
        result.append(abs(d))
        rest = (rest - d) * 10
    }
    // now we might need to round it up:
    return if (abs(rest.toInt()) < 5) result.toString() else roundUp(result, keepWidth = false).first
}

/**
 * Round up the mantissa part (call it with default arguments to start).
 * @return rounded mantissa and overflow flag (set when 9.99 -> 10.00 and like)
 */
private fun roundUp(
    result: StringBuilder, pos: Int = result.length - 1, keepWidth: Boolean = true
): Pair<String, Boolean> {
    if (pos < 0) {
        // if we get there, it means the number of digits should grow, like "9.99" -> "10.00"
        // but we need to keep the length so "10.0":
        result.insert(0, '1')
        if (keepWidth) result.deleteAt(result.length)
        return result.toString() to true
    }
    // not the first digit: perform rounding:
    val d = result[pos]
    // it could be a decimal point we ignore and continue with rounding
    if (d == '.') return roundUp(result, pos - 1, keepWidth)

    // Small number add one "0.19" -> "0.2"
    // Simple case: alter only the current digit
    if (d != '9') {
        result[pos] = d + 1
        return result.toString() to false
    }
    // Complex case:  9->0 and propogate changes up.
    result[pos] = '0'
    return roundUp(result, pos - 1, keepWidth)
}