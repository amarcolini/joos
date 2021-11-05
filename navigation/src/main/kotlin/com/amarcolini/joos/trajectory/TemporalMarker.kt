package com.amarcolini.joos.trajectory

/**
 * Trajectory marker that is triggered when the specified time passes.
 */
data class TemporalMarker(val producer: TimeProducer, val callback: MarkerCallback)
