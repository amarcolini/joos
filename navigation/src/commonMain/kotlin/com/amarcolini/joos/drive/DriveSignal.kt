package com.amarcolini.joos.drive

import com.amarcolini.joos.geometry.Pose2d
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads

/**
 * Signal indicating the commanded kinematic state of a drive.
 * @param vel robot frame velocity
 * @param accel robot frame acceleration
 */
@JsExport
data class DriveSignal @JvmOverloads constructor(
    val vel: Pose2d = Pose2d(),
    val accel: Pose2d = Pose2d()
)