package com.amarcolini.joos

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.epsilonEquals
import com.amarcolini.joos.util.rad
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import org.ejml.data.DMatrixRMaj
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM
import org.junit.jupiter.api.Test

class MatrixTest {
    private fun getWheelPoses() = listOf(
        Pose2d(Math.random(), 5.0, 0.deg),
        Pose2d(0.0, -5.0 + Math.random(), 0.deg),
        Pose2d(5.0, Math.random(), 90.deg)
    )

    private fun getWheelDeltas() = doubleArrayOf(
        (Math.random() - 0.5) * 2,
        (Math.random() - 0.5) * 2,
        (Math.random() - 0.5) * 2,
    )

    private val times = 2000

    @Test
    fun apacheBenchmark() {
        val clock = NanoClock.system
        val start = clock.seconds()

        val wheelPoses = getWheelPoses()
        val inverseMatrix = Array2DRowRealMatrix(3, 3)
        for (i in 0..2) {
            val orientationVector = wheelPoses[i].headingVec()
            val positionVector = wheelPoses[i].vec()
            inverseMatrix.setEntry(i, 0, orientationVector.x)
            inverseMatrix.setEntry(i, 1, orientationVector.y)
            inverseMatrix.setEntry(
                i,
                2,
                positionVector.x * orientationVector.y - positionVector.y * orientationVector.x
            )
        }
        val forwardSolver = LUDecomposition(inverseMatrix).solver

        repeat(times) {
            val result = forwardSolver.solve(
                MatrixUtils.createRealMatrix(
                    arrayOf(getWheelDeltas())
                ).transpose()
            )
        }

        val end = clock.seconds()
        println("apache benchmark: ${end - start}")
    }

    @Test
    fun ejmlBenchmark() {
        val clock = NanoClock.system
        val start = clock.seconds()

        val wheelPoses = getWheelPoses()
        val inverseMatrix = DMatrixRMaj(3, 3)
        for (i in 0..2) {
            val orientationVector = wheelPoses[i].headingVec()
            val positionVector = wheelPoses[i].vec()
            inverseMatrix[i, 0] = orientationVector.x
            inverseMatrix[i, 1] = orientationVector.y
            inverseMatrix[i, 2] = positionVector.x * orientationVector.y - positionVector.y * orientationVector.x
        }
        val forwardSolver = LinearSolverFactory_DDRM.linear(3)
        forwardSolver.setA(inverseMatrix)

        repeat(times) {
            val result = DMatrixRMaj()
            forwardSolver.solve(DMatrixRMaj(getWheelDeltas()), result)
        }

        val end = clock.seconds()
        println("ejml benchmark: ${end - start}")
    }

    @Test
    fun customSimpleBenchmark() {
        val clock = NanoClock.system
        val start = clock.seconds()

        val wheelPoses = getWheelPoses()
        val inverseMatrix = arrayOf(
            DoubleArray(3),
            DoubleArray(3),
            DoubleArray(3),
        )
        for (i in 0..2) {
            val orientationVector = wheelPoses[i].headingVec()
            val positionVector = wheelPoses[i].vec()
            val row = inverseMatrix[i]
            row[0] = orientationVector.x
            row[1] = orientationVector.y
            row[2] = positionVector.x * orientationVector.y - positionVector.y * orientationVector.x
        }

        val row0 = inverseMatrix[0]
        val row1 = inverseMatrix[1]
        val row2 = inverseMatrix[2]
        val a = row0[0]
        val b = row0[1]
        val c = row0[2]
        val d = row1[0]
        val e = row1[1]
        val f = row1[2]
        val g = row2[0]
        val h = row2[1]
        val i = row2[2]

        val det00 = e * i - f * h
        val det10 = f * g - d * i
        val det20 = d * h - e * g
        val det = a * det00 + b * det10 + c * det20
        if (det epsilonEquals 0.0) throw IllegalArgumentException("Matrix is singular!")
        val det01 = c * h - b * i
        val det02 = b * f - c * e
        val det11 = a * i - c * g
        val det12 = c * d - a * f
        val det21 = b * g - a * h
        val det22 = a * e - b * d

        val invDet = 1.0 / det
        val forwardMatrix = arrayOf(
            doubleArrayOf(invDet * det00, invDet * det01, invDet * det02),
            doubleArrayOf(invDet * det10, invDet * det11, invDet * det12),
            doubleArrayOf(invDet * det20, invDet * det21, invDet * det22),
        )

        repeat(times) {
            val (d1, d2, d3) = getWheelDeltas()
            val rowX = forwardMatrix[0]
            val x = rowX[0] * d1 + rowX[1] * d2 + rowX[2] * d3
            val rowY = forwardMatrix[1]
            val y = rowY[0] * d1 + rowY[1] * d2 + rowY[2] * d3
            val rowTheta = forwardMatrix[2]
            val theta = rowTheta[0] * d1 + rowTheta[1] * d2 + rowTheta[2] * d3
            val poseDelta = Pose2d(x, y, theta.rad)
        }

        val end = clock.seconds()
        println("customSimple benchmark: ${end - start}")
    }
}