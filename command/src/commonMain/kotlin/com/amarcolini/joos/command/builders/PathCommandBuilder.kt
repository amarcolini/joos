package com.amarcolini.joos.command.builders

import com.amarcolini.joos.command.*
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PathBuilder
import com.amarcolini.joos.path.PathContinuityViolationException
import com.amarcolini.joos.path.PathSegment
import com.amarcolini.joos.path.heading.*
import kotlin.jvm.JvmOverloads
import kotlin.math.min

/**
 * A convenient way to create [Command]s to follow paths and perform other actions simultaneously.
 *
 * @param allowedMarkerError Since markers for path followers are computed on the fly, this specifies how close
 * to the path the drive has to be before any markers will run.
 * @see PathBuilder
 */
class PathCommandBuilder @JvmOverloads constructor(
    private val drive: DrivePathFollower,
    private val startPose: Pose2d,
    startTangent: Angle = startPose.heading,
    var allowedMarkerError: Double = 5.0
) {
    private var builder = PathBuilder(startPose, startTangent)
    private var lastPathCommand: FollowPathCommand<*>? = null
    private var currentCommandStack: SequentialCommand = SequentialCommand(Command.empty())
    private val currentMarkers = mutableListOf<MarkerCommand.Marker>()
    private val outputCommand = SequentialCommand()
    private var currentPose = startPose
    private val allPathSegments = mutableListOf<PathSegment>()

    private fun isAtPosition(
        command: Command,
        segmentIndex: Int,
        condition: (ds: Double, pose: Pose2d) -> Boolean = { _, _ -> true }
    ): Boolean {
        val pathCommand = command as? FollowPathCommand<*>
        return pathCommand?.run {
            val segmentDisp = path.segments.subList(0, segmentIndex - 1).sumOf { it.length() }
            pathFollower.lastProjectDisplacement >= segmentDisp &&
                    pathFollower.lastError.vec().squaredNorm() <= allowedMarkerError * allowedMarkerError &&
                    condition(pathFollower.lastProjectDisplacement - segmentDisp, pathFollower.lastProjectPose)
        } ?: true
    }

    private fun tryAdd(segment: PathBuilder.() -> Unit): PathCommandBuilder {
        try {
            builder.segment()
        } catch (e: PathContinuityViolationException) {
            pushPathCommand()
            builder.segment()
        }
        return this
    }

    private fun pushPathCommand(newTangent: (Angle) -> Angle = { it }) {
        val path = builder.build().let {
            if (it.segments.isNotEmpty()) it else null
        }
        val newPose = path?.end() ?: currentPose
        builder = PathBuilder(
            newPose,
            Pose2d(newTangent(newPose.heading).vec()),
            path?.endSecondDeriv() ?: Pose2d()
        )
        if (path != null) {
            allPathSegments += path.segments
            currentPose = path.end()
            currentCommandStack.add(drive.followPath(path).also {
                lastPathCommand = it
            })
        }
    }

    private fun pushCommandStack() {
        pushPathCommand()
        outputCommand.add(MarkerCommand(currentCommandStack, currentMarkers))
        currentMarkers.clear()
        currentCommandStack = SequentialCommand(Command.empty())
        lastPathCommand = null
    }

    data class MarkerPosition(
        val commandIndex: Int,
        val segmentIndex: Int,
        val segment: PathSegment?
    )

    private fun getLastPathPosition(): MarkerPosition {
        val currentSegments = builder.preBuild().segments
        val pathCommand = lastPathCommand
        var commandIndex = currentCommandStack.commands.lastIndex
        var segmentIndex = -1
        var segment: PathSegment? = null
        if (currentSegments.isNotEmpty()) {
            segmentIndex = currentSegments.lastIndex
            segment = currentSegments[segmentIndex]
            commandIndex = currentCommandStack.commands.lastIndex
        } else if (pathCommand != null) {
            segmentIndex = pathCommand.path.segments.lastIndex
            segment = pathCommand.path.segments[segmentIndex]
            commandIndex = currentCommandStack.commands.indexOf(pathCommand)
        }
        return MarkerPosition(commandIndex, segmentIndex, segment)
    }

    /**
     * Runs [command] in parallel [ds] units into the previous path segment.
     */
    fun afterDisp(ds: Double, command: Command): PathCommandBuilder {
        val position = getLastPathPosition()
        val actualDs = position.segment?.let { ds.coerceIn(0.0, it.length()) } ?: 0.0
        currentMarkers += object : MarkerCommand.Marker(command) {
            override fun shouldSchedule(
                commandIndex: Int,
                currentCommand: Command,
                totalTime: Double,
                relativeTime: Double
            ): Boolean = commandIndex > position.commandIndex ||
                    (commandIndex == position.commandIndex &&
                            isAtPosition(currentCommand, position.segmentIndex) { ds, _ ->
                                ds >= actualDs
                            })
        }
        return this
    }

    /**
     * Runs [runnable] in parallel [ds] units into the previous path segment.
     */
    fun afterDisp(ds: Double, runnable: Runnable) = afterDisp(ds, Command.of(runnable))

    /**
     * Runs [command] in parallel when the previous path segment is nearest to [point].
     */
    fun whenAt(point: Vector2d, command: Command): PathCommandBuilder {
        val position = getLastPathPosition()
        val actualDs = position.segment?.curve?.project(point) ?: 0.0
        currentMarkers += object : MarkerCommand.Marker(command) {
            override fun shouldSchedule(
                commandIndex: Int,
                currentCommand: Command,
                totalTime: Double,
                relativeTime: Double
            ): Boolean = commandIndex > position.commandIndex ||
                    (commandIndex == position.commandIndex &&
                            isAtPosition(currentCommand, position.segmentIndex) { ds, _ ->
                                ds >= actualDs
                            })
        }
        return this
    }

    /**
     * Runs [runnable] in parallel when the previous path segment is nearest to [point].
     */
    fun whenAt(point: Vector2d, runnable: Runnable) = whenAt(point, Command.of(runnable))

    /**
     * Runs [command] [dt] seconds into the previous path segment or other command.
     */
    fun afterTime(dt: Double, command: Command): PathCommandBuilder {
        val position = getLastPathPosition()
        val actualIndex = min(position.commandIndex, currentCommandStack.commands.lastIndex)
        currentMarkers += object : MarkerCommand.Marker(WaitCommand(dt) then command) {
            override fun shouldSchedule(
                commandIndex: Int,
                currentCommand: Command,
                totalTime: Double,
                relativeTime: Double
            ): Boolean = commandIndex > actualIndex ||
                    (commandIndex == actualIndex && isAtPosition(currentCommand, position.segmentIndex))
        }
        return this
    }

    /**
     * Runs [runnable] [dt] seconds into the previous path segment or other command.
     */
    fun afterTime(dt: Double, runnable: Runnable) = afterTime(dt, Command.of(runnable))

    /**
     * Waits for the currently running path and then runs [command].
     */
    fun stopAndThen(command: Command): PathCommandBuilder {
        pushPathCommand()
        currentCommandStack.add(command)
        return this
    }

    /**
     * Waits for the currently running path and then runs [runnable].
     */
    fun stopAndThen(runnable: Runnable) = stopAndThen(Command.of(runnable))

    /**
     * Waits for the currently running path and then waits [duration] seconds.
     */
    fun stopAndWait(duration: Double) = stopAndThen(WaitCommand(duration))

    /**
     * Waits for the currently running path *and* any commands still running in parallel.
     */
    fun waitForAll(): PathCommandBuilder {
        pushCommandStack()
        return this
    }


    fun setTangent(angle: (Angle) -> Angle): PathCommandBuilder {
        pushPathCommand(angle)
        return this
    }

    fun setTangent(angle: Angle) = setTangent { angle }

    fun reverseTangent(): PathCommandBuilder {
        pushPathCommand { -it }
        return this
    }

    fun build(): PathCommand {
        pushCommandStack()
        return PathCommand(outputCommand, Path(allPathSegments))
    }

    /**
     * Adds a line segment with the specified heading interpolation.
     *
     * @param endPosition end position
     * @param headingInterpolation desired heading interpolation
     * @see HeadingInterpolation
     */
    fun addLine(endPosition: Vector2d, headingInterpolation: HeadingInterpolation = TangentHeading) =
        tryAdd {
            this.addLine(endPosition, headingInterpolation)
        }

    fun lineTo(endPosition: Vector2d) = addLine(endPosition, TangentHeading)
    fun lineToConstantHeading(endPosition: Vector2d) = addLine(endPosition, ConstantHeading)
    fun lineToLinearHeading(endPose: Pose2d) = addLine(endPose.vec(), LinearHeading(endPose.heading))
    fun lineToSplineHeading(endPose: Pose2d) = addLine(endPose.vec(), SplineHeading(endPose.heading))
    fun forward(distance: Double) = tryAdd { forward(distance) }
    fun back(distance: Double) = forward(-distance)
    fun strafeLeft(distance: Double) = tryAdd { strafeLeft(distance) }
    fun strafeRight(distance: Double) = strafeLeft(-distance)

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
    ) = tryAdd {
        this.addSpline(endPosition, endTangent, headingInterpolation, startTangentMag, endTangentMag)
    }

    fun splineTo(endPosition: Vector2d, endTangent: Angle) = addSpline(endPosition, endTangent)
    fun splineToConstantHeading(endPosition: Vector2d, endTangent: Angle) =
        addSpline(endPosition, endTangent, ConstantHeading)

    fun splineToLinearHeading(endPose: Pose2d, endTangent: Angle) =
        addSpline(endPose.vec(), endTangent, LinearHeading(endPose.heading))

    fun splineToSplineHeading(endPose: Pose2d, endTangent: Angle) =
        addSpline(endPose.vec(), endTangent, SplineHeading(endPose.heading))
}