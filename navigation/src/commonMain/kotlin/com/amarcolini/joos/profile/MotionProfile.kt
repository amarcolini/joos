package com.amarcolini.joos.profile

import com.amarcolini.joos.util.epsilonEquals
import kotlin.js.JsExport

/**
 * Trapezoidal motion profile composed of motion segments.
 *
 * @param segments profile motion segments
 */
@JsExport
class MotionProfile(val segments: List<MotionSegment>) {
    init {
        require(segments.isNotEmpty()) { "A MotionProfile cannot be constructed without any MotionSegments." }
    }

    /**
     * Returns the [MotionState] at time [t].
     */
    operator fun get(t: Double): MotionState {
        if (t < 0.0) return segments.first().start
        var remainingTime = t
        for (segment in segments) {
            if (remainingTime <= segment.dt) {
                return segment[remainingTime]
            }
            remainingTime -= segment.dt
        }
        return segments.last().end()
    }

    /**
     * Uses a binary search to find the [MotionState] corresponding to [s]. Only works correctly
     * if position is always increasing.
     */
    fun getByDistance(s: Double): MotionState {
        var tLo = 0.0
        var tHi = duration()
        for (i in 1..50) {
            val tMid = 0.5 * (tLo + tHi)
            if (this[tMid].x > s) {
                tHi = tMid
            } else {
                tLo = tMid
            }
            if (tLo epsilonEquals tHi) break
        }
        return this[0.5 * (tLo + tHi)]
    }

    /**
     * Returns the duration of the motion profile.
     */
    fun duration() = segments.sumOf { it.dt }

    /**
     * Returns a reversed version of the motion profile.
     */
    fun reversed() = MotionProfile(segments.map { it.reversed() }.reversed())

    /**
     * Returns a flipped (negated) version of the motion profile.
     */
    fun flipped() = MotionProfile(segments.map { it.flipped() })

    /**
     * Returns the start [MotionState].
     */
    fun start() = segments.first().start

    /**
     * Returns the end [MotionState].
     */
    fun end() = segments.last().end()

    /**
     * Returns a new motion profile with [other] concatenated.
     */
    operator fun plus(other: MotionProfile): MotionProfile {
        val builder = MotionProfileBuilder(start())
        builder.appendProfile(this)
        builder.appendProfile(other)
        return builder.build()
    }

    override fun toString(): String {
        return "[${segments.joinToString(",")}]"
    }
}