package com.amarcolini.joos.util

import com.amarcolini.joos.serialization.format

class Polynomial(vararg coeffs: Double) {
    val coeffs = coeffs.dropWhile { it == 0.0 }.toDoubleArray()
    val degree = coeffs.lastIndex

    /**
     * Evaluates this polynomial at [x].
     */
    operator fun get(x: Double): Double {
        var result = 0.0
        for (c in coeffs) {
            result = result * x + c
        }
        return result
    }

    operator fun plus(other: Polynomial): Polynomial {
        val (bigger, smaller) =
            if (coeffs.size > other.coeffs.size) coeffs to other.coeffs
            else other.coeffs to coeffs
        val out = bigger.copyOf()
        val diff = bigger.size - smaller.size
        for (i in smaller.indices) {
            out[i + diff] += smaller[i]
        }
        return Polynomial(*out)
    }

    operator fun times(other: Polynomial): Polynomial {
        val out = DoubleArray(coeffs.size + other.coeffs.size - 1)
        for (i in coeffs.indices) {
            val monomial = coeffs[i]
            for (j in other.coeffs.indices) {
                out[i + j] += monomial * other.coeffs[j]
            }
        }
        return Polynomial(*out)
    }

    operator fun times(scalar: Double): Polynomial {
        val out = coeffs.copyOf()
        for (i in out.indices) {
            out[i] *= scalar
        }
        return Polynomial(*out)
    }

    operator fun unaryMinus(): Polynomial = this * -1.0

    operator fun minus(other: Polynomial): Polynomial {
        val (bigger, smaller) =
            if (coeffs.size > other.coeffs.size) coeffs to other.coeffs
            else other.coeffs to coeffs
        val out = bigger.copyOf()
        val diff = bigger.size - smaller.size
        for (i in smaller.indices) {
            out[i + diff] -= smaller[i]
        }
        return Polynomial(*out)
    }

    fun deriv(): Polynomial {
        if (coeffs.size == 1) return Polynomial(0.0)
        val out = DoubleArray(coeffs.size - 1)
        for(i in 0..<coeffs.lastIndex) {
            out[i] = coeffs[i] * (coeffs.lastIndex - i)
        }
        return Polynomial(*out)
    }

    override fun toString(): String {
        return coeffs.mapIndexed { i, it -> it.format(2) + if (degree - i > 0) "x^${degree - i}" else "" }.joinToString(" + ")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Polynomial) return false
        return coeffs.contentEquals(other.coeffs)
    }

    override fun hashCode(): Int {
        return coeffs.contentHashCode()
    }
}