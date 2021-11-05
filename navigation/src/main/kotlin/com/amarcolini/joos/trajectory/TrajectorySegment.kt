package com.amarcolini.joos.trajectory

import com.amarcolini.joos.geometry.Pose2d

/**
 * Generic trajectory segment.
 */
interface TrajectorySegment {
    fun duration(): Double

    fun length(): Double

    operator fun get(time: Double): Pose2d

    fun distance(time: Double): Double

    fun deriv(time: Double): Pose2d

    fun secondDeriv(time: Double): Pose2d

    fun velocity(time: Double): Pose2d

    fun acceleration(time: Double): Pose2d

    fun start(): Pose2d

    fun end(): Pose2d
}