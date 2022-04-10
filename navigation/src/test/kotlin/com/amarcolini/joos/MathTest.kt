package com.amarcolini.joos

import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.wrap
import org.apache.commons.math3.util.FastMath
import org.junit.jupiter.api.Test
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
        val tests = mapOf(
            "kotlin" to { sin(Math.random() * 360) },
            "kotlin with convert" to { sin(Math.toRadians(Math.random() * 360)) },
            "kotlin with fast convert" to { sin(FastMath.toRadians(Math.random() * 360)) },
            "FastMath with convert" to { FastMath.sin(Math.toRadians(Math.random() * 360)) },
            "FastMath" to { FastMath.sin(Math.random() * 360) },
            "java" to { Math.sin(Math.random() * 360) },
            "java with convert" to { Math.sin(Math.toRadians(Math.random() * 360)) },
        )

        val clock = NanoClock.system()
        tests.map { (name, test) ->
            val start = clock.seconds()
            repeat(10000000) { test() }
            name to clock.seconds() - start
        }.sortedBy { it.second }.forEach { (name, result) ->
            println("$name: $result")
        }
    }
}