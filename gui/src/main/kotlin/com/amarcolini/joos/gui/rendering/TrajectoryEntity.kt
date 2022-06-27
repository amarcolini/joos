package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.AngleUnit
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.trajectory.*
import com.amarcolini.joos.gui.trajectory.LineTo
import com.amarcolini.joos.trajectory.config.GenericConstraints
import com.amarcolini.joos.util.*
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.CacheHint
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.scene.shape.*
import tornadofx.*
import kotlin.math.*

private const val SPATIAL_RESOLUTION = 1.0
private const val DRAGGING_RESOLUTION = 2.0

internal class TrajectoryEntity(private val getScale: () -> Double) : FieldEntity() {
    private val path: Path = Path()
    override val node: Group = Group(path)
    override var pose: Pose2d = Pose2d()
        private set

    override fun update(now: Long) {
        pose = Pose2d((trajectory.waypoints.first() as Start).pos)
    }

    init {
        node.isCache = true
        node.cacheHint = CacheHint.ROTATE
        path.isCache = true
        path.cacheHint = CacheHint.ROTATE
    }

    private var moving: Node? = null
    private var usedWaypoint: Waypoint? = null
    private var isPoint: Boolean = true
    var beingDragged: Boolean = false
        private set
    var trajectory: WaypointTrajectory = WaypointTrajectory(GenericConstraints(), Start())
        set(value) {
            trajectoryProperty.set(value)
            if (waypoints !== value.waypoints)
                waypoints.setAll(value.waypoints)
            if (constraints.value != value.constraints)
                constraints.set(value.constraints)

            path.elements.clear()
            node.children.removeIf {
                it != path && it != moving
            }

            val trajectory = value.trajectory
            val resolution = if (beingDragged) DRAGGING_RESOLUTION else SPATIAL_RESOLUTION

            if (trajectory != null) {
                // compute path samples
                val displacementSamples =
                    (trajectory.length() / resolution).roundToInt()
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
            }

            //render draggable waypoints
            waypoints.mapPose().zipWithNext { (_, last), (waypoint, current) ->
                val (pose, tangent) = current
                val (lastPose, _) = last
                when (waypoint) {
                    is Turn -> {
                        if (usedWaypoint == waypoint) return@zipWithNext
                        val arrow = node.group()
                        fun setAngle(angle: Angle, dragged: Boolean = false) {
                            arrow.children.clear()
                            val radius = 3.0
                            val turnAngle =
                                if (abs(angle) < Angle(2.0, AngleUnit.Degrees)) 0.0
                                else angle.degrees.coerceIn(-330.0, 330.0)
                            val endAngle = lastPose.heading + Angle.deg(turnAngle)
                            val pos = lastPose.vec() + Vector2d.polar(radius, endAngle)
                            val line1 =
                                pos + Vector2d.polar(
                                    1.0,
                                    endAngle + Angle.deg(90 * sign(turnAngle) + 135)
                                )
                            val line2 =
                                pos + Vector2d.polar(
                                    1.0,
                                    endAngle + Angle.deg(90 * sign(turnAngle) - 135)
                                )
                            if (dragged) {
                                arrow.arc(
                                    pose.x,
                                    pose.y,
                                    radius,
                                    radius,
                                    -lastPose.heading.degrees,
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
                                radius,
                                radius,
                                -lastPose.heading.degrees,
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
                        setAngle(waypoint.angle)

                        var currentWaypoint: Turn = waypoint
                        arrow.setOnMouseDragged {
                            isPoint = true
                            val mouse = Vector2d(it.x, it.y)
                            val newWaypoint = Turn(
                                ((mouse - lastPose.vec()).angle() - lastPose.heading).normDelta()
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
                                this.trajectory.constraints,
                                DRAGGING_RESOLUTION
                            )
                            arrow.toFront()
                            setAngle(newWaypoint.angle, true)
                        }
                        arrow.setOnMouseReleased {
                            setAngle(currentWaypoint.angle, false)
                            moving = null
                            beingDragged = false
                            usedWaypoint = null
                        }
                    }
                    is Wait -> {
                        if (usedWaypoint == waypoint) return@zipWithNext
                        val circle = node.circle(pose.x, pose.y, 1.0) {
                            fill = Color.LIGHTBLUE
                        }
                        circle.isMouseTransparent = true
                    }
                    else -> {
                        var currentWaypoint = waypoint
                        lateinit var lastMouse: Vector2d

                        val headingLine = if (waypoint is CustomHeading && !(usedWaypoint == waypoint && !isPoint)) {
                            val heading = waypoint.pose.heading
                            val pos = waypoint.pose.vec()
                            val start = Vector2d.polar(7.0, heading) + pos
                            val line1 = Vector2d.polar(6.0, heading - Angle(10.0, AngleUnit.Degrees)) + pos
                            val line2 = Vector2d.polar(6.0, heading + Angle(10.0, AngleUnit.Degrees)) + pos
                            val line = node.group { hide() }
                            val firstLine = line.line(start.x, start.y, line1.x, line1.y) {
                                stroke = Color.LIGHTBLUE
                                strokeLineCap = StrokeLineCap.ROUND
                                strokeWidth = 1.0
                            }
                            val secondLine = line.line(start.x, start.y, line2.x, line2.y) {
                                stroke = Color.LIGHTBLUE
                                strokeLineCap = StrokeLineCap.ROUND
                                strokeWidth = 1.0
                            }
                            val middleLine = line.line(pos.x, pos.y, start.x, start.y) {
                                stroke = Color.LIGHTBLUE
                                strokeLineCap = StrokeLineCap.ROUND
                                strokeWidth = 1.0
                            }
                            line.setOnMouseEntered {
                                line.show()
                            }
                            line.setOnMouseDragged {
                                val newAngle = (Vector2d(it.x, it.y) - pos).angle()
                                val newStart = Vector2d.polar(7.0, newAngle) + pos
                                val newLine1 = Vector2d.polar(6.0, newAngle - Angle(10.0, AngleUnit.Degrees)) + pos
                                val newLine2 = Vector2d.polar(6.0, newAngle + Angle(10.0, AngleUnit.Degrees)) + pos
                                firstLine.startX = newStart.x
                                firstLine.startY = newStart.y
                                secondLine.startX = newStart.x
                                secondLine.startY = newStart.y
                                firstLine.endX = newLine1.x
                                firstLine.endY = newLine1.y
                                secondLine.endX = newLine2.x
                                secondLine.endY = newLine2.y
                                middleLine.endX = newStart.x
                                middleLine.endY = newStart.y
                                val newWaypoint = waypoint.apply { this.pose = Pose2d(pos, newAngle) }
                                val currentIndex = waypoints.indexOf(currentWaypoint)
                                if (currentIndex < 0 || currentIndex > waypoints.size) return@setOnMouseDragged
                                isPoint = false
                                moving = line
                                beingDragged = true
                                val newList = waypoints.toMutableList()
                                newList[currentIndex] = newWaypoint
                                usedWaypoint = newWaypoint
                                currentWaypoint = newWaypoint
                                this.trajectory = WaypointTrajectory(
                                    newList,
                                    this.trajectory.constraints,
                                    DRAGGING_RESOLUTION
                                )
                            }
                            line.setOnMouseReleased {
                                moving = null
                                beingDragged = false
                                usedWaypoint = null
                                this.trajectory = this.trajectory
                                trajectoryProperty.set(this.trajectory)
                            }
                            line
                        } else null

                        val tangentLine = if (waypoint is Spline && !(usedWaypoint == waypoint && !isPoint)) {
                            val (x, y) = waypoint.pos
                            val (x2, y2) = Vector2d.polar(10.0, waypoint.tangent)
                            val group = node.group {
                                hide()
                                toFront()
                                headingLine?.toFront()
                            }
                            val line = group.line(x - x2, y - y2, x + x2, y + y2) {
                                stroke = c("#004000")
                                strokeLineCap = StrokeLineCap.BUTT
                                strokeWidth = 1.0
                            }
                            val size = 2.0
                            val box1 = group.rectangle(x - x2 - size / 2, y - y2 - size / 2, size, size) {
                                rotate = waypoint.tangent.degrees
                                fill = c("#004000")
                            }
                            val box2 = group.rectangle(x + x2 - size / 2, y + y2 - size / 2, size, size) {
                                rotate = waypoint.tangent.degrees
                                fill = c("#004000")
                            }
                            var firstAngle = waypoint.tangent
                            group.setOnMouseEntered {
                                group.show()
                                headingLine?.show()
                            }
                            group.setOnMousePressed {
                                firstAngle = waypoint.tangent - (Vector2d(it.x, it.y) - waypoint.pos).angle()
                            }
                            group.setOnMouseDragged {
                                val angle = (Vector2d(it.x, it.y) - waypoint.pos).angle() + firstAngle
                                val newWaypoint = waypoint.apply { this.tangent = angle }
                                val (newX, newY) = newWaypoint.pos
                                val (newX2, newY2) = Vector2d.polar(10.0, angle)
                                line.startX = newX - newX2
                                line.endX = newX + newX2
                                line.startY = newY - newY2
                                line.endY = newY + newY2
                                box1.x = newX - newX2 - size / 2
                                box1.y = newY - newY2 - size / 2
                                box2.x = newX + newX2 - size / 2
                                box2.y = newY + newY2 - size / 2
                                box1.rotate = angle.degrees
                                box2.rotate = angle.degrees
                                val currentIndex = waypoints.indexOf(currentWaypoint)
                                if (currentIndex < 0 || currentIndex > waypoints.size) return@setOnMouseDragged
                                isPoint = false
                                moving = group
                                beingDragged = true
                                val newList = waypoints.toMutableList()
                                newList[currentIndex] = newWaypoint
                                usedWaypoint = newWaypoint
                                currentWaypoint = newWaypoint
                                this.trajectory = WaypointTrajectory(
                                    newList,
                                    this.trajectory.constraints,
                                    DRAGGING_RESOLUTION
                                )
                            }
                            group.setOnMouseReleased {
                                moving = null
                                beingDragged = false
                                usedWaypoint = null
                                this.trajectory = this.trajectory
                                trajectoryProperty.set(this.trajectory)
                            }
                            group
                        } else null

                        if (usedWaypoint == waypoint && isPoint) return@zipWithNext
                        val circle = node.circle(pose.x, pose.y, 1.5) {
                            fill = Color.DARKGREEN
                            isCache = true
                            cacheHint = CacheHint.ROTATE
                        }
                        circle.setOnMousePressed {
                            val scale = getScale()
                            lastMouse = Vector2d(it.sceneY / scale, it.sceneX / scale)
                        }
                        circle.setOnMouseEntered {
                            tangentLine?.show()
                            headingLine?.show()
                        }
                        circle.setOnMouseExited {
                            tangentLine?.hide()
                            headingLine?.hide()
                        }
                        circle.setOnMouseDragged {
                            isPoint = true
                            val scale = getScale()
                            val mouse = Vector2d(it.sceneY / scale, it.sceneX / scale)
                            val delta = (mouse - lastMouse)
                            lastMouse = mouse
                            val new = Vector2d(delta.x + circle.centerX, delta.y + circle.centerY)
                            var pos =
                                Vector2d(new.x.coerceIn(-72.0, 72.0), new.y.coerceIn(-72.0, 72.0))
                            val newWaypoint = when (waypoint) {
                                is SplineTo -> SplineTo(pos, waypoint.tangent)
                                is Start -> Start(
                                    Pose2d(pos, waypoint.pose.heading),
                                    waypoint.tangent
                                )
                                is LineTo -> LineTo(pos)
                                is Forward -> {
                                    val c = pose.vec()
                                    val b = tangent.vec()
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
                                    val b = -tangent.vec()
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
                                    val b = (tangent + Angle(90.0, AngleUnit.Degrees)).vec()
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
                                    val b = (tangent - Angle(90.0, AngleUnit.Degrees)).vec()
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
                            circle.toFront()
                            moving = circle
                            beingDragged = true
                            val newList = waypoints.toMutableList()
                            newList[currentIndex] = newWaypoint
                            usedWaypoint = newWaypoint
                            currentWaypoint = newWaypoint
                            this.trajectory = WaypointTrajectory(
                                newList,
                                this.trajectory.constraints,
                                DRAGGING_RESOLUTION
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
                            usedWaypoint = null
                            this.trajectory = this.trajectory
                            trajectoryProperty.set(this.trajectory)
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
            trajectory =
                WaypointTrajectory(
                    it.list,
                    constraints.value,
                    if (beingDragged) DRAGGING_RESOLUTION else SPATIAL_RESOLUTION
                )
        }
        constraints.onChange {
            trajectory =
                WaypointTrajectory(
                    waypoints,
                    it ?: return@onChange,
                    if (beingDragged) DRAGGING_RESOLUTION else SPATIAL_RESOLUTION
                )
        }
    }
}