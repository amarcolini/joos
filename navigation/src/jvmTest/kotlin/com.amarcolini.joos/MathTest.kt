package com.amarcolini.joos

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.kinematics.SwerveKinematics
import com.amarcolini.joos.path.PathBuilder
import com.amarcolini.joos.path.PositionPath
import com.amarcolini.joos.path.QuinticSpline
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.constraints.GenericConstraints
import com.amarcolini.joos.util.*
import org.apache.commons.math3.util.FastMath
import org.junit.jupiter.api.Test
import utils.benchmark.logBenchmark
import kotlin.math.PI
import kotlin.math.sin

class MathTest {
    @Test
    fun testWrap() {
        assert(1.wrap(1, 3) == 3 || 1.wrap(1, 3) == 1)
        println(3.wrap(1, 3) == 3 || 3.wrap(1, 3) == 1)
        assert(2.wrap(1, 3) == 2)
        assert(4.wrap(1, 3) == 2)
        assert(0.wrap(1, 3) == 2)
    }

    @Test
    fun testAngleConversionSpeeds() {
        logBenchmark(
            times = 10_000_000,
            warmup = 20,
            logger = ::println,
            "kotlin" to { sin(Math.random() * 360) },
            "kotlin with convert" to { sin(Math.toRadians(Math.random() * 360)) },
            "kotlin with fast convert" to { sin(FastMath.toRadians(Math.random() * 360)) },
            "FastMath with convert" to { FastMath.sin(Math.toRadians(Math.random() * 360)) },
            "FastMath" to { FastMath.sin(Math.random() * 360) },
            "java" to { Math.sin(Math.random() * 360) },
            "java with convert" to { Math.sin(Math.toRadians(Math.random() * 360)) },
        )
    }

    @Test
    fun testPascalsTriangle() {
        println(generatePascalsTriangle(4).map { it.toList() })
    }

    @Test
    fun testPolynomialTranslation() {
        val a = 1.0
        val b = 3.0
        val c = 4.0
        val source = doubleArrayOf(a, b, c)
        assert(translate(source).contentEquals(doubleArrayOf(a, 2 * a + b, a + b + c)))
    }

    @Test
    fun testRootFinding() {
        val a = -0.7
        val b = 0.9
        val c = -0.1
        val roots = solveQuadratic(a, b, c).filter { it in (0.0..1.0) }
        val computedRoots = isolateRoots(
            Polynomial(a, b, c)
        )
        println(roots)
        println(computedRoots)
        assert(roots.size == computedRoots.size && roots.zip(computedRoots).all {
            it.first in it.second
        })
    }

    @Test
    fun testBezierProjection() {
        val spline = QuinticSpline(
            QuinticSpline.Knot(Vector2d(), Vector2d(15.0, 0.0)),
            QuinticSpline.Knot(Vector2d(30.0, 30.0), Vector2d(15.0, 0.0)),
        )
        val query = Vector2d(5.0, 20.0)
        val t = spline.project(query, EPSILON)
        val actualT = spline.reparam(PositionPath(spline).run {
            reparameterize()
            project(query, 3.0)
        })
        assert(t epsilonEquals actualT)
        println("$t, ${spline.internalGet(t) distTo query}")
        println("$actualT, ${spline.internalGet(actualT) distTo query}")
    }

    @Test
    fun benchmarkBezierProjection() {
        val spline = QuinticSpline(
            QuinticSpline.Knot(Vector2d(), Vector2d(100.0, 0.0)),
            QuinticSpline.Knot(Vector2d(0.0, 30.0), Vector2d(-100.0, 0.0)),
        )
        val path = PositionPath(spline)
        path.reparameterize()
        logBenchmark(
            times = 500_000,
            warmup = 200,
            "bezier projection" to {
                val query = Vector2d(
                    (Math.random() - 0.2) * 50.0,
                    (Math.random() - 0.2) * 50.0,
                )
                spline.project(query)
            },
            "bezier projection (fast)" to {
                val query = Vector2d(
                    (Math.random() - 0.2) * 50.0,
                    (Math.random() - 0.2) * 50.0,
                )
                spline.project(query, 0.1)
            },
            "arc length projection (fast)" to {
                val query = Vector2d(
                    (Math.random() - 0.2) * 50.0,
                    (Math.random() - 0.2) * 50.0,
                )
                path.fastProject(query)
            },
            "arc length projection" to {
                val query = Vector2d(
                    (Math.random() - 0.2) * 50.0,
                    (Math.random() - 0.2) * 50.0,
                )
                path.project(query)
            }
        )
    }

    @Test
    fun testIntegration() {
        logBenchmark(
            times = 1,
            warmup = 20,
            logger = ::println,
            "path" to {
                PathBuilder(Pose2d())
                    .splineTo(Vector2d(30.0, 30.0), 0.rad)
                    .splineTo(Vector2d(0.0, 0.0), 180.rad)
                    .build()
            }
        )
    }

    @Test
    fun testProjection() {
        val path = PathBuilder(Pose2d())
            .splineTo(Vector2d(30.0, 30.0), 0.deg)
            .build()
        val spline = path.segments[0].curve as QuinticSpline
        val times = 100_000
        fun naiveProject(vec: Vector2d): Double {
            var curr = 0.0;
            var dist = (spline[0.0, curr] - vec).squaredNorm()
            for (t in DoubleProgression.fromClosedInterval(0.0, 1.0, 100)) {
                val new = (spline[0.0, t] - vec).squaredNorm()
                if (new < dist) {
                    curr = t
                    dist = new
                }
            }
            return curr
        }

        val cache = DoubleProgression.fromClosedInterval(0.0, 1.0, 100).map {
            it to spline[0.0, it]
        }

        fun naiveCacheProject(vec: Vector2d): Double {
            return cache.minBy { (it.second - vec).squaredNorm() }.first
        }

        var rrErr = 0.0
        var naiveErr = 0.0
        var cacheErr = 0.0
        logBenchmark(
            times,
            warmup = 20,
            "rr" to {
                val vec = Vector2d(15.0 + Math.random(), 12.0 + Math.random())
                val result = path.fastProject(vec, iterations = 10)
//                val actual = spline.project(vec, EPSILON)
//                rrErr += abs(spline.reparam(result) - actual)
            },
            "fancy" to {
                val vec = Vector2d(15.0 + Math.random(), 12.0 + Math.random())
                val result = spline.project(vec, 0.0026)
            },
            "naive" to {
                val vec = Vector2d(15.0 + Math.random(), 12.0 + Math.random())
                val result = naiveProject(vec)
//                val actual = spline.project(vec, EPSILON)
//                naiveErr += abs(result - actual)
            },
            "naive cached" to {
                val vec = Vector2d(15.0 + Math.random(), 12.0 + Math.random())
                val result = naiveCacheProject(vec)
//                val actual = spline.project(vec, EPSILON)
//                cacheErr += abs(result - actual)
            }
        )
//        println("rr err: ${rrErr / (times + 20.0)}")
//        println("naive err: ${naiveErr / (times + 20.0)}")
//        println("cache err: ${cacheErr / (times + 20.0)}")
    }

    @Test
    fun benchmarkSwerveKinematics() {
        val trackWidth = 5.0
        val wheelBase = 10.0
        val wheelPositions = listOf(
            Vector2d(wheelBase / 2, -trackWidth / 2),
            Vector2d(-wheelBase / 2, -trackWidth / 2),
            Vector2d(-wheelBase / 2, trackWidth / 2),
            Vector2d(wheelBase / 2, trackWidth / 2)
        )
        logBenchmark(
            times = 50_000,
            warmup = 500,
            "old" to {
                SwerveKinematics.moduleToRobotVelocities(
                    List(4) { (Math.random() - 0.5) * 20.0 },
                    List(4) { (Math.random() - 0.5) * PI.rad },
                    trackWidth, wheelBase
                )
            },
            "new" to {
                val wheelVelocities = List(4) { (Math.random() - 0.5) * 20.0 }
                val moduleOrientations = List(4) { (Math.random() - 0.5) * PI.rad }
//                val robotVel = Pose2d(
//                    (Math.random() - 0.5) * 20.0,(Math.random() - 0.5) * 20.0,
//                        (Math.random() - 0.5) * PI.rad * 2.0
//                )
//                val wheelVelocities = SwerveKinematics.robotToWheelVelocities(
//                    robotVel, trackWidth, wheelBase
//                )
//                val moduleOrientations = SwerveKinematics.robotToModuleOrientations(
//                    robotVel, trackWidth, wheelBase
//                )
                val new = SwerveKinematics.moduleToRobotVelocities(
                    wheelVelocities,
                    moduleOrientations,
                    wheelPositions,
                )
                val old = SwerveKinematics.moduleToRobotVelocities(
                    wheelVelocities,
                    moduleOrientations,
                    trackWidth, wheelBase
                )
            }
        )
    }

    @Test
    fun benchmarkCompleteProfile() {
        logBenchmark(3_000, 200,
            "current" to {
                val traj = TrajectoryBuilder(
                    Pose2d(), false, GenericConstraints()
                ).splineToSplineHeading(Pose2d(-10.0, 10.0, 0.deg), 180.deg)
                    .forward(10.0)
                    .splineTo(Vector2d(-40.0, -20.0), 150.deg)
                    .build()
            })
    }

    @Test
    fun testPolynomialOperations() {
        val a = Polynomial(2.0, 3.0, 5.0)
        val b = Polynomial(4.0, 1.0)

        assert(a + b == Polynomial(2.0, 7.0, 6.0))
        assert(a * b == Polynomial(8.0, 14.0, 23.0, 5.0))
        assert(a.deriv() == Polynomial(4.0, 3.0))
        assert(a[3.0] == 32.0)
    }
}