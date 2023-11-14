package com.amarcolini.joos

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.PathBuilder
import com.amarcolini.joos.profile.MotionProfileGenerator
import com.amarcolini.joos.profile.MotionState
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.TrajectoryGenerator
import com.amarcolini.joos.trajectory.constraints.*
import com.amarcolini.joos.util.DoubleProgression
import com.amarcolini.joos.util.abs
import com.amarcolini.joos.util.deg
import org.junit.jupiter.api.Test
import org.knowm.xchart.QuickChart
import org.knowm.xchart.style.theme.MatlabTheme
import kotlin.math.PI

class ProfileTest {
    @Test
    fun testSimpleProfile() {
        GraphUtil.saveProfile(
            "Simple Profile", MotionProfileGenerator.generateSimpleMotionProfile(
                MotionState(0.0, 0.0),
                MotionState(60.0, 0.0),
                30.0,
                30.0
            )
        )
    }

    @Test
    fun testCompleteProfile() {
        val path = PathBuilder(Pose2d())
            .splineTo(Vector2d(24.0, 24.0), 0.0)
            .build()
        val velConstraint = MinVelocityConstraint(
            MecanumVelocityConstraint(312.0 * 60 * (4 * PI * 0.94), 12.05, 12.05, 1.11),
            TranslationalVelocityConstraint(45.0),
            AngularVelocityConstraint(260.deg),
            AngularAccelVelocityConstraint(360.deg, 30.0)
        )
        val accelConstraint = MinAccelerationConstraint(
            TranslationalAccelerationConstraint(30.0),
            AngularAccelerationConstraint(360.deg)
        )
        val trajectory = TrajectoryGenerator.generatePathTrajectorySegment(
            path,
            velConstraint, accelConstraint
        )
        GraphUtil.saveProfile("Complete Profile", trajectory.profile)
        val temporalData = DoubleProgression.fromClosedInterval(
            0.0,
            trajectory.path.length(),
            1000
        )
        val cData = temporalData.map { trajectory.path.curvature(it) }.toDoubleArray()
        val graph = QuickChart.getChart(
            "Hello",
            "t",
            "",
            arrayOf("c"),
            temporalData.toList().toDoubleArray(),
            arrayOf(cData)
        )
        graph.styler.isLegendVisible = false
        graph.styler.theme = MatlabTheme()

        GraphUtil.saveGraph("Hello", graph)
    }

    @Test
    fun testAngularAcceleration() {
        val maxAngAccel = 18.deg
        val trajectory = TrajectoryBuilder(
            startPose = Pose2d(),
            constraints = MecanumConstraints(30.0, 16.0, 16.0, 0.7, maxAngAccel = maxAngAccel),
            resolution = 0.25
        )
            .splineTo(Vector2d(24.0, 24.0), 0.deg)
            .forward(24.0)
            .build()
        val progression = DoubleProgression.fromClosedInterval(
            0.0, trajectory.duration(),
            (trajectory.duration() / 0.1).toInt()
        )
        val max = progression.map { trajectory.acceleration(it).heading }.maxOf { abs(it) }
        val f = { x: Double -> trajectory.velocity(x).heading }
        val accels =
            progression.zipWithNext { a, b -> (f(b) - f(a)).normDelta() / progression.step }
        val max2 = accels.maxByOrNull { abs(it) }
        val index = accels.indexOf(max2)
        println(index)
        println("${f(progression[index])}, ${f(progression[index + 1])}")
        println(progression.step)
        println("${(f(progression[index]) - f(progression[index + 1])).normDelta() / progression.step}")
        println("max: $max and $max2 when $maxAngAccel")
    }
}