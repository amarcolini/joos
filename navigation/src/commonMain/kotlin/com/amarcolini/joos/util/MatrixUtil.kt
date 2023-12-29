package com.amarcolini.joos.util

class Matrix3x3(
    private val a: Double,
    private val b: Double,
    private val c: Double,
    private val d: Double,
    private val e: Double,
    private val f: Double,
    private val g: Double,
    private val h: Double,
    private val i: Double,
) {
    constructor(data: Array<DoubleArray>) : this(
        data[0][0],
        data[0][1],
        data[0][2],
        data[1][0],
        data[1][1],
        data[1][2],
        data[2][0],
        data[2][1],
        data[2][2],
    )

    fun getInverse(): Matrix3x3? {
        val det00 = e * i - f * h
        val det10 = f * g - d * i
        val det20 = d * h - e * g
        val det = a * det00 + b * det10 + c * det20
        if (det epsilonEquals 0.0) return null
        val det01 = c * h - b * i
        val det02 = b * f - c * e
        val det11 = a * i - c * g
        val det12 = c * d - a * f
        val det21 = b * g - a * h
        val det22 = a * e - b * d

        val invDet = 1.0 / det
        return Matrix3x3(
            invDet * det00, invDet * det01, invDet * det02,
            invDet * det10, invDet * det11, invDet * det12,
            invDet * det20, invDet * det21, invDet * det22,
        )
    }

    operator fun times(vector: DoubleArray): DoubleArray {
        val (a0, a1, a2) = vector
        val b0 = a * a0 + b * a1 + c * a2
        val b1 = d * a0 + e * a1 + f * a2
        val b2 = g * a0 + h * a1 + i * a2
        return doubleArrayOf(b0, b1, b2)
    }

    operator fun times(vector: List<Double>): DoubleArray {
        val (a0, a1, a2) = vector
        val b0 = a * a0 + b * a1 + c * a2
        val b1 = d * a0 + e * a1 + f * a2
        val b2 = g * a0 + h * a1 + i * a2
        return doubleArrayOf(b0, b1, b2)
    }
}