package com.amarcolini.joos.util

import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix

/**
 * Creates a matrix of the specified size with an initializer providing every row.
 */
actual class Matrix private constructor(private val internal: RealMatrix) {
    actual constructor(
        rows: Int,
        columns: Int,
        initializer: (Int) -> DoubleArray
    ) : this(MatrixUtils.createRealMatrix(Array(rows, initializer)))

    actual companion object {
        @JvmStatic
        actual fun column(elements: List<Double>): Matrix =
            Matrix(MatrixUtils.createColumnRealMatrix(elements.toDoubleArray()))

        @JvmStatic
        actual fun row(elements: List<Double>): Matrix =
            Matrix(MatrixUtils.createRowRealMatrix(elements.toDoubleArray()))

    }

    actual operator fun get(row: Int, column: Int): Double = internal.getEntry(row, column)

    actual operator fun set(row: Int, column: Int, value: Double) = internal.setEntry(row, column, value)

    actual fun solver(): MatrixSolver = object : MatrixSolver {
        private val solver = LUDecomposition(internal).solver
        override fun solve(b: Matrix): Matrix = Matrix(solver.solve(b.internal))
    }
}