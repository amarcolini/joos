package com.amarcolini.joos.command.builders

import com.amarcolini.joos.command.*
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.hardware.drive.DriveTrajectoryFollower
import com.amarcolini.joos.hardware.drive.FollowTrajectoryCommand
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PathSegment
import com.amarcolini.joos.path.heading.*
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.TrajectorySegment
import com.amarcolini.joos.trajectory.constraints.TrajectoryAccelerationConstraint
import com.amarcolini.joos.trajectory.constraints.TrajectoryVelocityConstraint

/**
 * A convenient way to create [Command]s to follow trajectories and perform other actions simultaneously.
 *
 * @see TrajectoryBuilder
 */
class TrajectoryCommandBuilder @JvmOverloads constructor(
    private val drive: DriveTrajectoryFollower,
    private val startPose: Pose2d,
    startTangent: Angle = startPose.heading
) {
    private var builder = drive.trajectoryBuilder(startPose, startTangent)
    private var lastTrajectoryCommand: FollowTrajectoryCommand? = null
    private var currentCommandStack: SequentialCommand = SequentialCommand(Command.empty())
    private val currentMarkers = mutableListOf<MarkerCommand.Marker>()
    private val markersToCompile = mutableListOf<(Trajectory) -> MarkerCommand.Marker>()
    private val outputCommand = SequentialCommand()
    private var currentPose = startPose
    private val allPathSegments = mutableListOf<PathSegment>()

    private var currentSegmentIndex = -1
    private fun addSegment(segment: TrajectoryBuilder.() -> Unit): TrajectoryCommandBuilder {
        builder.segment()
        currentSegmentIndex++
        return this
    }

    private fun pushTrajectoryCommand(newTangent: (Angle) -> Angle = { it }) {
        val trajectory = builder.build().let {
            if (it.segments.isNotEmpty()) it else null
        }
        val newPose = trajectory?.end() ?: currentPose
        builder = drive.trajectoryBuilder(
            newPose,
            newTangent(newPose.heading)
        )
        if (trajectory != null) {
            allPathSegments += trajectory.path.segments
            currentMarkers += markersToCompile.map { it(trajectory) }
            markersToCompile.clear()
            currentPose = trajectory.end()
            currentCommandStack.add(drive.followTrajectory(trajectory).also {
                lastTrajectoryCommand = it
            })
        }
        currentSegmentIndex = -1
    }

    private fun pushCommandStack() {
        pushTrajectoryCommand()
        outputCommand.add(MarkerCommand(currentCommandStack, currentMarkers))
        currentMarkers.clear()
        currentCommandStack = SequentialCommand(Command.empty())
        lastTrajectoryCommand = null
    }

    private fun addMarker(command: Command, timeOffset: (TrajectorySegment?) -> Double) {
        val trajectoryCommand = lastTrajectoryCommand
        var targetCommandIndex = currentCommandStack.commands.lastIndex
        var segmentIndex: Int = -1
        val makeMarker = { trajectory: Trajectory? ->
            val offset = trajectory?.segments?.get(segmentIndex).let { timeOffset(it) }
            val segmentOffset = trajectory?.run {
                segments.subList(0, segmentIndex - 1).sumOf { it.duration() }
            } ?: 0.0
            object : MarkerCommand.Marker(command) {
                override fun shouldSchedule(
                    commandIndex: Int,
                    currentCommand: Command,
                    totalTime: Double,
                    relativeTime: Double
                ): Boolean = commandIndex > targetCommandIndex ||
                        (commandIndex == targetCommandIndex &&
                                relativeTime >= segmentOffset + offset)
            }
        }
        if (currentSegmentIndex >= 0) {
            segmentIndex = currentSegmentIndex
            markersToCompile += {
                makeMarker(it)
            }
        } else if (trajectoryCommand != null) {
            segmentIndex = trajectoryCommand.trajectory.segments.lastIndex
            targetCommandIndex = currentCommandStack.commands.indexOf(trajectoryCommand)
            currentMarkers += makeMarker(trajectoryCommand.trajectory)
        } else currentMarkers += makeMarker(null)
    }

    /**
     * Runs [command] in parallel [ds] units into the previous trajectory segment.
     */
    fun afterDisp(ds: Double, command: Command): TrajectoryCommandBuilder {
        addMarker(command) {
            if (it == null) return@addMarker 0.0
            Trajectory(it).reparam(ds)
        }
        return this
    }

    /**
     * Runs [runnable] in parallel [ds] units into the previous trajectory segment.
     */
    fun afterDisp(ds: Double, runnable: Runnable) = afterDisp(ds, Command.of(runnable))

    /**
     * Runs [command] in parallel when the previous trajectory segment is nearest to [pos].
     */
    fun whenAt(point: Vector2d, command: Command): TrajectoryCommandBuilder {
        addMarker(command) {
            if (it == null) return@addMarker 0.0
            val trajectory = Trajectory(it)
            trajectory.reparam(trajectory.path.project(point))
        }
        return this
    }

    /**
     * Runs [runnable] in parallel when the previous trajectory segment is nearest to [pos].
     */
    fun whenAt(point: Vector2d, runnable: Runnable) = whenAt(point, Command.of(runnable))

    /**
     * Runs [command] [dt] seconds into the previous trajectory segment or other command.
     */
    fun afterTime(dt: Double, command: Command): TrajectoryCommandBuilder {
        addMarker(command) {
            dt.coerceIn(0.0, it?.duration())
        }
        return this
    }

    /**
     * Runs [runnable] [dt] seconds into the previous trajectory segment or other command.
     */
    fun afterTime(dt: Double, runnable: Runnable) = afterTime(dt, Command.of(runnable))

    /**
     * Waits for the currently running trajectory and then runs [command].
     */
    fun stopAndThen(command: Command): TrajectoryCommandBuilder {
        pushTrajectoryCommand()
        currentCommandStack.add(command)
        return this
    }

    /**
     * Waits for the currently running trajectory and then runs [runnable].
     */
    fun stopAndThen(runnable: Runnable) = stopAndThen(Command.of(runnable))

    /**
     * Waits for the currently running trajectory and then waits [duration] seconds.
     */
    fun stopAndWait(duration: Double) = stopAndThen(WaitCommand(duration))

    /**
     * Waits for the currently running trajectory *and* any commands still running in parallel.
     */
    fun waitForAll(): TrajectoryCommandBuilder {
        pushCommandStack()
        return this
    }


    fun setTangent(angle: (Angle) -> Angle): TrajectoryCommandBuilder {
        pushTrajectoryCommand(angle)
        return this
    }

    fun setTangent(angle: Angle) = setTangent { angle }

    fun reverseTangent(): TrajectoryCommandBuilder {
        pushTrajectoryCommand { -it }
        return this
    }

    fun build(): PathCommand {
        pushCommandStack()
        return PathCommand(outputCommand, Path(allPathSegments))
    }

    @JvmOverloads
    fun turn(
        angle: Angle,
        angVelOverride: Angle? = null,
        angAccelOverride: Angle? = null,
        angJerkOverride: Angle? = null
    ) = addSegment {
        turn(angle, angVelOverride, angAccelOverride, angJerkOverride)
    }

    @JvmOverloads
    fun turnTo(
        angle: Angle,
        angVelOverride: Angle? = null,
        angAccelOverride: Angle? = null,
        angJerkOverride: Angle? = null
    ) = addSegment {
        turnTo(angle, angVelOverride, angAccelOverride, angJerkOverride)
    }

    /**
     * Adds a line segment with the specified heading interpolation.
     *
     * @param endPosition end position
     * @param headingInterpolation desired heading interpolation
     * @see HeadingInterpolation
     */
    @JvmOverloads
    fun addLine(
        endPosition: Vector2d,
        headingInterpolation: HeadingInterpolation = TangentHeading,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) =
        addSegment {
            this.addLine(
                endPosition, headingInterpolation,
                velConstraintOverride, accelConstraintOverride
            )
        }

    @JvmOverloads
    fun lineTo(
        endPosition: Vector2d,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) = addLine(endPosition, TangentHeading, velConstraintOverride, accelConstraintOverride)

    @JvmOverloads
    fun lineToConstantHeading(
        endPosition: Vector2d,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) = addLine(endPosition, ConstantHeading, velConstraintOverride, accelConstraintOverride)

    @JvmOverloads
    fun lineToLinearHeading(
        endPose: Pose2d,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) = addLine(endPose.vec(), LinearHeading(endPose.heading), velConstraintOverride, accelConstraintOverride)

    @JvmOverloads
    fun lineToSplineHeading(
        endPose: Pose2d,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) = addLine(endPose.vec(), SplineHeading(endPose.heading), velConstraintOverride, accelConstraintOverride)

    @JvmOverloads
    fun forward(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) = addSegment { forward(distance, velConstraintOverride, accelConstraintOverride) }

    @JvmOverloads
    fun back(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) = forward(-distance, velConstraintOverride, accelConstraintOverride)

    @JvmOverloads
    fun strafeLeft(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) = addSegment { strafeLeft(distance, velConstraintOverride, accelConstraintOverride) }

    @JvmOverloads
    fun strafeRight(
        distance: Double,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) = strafeLeft(-distance, velConstraintOverride, accelConstraintOverride)

    /**
     * Adds a spline segment with the specified heading interpolation.
     *
     * @param endPosition end position
     * @param endTangent end tangent
     * @param startTangentMag the magnitude of the start tangent (negative = default magnitude)
     * @param endTangentMag the magnitude of the end tangent (negative = default magnitude)
     * @param headingInterpolation desired heading interpolation
     * @see HeadingInterpolation
     */
    @JvmOverloads
    fun addSpline(
        endPosition: Vector2d,
        endTangent: Angle,
        headingInterpolation: HeadingInterpolation = TangentHeading,
        startTangentMag: Double = -1.0,
        endTangentMag: Double = -1.0,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) = addSegment {
        this.addSpline(
            endPosition, endTangent, headingInterpolation, startTangentMag, endTangentMag,
            velConstraintOverride, accelConstraintOverride
        )
    }

    @JvmOverloads
    fun splineTo(
        endPosition: Vector2d, endTangent: Angle,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) = addSpline(
        endPosition, endTangent,
        velConstraintOverride = velConstraintOverride,
        accelConstraintOverride = accelConstraintOverride
    )

    @JvmOverloads
    fun splineToConstantHeading(
        endPosition: Vector2d, endTangent: Angle,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) =
        addSpline(
            endPosition, endTangent, ConstantHeading,
            velConstraintOverride = velConstraintOverride,
            accelConstraintOverride = accelConstraintOverride
        )

    @JvmOverloads
    fun splineToLinearHeading(
        endPose: Pose2d, endTangent: Angle,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) =
        addSpline(
            endPose.vec(), endTangent, LinearHeading(endPose.heading),
            velConstraintOverride = velConstraintOverride,
            accelConstraintOverride = accelConstraintOverride
        )

    @JvmOverloads
    fun splineToSplineHeading(
        endPose: Pose2d, endTangent: Angle,
        velConstraintOverride: TrajectoryVelocityConstraint? = null,
        accelConstraintOverride: TrajectoryAccelerationConstraint? = null
    ) =
        addSpline(
            endPose.vec(), endTangent, SplineHeading(endPose.heading),
            velConstraintOverride = velConstraintOverride,
            accelConstraintOverride = accelConstraintOverride
        )
}