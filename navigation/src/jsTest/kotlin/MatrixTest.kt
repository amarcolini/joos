import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.deg
import space.kscience.kmath.linear.column
import space.kscience.kmath.linear.linearSpace
import space.kscience.kmath.linear.lupSolver
import space.kscience.kmath.operations.algebra
import kotlin.random.Random
import kotlin.test.Test

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
}