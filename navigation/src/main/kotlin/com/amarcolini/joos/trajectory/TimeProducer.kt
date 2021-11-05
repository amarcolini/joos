package com.amarcolini.joos.trajectory

/**
 * SAM interface that produces a marker absolute time offset from the trajectory duration.
 */
fun interface TimeProducer {
    fun produce(duration: Double): Double
}
