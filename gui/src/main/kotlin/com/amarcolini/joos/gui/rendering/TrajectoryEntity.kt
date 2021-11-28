package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.style.Theme
import com.amarcolini.joos.gui.trajectory.*
import com.amarcolini.joos.trajectory.config.GenericConstraints
import com.amarcolini.joos.util.Angle
import com.amarcolini.joos.util.DoubleProgression
import com.amarcolini.joos.util.epsilonEquals
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.CacheHint
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.StrokeLineCap
import tornadofx.*
import kotlin.math.*

private const val SPATIAL_RESOLUTION = 1.0

internal class TrajectoryEntity : Entity() {
    private val path: Path = Path()
    override val node: Group = Group(path)
    override var pose: Pose2d = Pose2d()
        private set

    override fun update(now: Long, theme: Theme) {
        val bounds = node.boundsInLocal
        pose = Pose2d(bounds.centerX, bounds.centerY)
    }

    init {
        node.isCache = true
        node.cacheHint = CacheHint.ROTATE
        path.isCache = true
        path.cacheHint = CacheHint.ROTATE
    }

    private var moving: Node? = null
    private var usedWaypoint: Waypoint? = null
    var beingDragged: Boolean = false
        private set
    var trajectory: WaypointTrajectory = WaypointTrajectory(GenericConstraints(), Start())
        set(value) {
            trajectoryProperty.set(value)
            if (waypoints != value.waypoints)
                waypoints.setAll(value.waypoints)
            if (constraints.value != value.constraints)
                constraints.set(value.constraints)

            path.elements.clear()
            node.children.removeIf {
                it != path && it != moving
            }

            val trajectory = value.trajectory ?: return

            // compute path samples
            val displacementSamples =
                (trajectory.length() / SPATIAL_RESOLUTION).roundToInt()
            val displacements =
                DoubleProgression.fromClosedInterval(
                    0.0,
                    trajectory.length(),
                    displacementSamples
                )
            val poses = displacements.map { trajectory.path[it] }
            val start = trajectory.start()
            path.elements += MoveTo(start.x, start.y)
            path.elements += poses.drop(1).map {
                javafx.scene.shape.LineTo(it.x, it.y)
            }
            path.stroke = Color.GREEN
            path.strokeWidth = 1.0

            //render draggable waypoints
            waypoints.mapPose().zipWithNext { (_, last), (waypoint, current) ->
                if (usedWaypoint == waypoint) return@zipWithNext
                val (pose, tangent) = current
                val (lastPose, lastTangent) = last
                when (waypoint) {
                    is Turn -> {
                        val arrow = node.group()
                        fun setAngle(angle: Double, dragged: Boolean = false) {
                            arrow.children.clear()
                            val radius = 3.0
                            val turnAngle =
                                if (abs(angle) < 2) 0.0 else angle.coerceIn(-330.0, 330.0)
                            val endAngle = lastPose.heading + Math.toRadians(turnAngle)
                            val pos = lastPose.vec() + Vector2d(
                                cos(endAngle) * radius,
                                sin(endAngle) * radius
                            )
                            val line1 =
                                pos + Vector2d.polar(
                                    1.0,
                                    endAngle + (PI / 2 * sign(endAngle - lastPose.heading)) + Math.toRadians(
                                        135.0
                                    )
                                )
                            val line2 =
                                pos + Vector2d.polar(
                                    1.0,
                                    endAngle + (PI / 2 * sign(endAngle - lastPose.heading)) - Math.toRadians(
                                        135.0
                                    )
                                )
                            if (dragged) {
                                arrow.arc(
                                    pose.x,
                                    pose.y,
                                    3.0,
                                    3.0,
                                    -Math.toDegrees(lastPose.heading),
                                    -turnAngle
                                ) {
                                    fill = Color.TRANSPARENT
                                    strokeLineCap = StrokeLineCap.ROUND
                                    stroke = Color.BLACK
                                    strokeWidth = 1.5
                                }
                                arrow.line(pos.x, pos.y, line1.x, line1.y) {
                                    stroke = Color.BLACK
                                    strokeLineCap = StrokeLineCap.ROUND
                                    strokeWidth = 1.5
                                }
                                arrow.line(pos.x, pos.y, line2.x, line2.y) {
                                    stroke = Color.BLACK
                                    strokeLineCap = StrokeLineCap.ROUND
                                    strokeWidth = 1.5
                                }
                            }
                            arrow.arc(
                                pose.x,
                                pose.y,
                                3.0,
                                3.0,
                                -Math.toDegrees(lastPose.heading),
                                -turnAngle
                            ) {
                                fill = Color.TRANSPARENT
                                strokeLineCap = StrokeLineCap.ROUND
                                stroke = Color.LIGHTBLUE
                                strokeWidth = 1.0
                            }
                            arrow.line(pos.x, pos.y, line1.x, line1.y) {
                                stroke = Color.LIGHTBLUE
                                strokeLineCap = StrokeLineCap.ROUND
                                strokeWidth = 1.0
                            }
                            arrow.line(pos.x, pos.y, line2.x, line2.y) {
                                stroke = Color.LIGHTBLUE
                                strokeLineCap = StrokeLineCap.ROUND
                                strokeWidth = 1.0
                            }
                        }
                        setAngle(waypoint.angle.value)

                        var currentWaypoint: Turn = waypoint
                        arrow.setOnMouseDragged {
                            val mouse = Vector2d(it.x, it.y)
                            val newWaypoint = Turn(
                                Degree(
                                    Angle.normDelta((mouse - lastPose.vec()).angle() - lastPose.heading),
                                    false
                                )
                            )
                            val currentIndex = waypoints.indexOf(currentWaypoint)
                            if (currentIndex < 0 || currentIndex > waypoints.size) return@setOnMouseDragged
                            moving = arrow
                            beingDragged = true
                            val newList = waypoints.toMutableList()
                            newList[currentIndex] = newWaypoint
                            usedWaypoint = newWaypoint
                            currentWaypoint = newWaypoint
                            this.trajectory = WaypointTrajectory(
                                newList,
                                this.trajectory.constraints
                            )
                            arrow.toFront()
                            setAngle(newWaypoint.angle.value, true)
                        }
                        arrow.setOnMouseReleased {
                            setAngle(currentWaypoint.angle.value, false)
                            moving = null
                            beingDragged = false
                            usedWaypoint = null
                        }
                    }
                    is Wait -> {
                        val circle = node.circle(pose.x, pose.y, 1.0) {
                            fill = Color.LIGHTBLUE
                        }
                        circle.isMouseTransparent = true
                    }
                    else -> {
                        val circle = node.circle(pose.x, pose.y, 1.0) {
                            fill = Color.DARKGREEN
                        }
                        circle.isCache = true
                        circle.cacheHint = CacheHint.ROTATE
                        var currentWaypoint = waypoint
                        circle.setOnMouseDragged {
                            var pos =
                                Vector2d(it.x.coerceIn(-72.0, 72.0), it.y.coerceIn(-72.0, 72.0))
                            val newWaypoint = when (waypoint) {
                                is SplineTo -> SplineTo(pos, waypoint.tangent)
                                is Start -> Start(
                                    Pose2d(pos, waypoint.pose.heading),
                                    waypoint.tangent
                                )
                                is LineTo -> LineTo(pos)
                                is Forward -> {
                                    val c = pose.vec()
                                    val b = Angle.vec(tangent.radians)
                                    val numerator = (pos - c) dot b
                                    val result =
                                        (b * (numerator / (b dot b))) + c
                                    val xDif = (result - c + b * waypoint.distance).x /
                                            if (b.x epsilonEquals 0.0) 0.0 else b.x
                                    pos = result
                                    Forward(
                                        if (!xDif.isInfinite() && !xDif.isNaN()) xDif
                                        else (result - c + b * waypoint.distance).y / b.y
                                    )
                                }
                                is Back -> {
                                    val c = pose.vec()
                                    val b = -Angle.vec(tangent.radians)
                                    val numerator = (pos - c) dot b
                                    val result =
                                        (b * (numerator / (b dot b))) + c
                                    val xDif = (result - c + b * waypoint.distance).x /
                                            if (b.x epsilonEquals 0.0) 0.0 else b.x
                                    pos = result
                                    Back(
                                        if (!xDif.isInfinite() && !xDif.isNaN()) xDif
                                        else (result - c + b * waypoint.distance).y / b.y
                                    )
                                }
                                is StrafeLeft -> {
                                    val c = pose.vec()
                                    val b = Angle.vec(tangent.radians + PI / 2)
                                    val numerator = (pos - c) dot b
                                    val result =
                                        (b * (numerator / (b dot b))) + c
                                    val xDif = (result - c + b * waypoint.distance).x /
                                            if (b.x epsilonEquals 0.0) 0.0 else b.x
                                    pos = result
                                    StrafeLeft(
                                        if (!xDif.isInfinite() && !xDif.isNaN()) xDif
                                        else (result - c + b * waypoint.distance).y / b.y
                                    )
                                }
                                is StrafeRight -> {
                                    val c = pose.vec()
                                    val b = Angle.vec(tangent.radians - PI / 2)
                                    val numerator = (pos - c) dot b
                                    val result =
                                        (b * (numerator / (b dot b))) + c
                                    val xDif = (result - c + b * waypoint.distance).x /
                                            if (b.x epsilonEquals 0.0) 0.0 else b.x
                                    pos = result
                                    StrafeRight(
                                        if (!xDif.isInfinite() && !xDif.isNaN()) xDif
                                        else (result - c + b * waypoint.distance).y / b.y
                                    )
                                }
                                is StrafeTo -> StrafeTo(pos)
                                is SplineToConstantHeading -> SplineToConstantHeading(
                                    pos,
                                    tangent
                                )
                                is SplineToLinearHeading -> SplineToLinearHeading(
                                    Pose2d(
                                        pos,
                                        pose.heading
                                    ), tangent
                                )
                                is SplineToSplineHeading -> SplineToSplineHeading(
                                    Pose2d(
                                        pos,
                                        pose.heading
                                    ), tangent
                                )
                                is LineToConstantHeading -> LineToConstantHeading(pos)
                                is LineToLinearHeading -> LineToLinearHeading(
                                    Pose2d(
                                        pos,
                                        pose.heading
                                    )
                                )
                                is LineToSplineHeading -> LineToSplineHeading(
                                    Pose2d(
                                        pos,
                                        pose.heading
                                    )
                                )
                                else -> return@setOnMouseDragged
                            }
                            val currentIndex = waypoints.indexOf(currentWaypoint)
                            if (currentIndex < 0 || currentIndex > waypoints.size) return@setOnMouseDragged
                            moving = circle
                            beingDragged = true
                            val newList = waypoints.toMutableList()
                            newList[currentIndex] = newWaypoint
                            usedWaypoint = newWaypoint
                            currentWaypoint = newWaypoint
                            this.trajectory = WaypointTrajectory(
                                newList,
                                this.trajectory.constraints
                            )
                            circle.stroke = Color.BLACK
                            circle.strokeWidth = 0.25
                            circle.centerX = pos.x
                            circle.centerY = pos.y
                        }
                        circle.setOnMouseReleased {
                            circle.stroke = Color.TRANSPARENT
                            moving = null
                            beingDragged = false
                            trajectoryProperty.set(this.trajectory)
                            usedWaypoint = null
                        }
                    }
                }
            }

            field = value
        }

    val trajectoryProperty = SimpleObjectProperty(trajectory)
    val waypoints = trajectory.waypoints.toObservable()
    val constraints = SimpleObjectProperty(trajectory.constraints)

    init {
        waypoints.onChange {
            trajectory = WaypointTrajectory(it.list, constraints.value)
        }
        constraints.onChange {
            trajectory = WaypointTrajectory(waypoints, it ?: return@onChange)
        }
    }
}