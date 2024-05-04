package com.amarcolini.joos

import com.amarcolini.joos.LetsPlotUtil.createPathLayer
import com.amarcolini.joos.LetsPlotUtil.plotVectorField
import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.followers.GVFFollower
import com.amarcolini.joos.followers.HolonomicGVFFollower
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.path.*
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.constraints.GenericConstraints
import com.amarcolini.joos.util.*
import com.amarcolini.joos.util.PositionPathGVF.Companion.createRecticircle
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomPath
import org.jetbrains.letsPlot.letsPlot
import org.junit.jupiter.api.Test
import utils.benchmark.logBenchmark
import kotlin.math.PI
import kotlin.math.sign

class GVFTest {
    @Test
    fun testPathGVF() {
        val path = PathBuilder(Pose2d())
            .splineToSplineHeading(Pose2d(20.0, 20.0, 0.deg), 0.rad)
            .build()
        val gvf = PathGVF(path, 1.0) { it }

        val simulationData = arrayListOf(
            arrayListOf<Double>(),
            arrayListOf(),
            arrayListOf()
        )
        var currentPose = Pose2d(5.0, 6.0, (-45).deg)
        val deltaTime = 0.01
        var seconds = 0.0
        val clock = object : NanoClock {
            override fun seconds(): Double = seconds
        }
        val follower = GVFFollower(
            50.0, 40.0, 30.0, 180.deg, 180.deg, Pose2d(0.5, 0.5, 5.deg),
            0.5, 2.0, PIDCoefficients(4.0), clock = clock,
        )
        follower.followGVF(gvf)
        var currentStep = 1
        while (follower.isFollowing() && currentStep < 200) {
            seconds += deltaTime
            val targetVel = follower.update(currentPose).vel
            currentPose =
                Kinematics.relativeOdometryUpdate(currentPose, targetVel * deltaTime)
            simulationData[0].add(currentPose.x)
            simulationData[1].add(currentPose.y)
            simulationData[2].add(targetVel.x)
            currentStep++
        }

        val fig = plotVectorField(gvf, Vector2d(-2.0, -2.0), Vector2d(25.0, 25.0), 1.0) +
                createPathLayer(path) +
                geomPath(
                    data = mapOf(
                        "x" to simulationData[0],
                        "y" to simulationData[1],
                        "magnitude" to simulationData[2]
                    ),
                    color = "red",
                    size = 1.0
                ) { x = "x"; y = "y"; color = "magnitude" }
        ggsave(fig, "path.png")
    }

    @Test
    fun testCircularGVF() {
        val gvf = CircularGVF(
            Vector2d(), 10.0,
            0.1
        ) { if (it > 0) 100.0 else it }
        val plot = plotVectorField(gvf, Vector2d(-20.0, -20.0), Vector2d(20.0, 20.0))
        ggsave(plot, "circle.png")
    }

    @Test
    fun testCompositeGVF() {
        val kN = 1.0
        val path = PathGVF(
            PathBuilder(Pose2d())
                .splineToSplineHeading(Pose2d(30.0, 30.0, 90.deg), 0.rad)
                .build(),
            kN
        )
        val obstacleRadius = 2.0
        val avoidanceDistance = 10.0
        val robotRadius = Vector2d(18.0, 16.0).norm() * 0.5
        val circle = CircularGVF(Vector2d(15.0, 15.0), obstacleRadius + robotRadius + avoidanceDistance, kN)
        val l = 0.1
        val gvf = CompositeGVF(
            path,
            listOf(
                GVFObstacle(circle, avoidanceDistance, {
                    GVFObstacle.defaultMapFunction(it, l, l)
                }, {
                    GVFObstacle.defaultMapFunction(it, l, l)
                })
            )
        )

        val simulationData = arrayListOf(
            arrayListOf<Double>(),
            arrayListOf(),
            arrayListOf()
        )
        var currentPose = Pose2d(0.0, 0.0, 0.deg)
        var currentVel = Pose2d()
        val deltaTime = 0.01
        var seconds = 0.0
        val clock = object : NanoClock {
            override fun seconds(): Double = seconds
        }
        val kV = 0.25
        val maxVel = 60.0
        val maxAccel = 40.0
        val follower = HolonomicGVFFollower(
            maxVel * 0.8, maxAccel * 0.8, maxAccel * 0.6, 360.deg, 360.deg, Pose2d(0.5, 0.5, 5.deg),
            kN, 40.0, kV, kV, PIDCoefficients(8.0), clock = clock, useCurvatureControl = false
        )
        follower.followGVF(gvf)
        var currentStep = 1
        while (follower.isFollowing() && currentStep < 500) {
            seconds += deltaTime
            val targetVel = follower.update(currentPose).vel
            val diff = (targetVel - currentVel)
            val newXVel = currentVel.x + diff.x.sign * maxAccel * deltaTime
            val newYVel = currentVel.y + diff.y.sign * maxAccel * deltaTime
//            val newAngVel =
//                currentVel.heading + sign((targetVel.heading - currentVel.heading)) * follower.maxAngAccel * deltaTime
            val newAngVel = targetVel.heading
            currentVel = Pose2d(
                newXVel.coerceIn(-maxVel, maxVel),
                newYVel.coerceIn(-maxVel, maxVel),
                newAngVel.coerceIn(-follower.maxAngVel, follower.maxAngVel)
            )
            currentPose =
                Kinematics.relativeOdometryUpdate(currentPose, currentVel * deltaTime)
            simulationData[0].add(currentPose.x)
            simulationData[1].add(currentPose.y)
            simulationData[2].add(currentVel.vec().norm())
            currentStep++
        }

//        gvf.reset()
        val plot = plotVectorField(gvf, Vector2d(0.0, -10.0), Vector2d(30.0, 30.0)) +
                createPathLayer(path.path) + createPathLayer(
            PositionPath(CircularArc(circle.center, circle.radius, 0.rad, Angle.circle))
        ) + createPathLayer(
            PositionPath(
                CircularArc(
                    circle.center,
                    circle.radius - avoidanceDistance,
                    0.rad,
                    Angle.circle
                )
            ), "blue"
        ) + geomPath(
            data = mapOf(
                "x" to simulationData[0],
                "y" to simulationData[1],
                "color" to simulationData[2]
            ),
//            color = "red",
            size = 1.0
        ) { x = "x"; y = "y"; color = "color" }
        ggsave(plot, "composite.png")
    }

    @Test
    fun testComplexCompositeGVF() {
        val path = PathGVF(
            PathBuilder(Pose2d())
                .splineTo(Vector2d(30.0, 30.0), 0.rad)
                .build(),
            1.0
        )
        val inset = 6.0
        val center = Vector2d(15.0, 15.0)
        val dimensions = Vector2d(10.0, 5.0)
        val radius = 5.0
        val obstaclePath = createRecticircle(
            center, dimensions, radius
        )
        val obstacle = GVFObstacle(
            PositionPathGVF(obstaclePath, 0.5), inset,
        )
        val gvf = CompositeGVF(path, listOf(obstacle))

        val simulationData = arrayListOf(
            arrayListOf<Double>(),
            arrayListOf(),
            arrayListOf()
        )
        var currentPose = Pose2d(5.0, -5.0, 45.deg)
        val deltaTime = 0.01
        var seconds = 0.0
        val clock = object : NanoClock {
            override fun seconds(): Double = seconds
        }
        val follower = GVFFollower(
            50.0, 40.0, 30.0, 180.deg, 180.deg, Pose2d(0.5, 0.5, 5.deg),
            0.3, 1.0, PIDCoefficients(4.0), clock = clock,
        )
        follower.followGVF(gvf)
        var currentStep = 1
        while (follower.isFollowing() && currentStep < 200) {
            seconds += deltaTime
            val targetVel = follower.update(currentPose).vel
            currentPose =
                Kinematics.relativeOdometryUpdate(currentPose, targetVel * deltaTime)
            simulationData[0].add(currentPose.x)
            simulationData[1].add(currentPose.y)
            simulationData[2].add(targetVel.x)
            currentStep++
        }

        val plot = plotVectorField(gvf, Vector2d(0.0, -10.0), Vector2d(30.0, 30.0)) +
                createPathLayer(path.path) + createPathLayer(
            obstaclePath
        ) + createPathLayer(
            createRecticircle(
                center,
                dimensions - Vector2d(inset, inset),
                radius * (1 - inset / dimensions.x)
            ), "blue"
        ) + geomPath(
            data = mapOf(
                "x" to simulationData[0],
                "y" to simulationData[1],
                "color" to simulationData[2]
            ),
            color = "red",
            size = 1.0
        ) { x = "x"; y = "y"; color = "color" }
        ggsave(plot, "complex_composite.png")
    }

    @Test
    fun testIntersectingObstacles() {
        val kN = 1.0
        val path = PathGVF(
            PathBuilder(Pose2d())
                .splineToSplineHeading(Pose2d(30.0, 30.0, 90.deg), 0.rad)
                .build(),
            kN
        )
        val obstacleRadius = 1.0
        val avoidanceDistance = 10.0
        val robotRadius = Vector2d(18.0, 16.0).norm() * 0.5
        val circleCenters = listOf(
            Vector2d(0.0, 10.0),
            Vector2d(30.0, 10.0),
        )
        val circles = circleCenters.map {
            CircularGVF(it, obstacleRadius + robotRadius + avoidanceDistance, 3.0 / (obstacleRadius + robotRadius + avoidanceDistance))
        }
        val l = 1.0
        val obstacles = circles.map { circle ->
            GVFObstacle(circle, avoidanceDistance, {
                GVFObstacle.defaultMapFunction(it, l, l)
            }, {
                GVFObstacle.defaultMapFunction(it, l, l)
            })
        }
        val gvf = CompositeGVF(
            path,
            obstacles
        )

        val simulationData = arrayListOf(
            arrayListOf<Double>(),
            arrayListOf(),
            arrayListOf()
        )
        var currentPose = Pose2d(0.0, 0.0, 0.deg)
        var currentVel = Pose2d()
        val deltaTime = 0.01
        var seconds = 0.0
        val clock = object : NanoClock {
            override fun seconds(): Double = seconds
        }
        val kV = 0.0
        val maxVel = 60.0
        val maxAccel = 40.0
        val follower = HolonomicGVFFollower(
            maxVel * 0.8, maxAccel * 0.8, maxAccel * 0.6, 360.deg, 360.deg, Pose2d(0.5, 0.5, 5.deg),
            kN, 0.0, kV, kV, PIDCoefficients(8.0), clock = clock, useCurvatureControl = false
        )
        follower.followGVF(gvf)
        var currentStep = 1
        while (follower.isFollowing() && currentStep < 500) {
            seconds += deltaTime
            val targetVel = follower.update(currentPose).vel
            val diff = (targetVel - currentVel)
//            val newXVel = currentVel.x + diff.x.sign * maxAccel * deltaTime
//            val newYVel = currentVel.y + diff.y.sign * maxAccel * deltaTime
            val newXVel = targetVel.x
            val newYVel = targetVel.y
//            val newAngVel =
//                currentVel.heading + sign((targetVel.heading - currentVel.heading)) * follower.maxAngAccel * deltaTime
            val newAngVel = targetVel.heading
            currentVel = Pose2d(
                newXVel.coerceIn(-maxVel, maxVel),
                newYVel.coerceIn(-maxVel, maxVel),
                newAngVel.coerceIn(-follower.maxAngVel, follower.maxAngVel)
            )
            currentPose =
                Kinematics.relativeOdometryUpdate(currentPose, currentVel * deltaTime)
            simulationData[0].add(currentPose.x)
            simulationData[1].add(currentPose.y)
            simulationData[2].add(currentVel.vec().norm())
            currentStep++
        }

        var plot = plotVectorField(gvf, Vector2d(-10.0, -10.0), Vector2d(30.0, 30.0)) +
                createPathLayer(path.path)
        circles.zip(obstacles).forEach { (circle, obstacle) ->
            plot += createPathLayer(
                PositionPath(CircularArc(circle.center, circle.radius, 0.rad, Angle.circle))
            ) + createPathLayer(
                PositionPath(
                    CircularArc(
                        circle.center,
                        circle.radius - obstacle.insetDistance,
                        0.rad,
                        Angle.circle
                    )
                ), "blue"
            )
        }
        plot += geomPath(
            data = mapOf(
                "x" to simulationData[0],
                "y" to simulationData[1],
                "color" to simulationData[2]
            ),
            color = "red",
            size = 1.0
        ) { x = "x"; y = "y"; color = "color" }
        ggsave(plot, "intersecting_obstacles.png")
    }

    @Test
    fun testCircularArc() {
        val gvf = PositionPathGVF(
            PositionPath(CircularArc(Vector2d(5.0, 0.0), 10.0, 0.rad, 2.0 * PI.rad)), 1.0
        )
        val plot = plotVectorField(
            gvf,
            Vector2d(-20.0, -20.0),
            Vector2d(20.0, 20.0)
        ) + createPathLayer(gvf.path)
        ggsave(plot, "circulararc.png")
    }

    @Test
    fun testExtrema() {
        val path = PathBuilder(Pose2d())
            .splineTo(Vector2d(20.0, 30.0), 135.deg)
            .build()
        var plot = letsPlot() + LetsPlotUtil.createPathLayer(path)
        val curve = (path.segments[0].curve as QuinticSpline)
        val extrema = curve.maxCurvatures(0.01)

        logBenchmark(1_000, 20, "basic" to {
            val path2 = PathBuilder(Pose2d())
                .splineTo(Vector2d(20.0, 30.0), 135.deg)
                .preBuild()
//            val curve2 = (path.segments[0].curve as QuinticSpline)
//            val extrema2 = curve2.maxCurvatures(0.01)
        }, "original" to {
            val traj = TrajectoryBuilder(Pose2d(), false, GenericConstraints())
                .splineTo(Vector2d(20.0, 30.0), 135.deg)
                .build()
        })
        println(curve.preCurvature.coeffs.toList())
        println(extrema.map { it to curve.curvature(0.0, it)})
        println(curve.x.dcoeffs.toList())
        println(curve.y.dcoeffs.toList())
        println(curve.x.d2coeffs.toList())
        println(curve.y.d2coeffs.toList())
        extrema.map {
            val point = curve.internalGet(it)
            val deriv = curve.deriv(0.0, it).run { Vector2d(-y, x) } * 2.0
            plot += createPathLayer("red", listOf(point + deriv, point - deriv))
        }
        ggsave(plot, "curvatureMax.png")
    }

    @Test
    fun testRecticircle() {
//        val path = PositionPathBuilder(Vector2d(), 0.deg)
//            .forward(20.0)
//            .turnLeft(90.deg, 5.0)
//            .forward(10.0)
//            .turnLeft(90.deg, 5.0)
//            .forward(20.0)
//            .turnLeft(90.deg, 5.0)
//            .forward(10.0)
//            .turnLeft(90.deg, 5.0)
//            .preBuild()
        val center = Vector2d(10.0, 10.0)
        val radii = Vector2d(10.0, 5.0)
        val radius = 5.0
        val path = createRecticircle(center, radii * 2.0, radius)
        val gvf = PositionPathGVF(
            path, 1.0
        )
        val plot = plotVectorField(
            gvf,
            Vector2d(-10.0, -10.0),
            Vector2d(30.0, 30.0)
        ) + createPathLayer(gvf.path)
        ggsave(plot, "recticircle.png")
    }

    @Test
    fun benchmarkRecticircle() {
        fun stressTest(vf: VectorField) {
            repeat(1) {
                vf[Math.random() * 100, Math.random() * 100]
            }
        }
        logBenchmark(
            times = 10_000,
            warmup = 20,
            logger = ::println,
            "spline" to {
                val path = PathBuilder(Pose2d(Math.random()))
                    .splineTo(Vector2d(20.0 + Math.random(), 10.0), 0.rad)
                    .build()
                val gvf = PathGVF(path, 1.0) { it }
                stressTest(gvf)
            },
            "naive recticircle" to {
                val path = PositionPathBuilder(Vector2d(Math.random()), 0.deg)
                    .forward(20.0)
                    .turnLeft(90.deg, 5.0)
                    .forward(10.0)
                    .turnLeft(90.deg, 5.0)
                    .forward(20.0)
                    .turnLeft(90.deg, 5.0)
                    .forward(10.0)
                    .turnLeft(90.deg, 5.0)
                    .preBuild()
                val gvf = PositionPathGVF(
                    path, 1.0
                )
                stressTest(gvf)
            },
            "optimized recticircle" to {
                val center = Vector2d(10.0 + Math.random(), 10.0)
                val radii = Vector2d(10.0, 5.0 + Math.random())
                val radius = 5.0
                val path = createRecticircle(center, radii * 2.0, radius)
                val gvf = PositionPathGVF(
                    path, 1.0
                )
                stressTest(gvf)
            },
            "arc circle" to {
                val gvf = PositionPathGVF(
                    PositionPath(
                        CircularArc(
                            Vector2d(5.0 + Math.random(), 0.0),
                            10.0 + Math.random(),
                            0.rad,
                            2.0 * PI.rad
                        )
                    ), 1.0
                )
                stressTest(gvf)
            },
            "pure circle" to {
                val gvf = CircularGVF(
                    Vector2d(Math.random()), 10.0 + Math.random(),
                    1.0
                )
                stressTest(gvf)
            }
        )
    }

    @Test
    fun benchmarkCompositeGVF() {
        val kN = 1.0
        val path = PathGVF(
            PathBuilder(Pose2d())
                .splineToSplineHeading(Pose2d(30.0, 30.0, 90.deg), 0.rad)
                .build(),
            kN
        )
        val obstacleRadius = 2.0
        val avoidanceDistance = 5.0
        val robotRadius = Vector2d(18.0, 16.0).norm() * 0.5
        val circle = CircularGVF(Vector2d(15.0, 15.0), obstacleRadius + robotRadius + avoidanceDistance, kN)
        val l = 0.1
        val gvf = CompositeGVF(
            path,
            listOf(
                GVFObstacle(circle, avoidanceDistance, {
                    GVFObstacle.defaultMapFunction(it, l, l)
                }, {
                    GVFObstacle.defaultMapFunction(it, l, l)
                })
            )
        )
        val kV = 0.25
        val maxVel = 60.0
        val maxAccel = 40.0
        val follower = HolonomicGVFFollower(
            maxVel * 0.8, maxAccel * 0.8, maxAccel * 0.6, 360.deg, 360.deg, Pose2d(0.5, 0.5, 5.deg),
            kN, 40.0, kV, kV, PIDCoefficients(8.0), clock = NanoClock.system, useCurvatureControl = false
        )
        follower.followGVF(gvf)
        logBenchmark(
            times = 10_000,
            warmup = 0,
            "compositeGVF" to {
                val pos = Vector2d(
                    Math.random() * 100,
                    Math.random() * 100
                )
                follower.update(Pose2d(pos))
            },
        )
    }
}