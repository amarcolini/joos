package com.amarcolini.joos.serialization

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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
    } else
        result.append((if (value < 0) "-0." else "0."))

    var fl: Int = if (decimals < 0) {
        if (width < 0) 6
        else width - result.length
    } else if (width < 0) decimals else min(width - result.length, decimals)
    var rest = value * 10
    while (fl-- > 0) {
        val d = if (fl > 0) rest.toInt() else rest.roundToInt()
        result.append(abs(d))
        rest = (rest - d) * 10
    }
    return result.toString()
}