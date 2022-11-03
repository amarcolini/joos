package com.amarcolini.joos.util

import kotlin.jvm.JvmStatic

expect class Matrix
/**
 * Creates a matrix of the specified size with an initializer providing every row.
 */
    (rows: Int, columns: Int, initializer: (Int) -> DoubleArray = { DoubleArray(rows) }) {

    companion object {
        @JvmStatic
        fun column(elements: List<Double>): Matrix

        @JvmStatic
        fun row(elements: List<Double>): Matrix
    }

    operator fun get(row: Int, column: Int): Double

    operator fun set(row: Int, column: Int, value: Double)

    fun solver(): MatrixSolver
}

interface MatrixSolver {
    fun solve(b: Matrix): Matrix
}