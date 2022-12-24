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
import kotlin.math.PI
import kotlin.math.abs

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
        val profile = TrajectoryGenerator.generatePathTrajectorySegment(
            path,
            velConstraint, accelConstraint
        ).profile
        GraphUtil.saveProfile("Complete Profile", profile)
    }

    @Test
    fun testAngularAcceleration() {
        val maxAngAccel = 1.deg
        val trajectory = TrajectoryBuilder(
            startPose = Pose2d(),
            constraints = GenericConstraints(maxAngAccel = maxAngAccel),
            resolution = 0.25
        )
            .splineTo(Vector2d(30.0, -30.0), 0.0)
            .build()
        val progression = DoubleProgression.fromClosedInterval(
            0.0, trajectory.duration(),
            (trajectory.duration() / 0.01).toInt()
        )
        val max = progression.map { trajectory.acceleration(it).heading }.maxOf { abs(it) }
        println("max: $max vs $maxAngAccel")
        assert(max <= maxAngAccel)
    }
}