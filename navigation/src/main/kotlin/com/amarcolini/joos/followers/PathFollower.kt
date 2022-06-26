package com.amarcolini.joos.followers

import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.abs
import kotlin.math.abs

/**
 * Generic [Path] follower for time-independent pose reference tracking.
 *
 * @param clock clock
 */
abstract class PathFollower @JvmOverloads constructor(
    private val admissibleError: Pose2d,
    protected val clock: NanoClock = NanoClock.system()
) {
    private var startTimestamp: Double = 0.0
    private var admissible = true

    /**
     * Path being followed if [isFollowing] is true.
     */
    lateinit var path: Path
        protected set

    /**
     * Robot pose error computed in the last [update] call.
     */
    abstract var lastError: Pose2d
        protected set

    /**
     * Follow the given [path].
     *
     * @param path path
     * @param
     */
    open fun followPath(path: Path) {
        this.startTimestamp = clock.seconds()
        this.path = path
        this.admissible = false
    }

    /**
     * Returns false if the current path has finished executing.
     */
    fun isFollowing(): Boolean {
        return !admissible
    }

    /**
     * Run a single iteration of the path follower.
     *
     * @param currentPose current robot pose
     */
    fun update(currentPose: Pose2d): DriveSignal {
        val pathEndError = path.end() - currentPose
        admissible = abs(pathEndError.x) < admissibleError.x &&
                abs(pathEndError.y) < admissibleError.y &&
                abs(pathEndError.heading.normDelta()) < admissibleError.heading
        return if (isFollowing()) {
            internalUpdate(currentPose)
        } else {
            DriveSignal()
        }
    }

    protected abstract fun internalUpdate(currentPose: Pose2d): DriveSignal
}