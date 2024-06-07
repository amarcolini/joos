package com.amarcolini.joos.followers

import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.abs
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.math.abs

/**
 * Generic [Path] follower for time-independent pose reference tracking.
 *
 * @param clock clock
 */
@JsExport
abstract class PathFollower @JvmOverloads constructor(
    protected val admissibleError: Pose2d,
    protected val clock: NanoClock = NanoClock.system
) {
    private var startTimestamp: Double = Double.NaN
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
        this.lastProjectDisplacement = 0.0
    }

    /**
     * Returns false if the current path has finished executing.
     */
    fun isFollowing(): Boolean {
        return !admissible
    }

    fun elapsedTime() = clock.seconds() - startTimestamp

    var lastProjectDisplacement = 0.0
        private set
    var lastProjectPose = Pose2d()
        private set

    /**
     * Run a single iteration of the path follower.
     *
     * @param currentPose current field frame pose
     * @param currentRobotVel current robot frame velocity
     */
    @JvmOverloads
    fun update(currentPose: Pose2d, currentRobotVel: Pose2d? = null): DriveSignal {
        val pathEndError = path.end() - currentPose
        val projectedDisplacement = path.fastProject(currentPose.vec(), lastProjectDisplacement)
        val projectedPose = path[projectedDisplacement]
        lastProjectPose = projectedPose
        lastProjectDisplacement = projectedDisplacement
        admissible = abs(pathEndError.x) < admissibleError.x &&
                abs(pathEndError.y) < admissibleError.y &&
                abs(pathEndError.heading.normDelta()) < admissibleError.heading
        return if (isFollowing()) {
            internalUpdate(currentPose, currentRobotVel, projectedPose, projectedDisplacement)
        } else {
            DriveSignal()
        }
    }

    protected abstract fun internalUpdate(
        currentPose: Pose2d,
        currentRobotVel: Pose2d?,
        projectedPose: Pose2d,
        projectedDisplacement: Double
    ): DriveSignal
}