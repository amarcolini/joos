package com.amarcolini.joos

import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.followers.TankPIDVAFollower
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.WaitSegment
import org.junit.jupiter.api.Test

class FollowerTest {
    @Test
    fun testIsFollowing() {
        val follower = TankPIDVAFollower(PIDCoefficients(), PIDCoefficients())
        assert(!follower.isFollowing())
        follower.followTrajectory(Trajectory(WaitSegment(Pose2d(), 0.0)))
        assert(follower.isFollowing())
    }
}