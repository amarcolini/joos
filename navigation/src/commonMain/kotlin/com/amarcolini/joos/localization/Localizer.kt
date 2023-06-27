package com.amarcolini.joos.localization

import com.amarcolini.joos.geometry.Pose2d
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Generic interface for estimating robot pose over time.
 */
@JsExport
interface Localizer {

    /**
     * Current robot pose estimate.
     */
    var poseEstimate: Pose2d

    /**
     * Current robot pose velocity (optional)
     */
    val poseVelocity: Pose2d?

    /**
     * Completes a single localization update.
     */
    fun update()
}