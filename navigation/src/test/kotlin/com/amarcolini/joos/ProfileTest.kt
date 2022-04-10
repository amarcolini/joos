package com.amarcolini.joos

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.PathBuilder
import com.amarcolini.joos.profile.MotionProfileGenerator
import com.amarcolini.joos.profile.MotionState
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.TrajectoryGenerator
import com.amarcolini.joos.trajectory.config.GenericConstraints
import com.amarcolini.joos.util.DoubleProgression
import com.amarcolini.joos.util.deg
import org.junit.jupiter.api.Test

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
        val constraints = GenericConstraints()
        val path = PathBuilder(Pose2d())
            .splineTo(Vector2d(30.0, 0.0), Math.toRadians(160.0))
            .build()
        val profile = TrajectoryGenerator.generatePathTrajectorySegment(
            path,
            constraints.velConstraint, constraints.accelConstraint
        ).profile
        GraphUtil.saveProfile("Complete Profile", profile)
    }

    @Test
    fun testAngularAcceleration() {
        val trajectory = TrajectoryBuilder(
            startPose = Pose2d(),
            constraints = GenericConstraints(maxAngAccel = 180.deg),
            resolution = 0.25
        )
            .splineTo(Vector2d(30.0, -30.0), 0.0)
            .build()
        val progression = DoubleProgression.fromClosedInterval(
            0.0, trajectory.duration(),
            (trajectory.duration() / 0.01).toInt()
        )
        val max = progression.map { trajectory.acceleration(it).heading }.maxByOrNull { it.radians }
        println("max: $max vs ${Math.toRadians(180.0)}")
        if (max != null) {
            assert(max < 180.deg)
        }
    }
}