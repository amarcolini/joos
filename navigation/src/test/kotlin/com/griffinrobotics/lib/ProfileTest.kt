package com.griffinrobotics.lib

import com.griffinrobotics.lib.profile.MotionProfileGenerator
import com.griffinrobotics.lib.profile.MotionState
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
}