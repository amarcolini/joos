package field

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PathBuilder
import com.amarcolini.joos.path.PathContinuityViolationException
import com.amarcolini.joos.path.heading.*
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.TrajectoryBuilder
import com.amarcolini.joos.trajectory.constraints.GenericConstraints
import com.amarcolini.joos.util.deg

const val tile = 24.0

enum class PropLocation {
    Left, Center, Right
}

fun getTrajectoryBuilder(startPose: Pose2d): TrajectoryBuilder {
    return TrajectoryBuilder(startPose, false, GenericConstraints())
}

object BlueFarAuto {
    private var startPose = Pose2d(-2 * tile + 8.0, 3 * tile - 9.0, (-90).deg)
    private var rightPlopPose = Pose2d(-39.5, 32.0, (-180).deg)
    private var centerPlopPose = Pose2d(-49.0, 28.0, (0).deg)
    private var leftPlopPose = Pose2d(-34.0, 35.0, (0).deg)
    private var rightPlacePose = Pose2d(47.5, 34.0, 5.deg)
    private var centerPlacePose = Pose2d(47.5, 35.0, 0.deg)
    private var leftPlacePose = Pose2d(47.5, 43.0, 0.deg)
    private var regularExitPose = Pose2d(-40.0, 13.0, 0.deg)
    private var centerExitPose = Pose2d(-50.0, 13.0, 0.deg)
    private var stackPose = Pose2d(-60.0, 17.0, 0.deg)
    private var crossPose = Pose2d(30.0, 13.0, 0.deg)

    private var stackHigh = 0.52

    fun getPath(propLocation: PropLocation): Trajectory {
        val (plopPose, placePose) = when (propLocation) {
            PropLocation.Left -> leftPlopPose to leftPlacePose
            PropLocation.Center -> centerPlopPose to centerPlacePose
            PropLocation.Right -> rightPlopPose to rightPlacePose
        }

        val exitPose = if (propLocation == PropLocation.Center) {
            centerExitPose
        } else regularExitPose
        return getTrajectoryBuilder(startPose)
            .lineToSplineHeading(plopPose)
            .wait(0.1)
            .back(3.0)
            .lineToSplineHeading(exitPose)
            .lineToSplineHeading(stackPose)
            .wait(1.0)
            .splineTo(crossPose.vec(), crossPose.heading)
            .splineTo(placePose.vec(), placePose.heading)
            .wait(1.0)
            .forward(3.0)
            .wait(2.0)
            .build()
    }
}

object BlueCloseAuto {
    private var startPose = Pose2d(16.0, 3 * tile - 9.0, (-90).deg)
    private var rightPlopPose = Pose2d(10.0, 35.0, (180).deg)
    private var rightPlacePose = Pose2d(50.0, 33.0, 0.deg)
    private var centerPlopPose = Pose2d(16.0, 37.0, (-90).deg)
    private var centerPlacePose = Pose2d(50.0, 39.0, 0.deg)
    private var leftPlopPose = Pose2d(26.0, 46.0, (-90).deg)
    private var leftPlacePose = Pose2d(50.0, 48.0, 0.deg)
    private var parkPose = Pose2d(52.0, 64.0, 0.deg)

    fun getTrajectory(propLocation: PropLocation): Trajectory {
        val (plopPose, placePose) = when (propLocation) {
            PropLocation.Left -> leftPlopPose to leftPlacePose
            PropLocation.Center -> centerPlopPose to centerPlacePose
            PropLocation.Right -> rightPlopPose to rightPlacePose
        }
        return getTrajectoryBuilder(startPose)
            .lineToSplineHeading(plopPose)
            .wait(0.1)
            .back(3.0)
            .lineToSplineHeading(placePose)
            .wait(0.1)
            .forward(3.0)
            .wait(2.0)
            .back(3.0)
            .lineToSplineHeading(parkPose)
            .build()
    }
}

object RedFarAuto {
    private var startPose = Pose2d(-2 * tile + 8.0, -3 * tile + 9.0, (90).deg)
    private var leftPlopPose = Pose2d(-39.5, -32.0, (180).deg)
    private var centerPlopPose = Pose2d(-49.0, -25.0, (0).deg)
    private var rightPlopPose = Pose2d(-34.0, -35.0, (0).deg)
    private var leftPlacePose = Pose2d(47.5, -34.0, (-5).deg)
    private var centerPlacePose = Pose2d(47.5, -32.0, 0.deg)
    private var rightPlacePose = Pose2d(47.5, -43.0, 0.deg)
    private var regularExitPose = Pose2d(-40.0, -13.0, 0.deg)
    private var centerExitPose = Pose2d(-50.0, -13.0, 0.deg)
    private var stackPose = Pose2d(-61.0, -17.0, 0.deg)
    private var crossPose = Pose2d(30.0, -13.0, 0.deg)

    fun getTrajectory(propLocation: PropLocation): Trajectory {
        val (plopPose, placePose) = when (propLocation) {
            PropLocation.Left -> leftPlopPose to leftPlacePose
            PropLocation.Center -> centerPlopPose to centerPlacePose
            PropLocation.Right -> rightPlopPose to rightPlacePose
        }

        val exitPose = if (propLocation == PropLocation.Center) {
            centerExitPose
        } else regularExitPose
        return getTrajectoryBuilder(startPose)
            .lineToSplineHeading(plopPose)
            .wait(0.1)
            .back(3.0)
            .lineToSplineHeading(exitPose)
            .lineToSplineHeading(stackPose)
            .wait(1.0)
            .splineTo(crossPose.vec(), crossPose.heading)
            .splineTo(placePose.vec(), placePose.heading)
            .wait(1.0)
            .forward(3.0)
            .wait(1.0)
            .build()
    }
}

object RedCloseAuto {
    private var startPose = Pose2d(16.0, -3 * tile + 9.0, (90).deg)
    private var rightPlopPose = Pose2d(10.0, -34.0, (180).deg)
    private var rightPlacePose = Pose2d(50.0, -33.0, 0.deg)
    private var centerPlopPose = Pose2d(16.0, -37.0, (90).deg)
    private var centerPlacePose = Pose2d(50.0, -39.0, 0.deg)
    private var leftPlopPose = Pose2d(26.0, -46.0, (90).deg)
    private var leftPlacePose = Pose2d(50.0, -48.0, 0.deg)
    private var parkPose = Pose2d(52.0, -64.0, 0.deg)

    fun getTrajectory(propLocation: PropLocation): Trajectory {
        val (plopPose, placePose) = when (propLocation) {
            PropLocation.Left -> leftPlopPose to leftPlacePose
            PropLocation.Center -> centerPlopPose to centerPlacePose
            PropLocation.Right -> rightPlopPose to rightPlacePose
        }
        return getTrajectoryBuilder(startPose)
            .lineToSplineHeading(plopPose)
            .wait(0.1)
            .back(3.0)
            .lineToSplineHeading(placePose)
            .wait(0.1)
            .forward(3.0)
            .wait(1.0)
            .back(3.0)
            .lineToSplineHeading(parkPose)
            .build()
    }
}

class PathCommandBuilder(
    private val startPose: Pose2d,
    startTangent: Angle = startPose.heading,
) {
    private val paths = ArrayList<Path>()
    private var builder = PathBuilder(startPose, startTangent)
    private var currentPose = startPose

    private fun tryAdd(segment: PathBuilder.() -> Unit): PathCommandBuilder {
        try {
            builder.segment()
        } catch (e: PathContinuityViolationException) {
            pushAndAddPath()
            builder.segment()
        }
        return this
    }

    private fun pushPath(newTangent: (Angle) -> Angle = { it }): Path? {
        val path = builder.build().let {
            if (it.segments.isNotEmpty()) it else null
        }
        val newPose = path?.end() ?: currentPose
        builder = PathBuilder(
            newPose,
            Pose2d(newTangent(newPose.heading).vec()),
            path?.endSecondDeriv() ?: Pose2d()
        )
        currentPose = path?.end() ?: currentPose
        return path
    }

    private fun pushAndAddPath(newTangent: (Angle) -> Angle = { it }) {
        pushPath(newTangent)?.let {
            paths += it
        }
    }

    fun setTangent(angle: (Angle) -> Angle): PathCommandBuilder {
        pushAndAddPath(angle)
        return this
    }

    fun setTangent(angle: Angle) = setTangent { angle }

    fun reverseTangent(): PathCommandBuilder {
        pushAndAddPath { -it }
        return this
    }

    fun then(): PathCommandBuilder {
        pushAndAddPath()
        return this
    }

    fun combine(): PathCommandBuilder {
        pushAndAddPath()
        return this
    }

    fun and(): PathCommandBuilder = combine()

    fun wait(duration: Double) = then()

    fun race(): PathCommandBuilder = combine()

    fun withTimeout(duration: Double) = race()

    fun build(): Path {
        pushAndAddPath()
        return Path(paths.flatMap { it.segments })
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

object RedSus {
    private var startPose = Pose2d(16.0, -3 * tile + 9.0, (90).deg)
    private var leftPlopPose = Pose2d(10.0, -35.0, (-180).deg)
    private var leftPlacePose = Pose2d(50.0, -30.0, 0.deg)
    private var centerPlopPose = Pose2d(16.0, -37.0, (90).deg)
    private var centerPlacePose = Pose2d(50.0, -36.0, 0.deg)
    private var rightPlopPose = Pose2d(26.0, -46.0, (90).deg)
    private var rightPlacePose = Pose2d(50.0, -45.0, 0.deg)
    private var parkPose = Pose2d(52.0, -64.0, 0.deg)

    private var exitTangent = (-120).deg
    private var exitPose = Pose2d(18.0, -60.0, 180.deg)
    private var stackPose = Pose2d(-61.0, -36.0, 180.deg)
    private var crossPose = Pose2d(-36.0, -60.0, 180.deg)
    private var stackPlacePose = Pose2d(47.0, -45.0, 0.deg)

    private var stackHigh = 0.5

    fun getPath(propLocation: PropLocation): Path {
        val (plopPose, placePose) = when (propLocation) {
            PropLocation.Left -> leftPlopPose to leftPlacePose
            PropLocation.Center -> centerPlopPose to centerPlacePose
            PropLocation.Right -> rightPlopPose to rightPlacePose
        }
        val purplePlopCommand = PathCommandBuilder(startPose)
            .lineToSplineHeading(plopPose)
            .then()
            .build()
        val yellowPlaceCommand = PathCommandBuilder(plopPose)
            .back(3.0)
            .lineToSplineHeading(placePose)
            .wait(0.1)
            .forward(3.0).withTimeout(1.0)
            .then()
            .wait(2.0)
            .then()
            .wait(1.0)
            .then()
            .back(3.0)
            .build()

        val cycleCommand = PathCommandBuilder(yellowPlaceCommand.end())
            .setTangent(exitTangent)
            .splineToSplineHeading(exitPose, exitPose.heading)
            .splineToConstantHeading(crossPose.vec(), crossPose.heading)
            .splineToSplineHeading(stackPose, stackPose.heading)
            .and()
            .then()
            .setTangent(stackPose.heading + 180.deg)
            .splineToSplineHeading(Pose2d(crossPose.vec(), exitPose.heading), crossPose.heading + 180.deg)
            .splineToConstantHeading(exitPose.vec(), crossPose.heading + 180.deg)
            .splineToSplineHeading(stackPlacePose, stackPlacePose.heading)
            .wait(0.1)
            .then()
            .wait(1.0)
            .then()
            .wait(1.0)
            .then()
            .build()

        val parkCommand = PathCommandBuilder(leftPlacePose - Pose2d(3.0))
            .lineToSplineHeading(parkPose)
            .build()

        return Path(
            listOf(
                cycleCommand
            ).flatMap { it.segments }
        )
    }
}

object BlueSus {
    private var startPose = Pose2d(16.0, 3 * tile - 9.0, (-90).deg)
    private var leftPlopPose = Pose2d(10.0, 35.0, (180).deg)
    private var leftPlacePose = Pose2d(50.0, 30.0, 0.deg)
    private var centerPlopPose = Pose2d(16.0, 37.0, (-90).deg)
    private var centerPlacePose = Pose2d(50.0, 36.0, 0.deg)
    private var rightPlopPose = Pose2d(26.0, 46.0, (-90).deg)
    private var rightPlacePose = Pose2d(50.0, 45.0, 0.deg)
    private var parkPose = Pose2d(52.0, 64.0, 0.deg)

    private var exitTangent = (120).deg
    private var exitPose = Pose2d(18.0, 60.0, -180.deg)
    private var stackPose = Pose2d(-61.0, 36.0, -180.deg)
    private var crossPose = Pose2d(-36.0, 60.0, -180.deg)
    private var stackPlacePose = Pose2d(47.0, 45.0, 0.deg)

    private var stackHigh = 0.5

    fun getPath(propLocation: PropLocation): Path {
        val (plopPose, placePose) = when (propLocation) {
            PropLocation.Left -> leftPlopPose to leftPlacePose
            PropLocation.Center -> centerPlopPose to centerPlacePose
            PropLocation.Right -> rightPlopPose to rightPlacePose
        }
        val purplePlopCommand = PathCommandBuilder(startPose)
            .lineToSplineHeading(plopPose)
            .then()
            .build()
        val yellowPlaceCommand = PathCommandBuilder(plopPose)
            .back(3.0)
            .lineToSplineHeading(placePose)
            .wait(0.1)
            .forward(3.0).withTimeout(1.0)
            .then()
            .wait(2.0)
            .then()
            .wait(1.0)
            .then()
            .back(3.0)
            .build()

        val cycleCommand = PathCommandBuilder(yellowPlaceCommand.end())
            .setTangent(exitTangent)
            .splineToSplineHeading(exitPose, exitPose.heading)
            .splineToConstantHeading(crossPose.vec(), crossPose.heading)
            .splineToSplineHeading(stackPose, stackPose.heading)
            .and()
            .then()
            .setTangent(stackPose.heading + 180.deg)
            .splineToSplineHeading(Pose2d(crossPose.vec(), exitPose.heading), crossPose.heading + 180.deg)
            .splineToConstantHeading(exitPose.vec(), crossPose.heading + 180.deg)
            .splineToSplineHeading(stackPlacePose, stackPlacePose.heading)
            .wait(0.1)
            .then()
            .wait(1.0)
            .then()
            .wait(1.0)
            .then()
            .build()

        val parkCommand = PathCommandBuilder(leftPlacePose - Pose2d(3.0))
            .lineToSplineHeading(parkPose)
            .build()

        return Path(
            listOf(
                cycleCommand
            ).flatMap { it.segments }
        )
    }
}