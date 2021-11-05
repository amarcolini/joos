package com.amarcolini.joos.trajectory

/**
 * Trajectory marker that is triggered when the specified time passes.
 */
data class TrajectoryMarker(val time: Double, val callback: MarkerCallback)
