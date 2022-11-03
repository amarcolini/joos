package com.amarcolini.joos.util

import space.kscience.kmath.linear.linearSpace
import space.kscience.kmath.linear.lupSolver
import space.kscience.kmath.operations.algebra

/**
 * Creates a matrix of the specified size with an initializer providing every row.
 */
actual class Matrix actual constructor(
    internal val rows: Int,
    internal val columns: Int,
    initializer: (Int) -> DoubleArray
) {
    private val data: Array<DoubleArray> = Array(rows, initializer)

    actual companion object {
        actual fun column(elements: List<Double>): Matrix = Matrix(elements.size, 1) {
            doubleArrayOf(elements[it])
        }

        actual fun row(elements: List<Double>): Matrix = Matrix(1, elements.size) {
            elements.toDoubleArray()
        }
    }

    actual operator fun get(row: Int, column: Int): Double {
        require(row in 0 until rows && column in 0 until column) { "Position out of matrix bounds." }
        return data[row][column]
    }

    actual operator fun set(row: Int, column: Int, value: Double) {
        data[row][column] = value
    }

    actual fun solver(): MatrixSolver = object : MatrixSolver {
        private val internal = Double.algebra.linearSpace.lupSolver()
        private val a = KMathMatrix(this@Matrix)

        override fun solve(b: Matrix): Matrix {
            val result = internal.solve(a, KMathMatrix(b))
            val matrix = Matrix(result.rowNum, result.colNum)
            for (row in 0 until result.rowNum) {
                for (column in 0 until result.colNum) {
                    matrix[row, column] = result[row, column]
                }
            }
            return matrix
        }
    }
}

private class KMathMatrix(val inner: Matrix) : space.kscience.kmath.linear.Matrix<Double> {
    override val colNum: Int = inner.columns
    override val rowNum: Int = inner.rows

    override fun get(i: Int, j: Int): Double = inner[i, j]
}