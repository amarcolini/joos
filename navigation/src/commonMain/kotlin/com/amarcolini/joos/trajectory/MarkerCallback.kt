package com.amarcolini.joos.trajectory

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * SAM interface for marker callbacks.
 */
@JsExport
fun interface MarkerCallback {
    fun onMarkerReached()
}