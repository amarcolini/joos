package com.amarcolini.joos

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.deg
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import org.ejml.data.DMatrixRMaj
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM
import org.junit.jupiter.api.Test
import space.kscience.kmath.linear.column
import space.kscience.kmath.linear.linearSpace
import space.kscience.kmath.linear.lupSolver
import space.kscience.kmath.operations.algebra

class MatrixTest {
    @Test
    fun kmathBenchmark() {
        val clock = NanoClock.system()
        val start = clock.seconds()

        val wheelPoses = listOf(
            Pose2d(0.0, 5.0, 0.0),
            Pose2d(0.0, -5.0, 0.0),
            Pose2d(5.0, 0.0, 90.deg)
        )
        val inverseMatrix =
            Double.algebra.linearSpace.buildMatrix(3, 3) { i, j ->
                val orientationVector = wheelPoses[i].headingVec()
                val positionVector = wheelPoses[i].vec()
                when (j) {
                    0 -> orientationVector.x
                    1 -> orientationVector.y
                    2 -> positionVector.x * orientationVector.y - positionVector.y * orientationVector.x
                    else -> 0.0
                }
            }
        val forwardSolver = Double.algebra.linearSpace.lupSolver()
//        for (i in 0..2) {
//            val orientationVector = wheelPoses[i].headingVec()
//            val positionVector = wheelPoses[i].vec()
//            inverseMatrix[i, 0] = orientationVector.x
//            inverseMatrix[i, 1] = orientationVector.y
//            inverseMatrix[i, 2] = positionVector.x * orientationVector.y - positionVector.y * orientationVector.x
//        }

        repeat(2000) {
            val result = forwardSolver.solve(inverseMatrix, Double.algebra.linearSpace.column(0.0, 0.0, 0.0))
        }

        val end = clock.seconds()
        println("kmath benchmark: ${end - start}")
    }

    @Test
    fun apacheBenchmark() {
        val clock = NanoClock.system()
        val start = clock.seconds()

        val wheelPoses = listOf(
            Pose2d(0.0, 5.0, 0.0),
            Pose2d(0.0, -5.0, 0.0),
            Pose2d(5.0, 0.0, 90.deg)
        )
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

        repeat(2000) {
            val result = forwardSolver.solve(
                MatrixUtils.createRealMatrix(
                    arrayOf(doubleArrayOf(0.0, 0.0, 0.0))
                ).transpose()
            )
        }

        val end = clock.seconds()
        println("apache benchmark: ${end - start}")
    }

    @Test
    fun ejmlBenchmark() {
        val clock = NanoClock.system()
        val start = clock.seconds()

        val wheelPoses = listOf(
            Pose2d(0.0, 5.0, 0.0),
            Pose2d(0.0, -5.0, 0.0),
            Pose2d(5.0, 0.0, 90.deg)
        )
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

        repeat(2000) {
            val result = DMatrixRMaj()
            forwardSolver.solve(DMatrixRMaj(doubleArrayOf(0.0, 0.0, 0.0)), result)
        }

        val end = clock.seconds()
        println("ejml benchmark: ${end - start}")
    }
}