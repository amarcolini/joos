package com.amarcolini.joos.followers

import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.util.NanoClock
import com.amarcolini.joos.util.abs
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.math.abs

/**
 * Generic [Trajectory] follower for time-based pose reference tracking.
 *
 * @param admissibleError admissible/satisfactory pose error at the end of each move
 * @param timeout max time to wait for the error to be admissible
 * @param clock clock
 */
@JsExport
abstract class TrajectoryFollower @JvmOverloads constructor(
    private val admissibleError: Pose2d = Pose2d(),
    private val timeout: Double = 0.0,
    protected val clock: NanoClock = NanoClock.system
) {
    private var startTimestamp: Double = Double.NaN
    private var admissible = false
    private var executedFinalUpdate = false

    /**
     * Trajectory being followed if [isFollowing] is true.
     */
    lateinit var trajectory: Trajectory
        protected set

    /**
     * Robot pose error computed in the last [update] call.
     */
    abstract var lastError: Pose2d
        protected set

    /**
     * Follow the given [trajectory].
     */
    open fun followTrajectory(trajectory: Trajectory) {
        this.startTimestamp = clock.seconds()
        this.trajectory = trajectory
        this.admissible = false
        executedFinalUpdate = false
    }

    private fun internalIsFollowing(): Boolean {
        val timeRemaining = trajectory.duration() - elapsedTime()
        return timeRemaining > 0 || (!admissible && timeRemaining > -timeout)
    }

    /**
     * Returns true if the current trajectory is currently executing.
     */
    fun isFollowing() =
        ::trajectory.isInitialized && (!executedFinalUpdate || internalIsFollowing())

    /**
     * Returns the elapsed time since the last [followTrajectory] call.
     */
    fun elapsedTime() = clock.seconds() - startTimestamp

    /**
     * Run a single iteration of the trajectory follower.
     *
     * @param currentPose current field frame pose
     * @param currentRobotVel current robot frame velocity
     */
    @JvmOverloads
    fun update(currentPose: Pose2d, currentRobotVel: Pose2d? = null): DriveSignal {
        val trajEndError = trajectory.end() - currentPose
        admissible = abs(trajEndError.x) < admissibleError.x &&
                abs(trajEndError.y) < admissibleError.y &&
                abs(trajEndError.heading.normDelta()) < admissibleError.heading
        return if (internalIsFollowing() || executedFinalUpdate) {
            internalUpdate(currentPose, currentRobotVel)
        } else {
            executedFinalUpdate = true
            DriveSignal()
        }
    }

    protected abstract fun internalUpdate(
        currentPose: Pose2d,
        currentRobotVel: Pose2d?
    ): DriveSignal
}