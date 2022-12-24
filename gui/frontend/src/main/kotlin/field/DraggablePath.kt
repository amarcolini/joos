package field

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.PathBuilder
import com.amarcolini.joos.util.abs
import com.amarcolini.joos.util.deg
import com.amarcolini.joos.util.sign
import field.Field.fieldSize
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.*
import io.nacular.doodle.focus.FocusTraversalPolicy
import io.nacular.doodle.focus.impl.FocusTraversalPolicyImpl
import io.nacular.doodle.focus.impl.FocusabilityChecker
import io.nacular.doodle.geometry.Circle
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.utils.PropertyObserver
import io.nacular.doodle.utils.PropertyObserversImpl

object DraggablePath {
    var startTangent: Angle = 0.deg
    var startTangentMag: Double = 1.0
    val startTangentLine = object : FieldEntity() {
        private var start = Circle(1.0)
        private var end = Circle(1.0)
        private val stretchFactor = 0.2

        init {
            Dragger(this).apply {
                var flipped = false
                mouseDown = {
                    flipped = abs(((it).toVector2d().angle() - startTangent).normDelta()) >= 45.deg
                }
                mouseDragged = { pos, _ ->
                    val newAngle = (pos).toVector2d().angle()
                    startTangent = newAngle + if (flipped) 180.deg else 0.deg
                    startTangentMag = pos.toVector2d().norm() / stretchFactor
                    update()
                    rerender()
                }
            }
            clipCanvasToBounds = false
            bounds = Rectangle(10.0, 10.0)
            visible = false
            focusable = false
        }

        override fun render(canvas: Canvas) {
            val tangentVec = startTangent.vec() * startTangentMag * stretchFactor
            start = Circle(tangentVec.toPoint(), 1.0)
            end = Circle(-tangentVec.toPoint(), 1.0)
            canvas.line(start.center, end.center, Stroke(Color.Black, 0.7))
            canvas.circle(start, Stroke(Color.Black, 0.2), Color.Blue)
            canvas.circle(end, Stroke(Color.Black, 0.2), Color.Blue)
        }

        override fun intersects(point: Point): Boolean {
            val spot = point - position
            return spot in start || spot in end
        }
    }
    val startCircle = object : FieldEntity() {
        private val circle = Circle(2.0)

        init {
            val view = this
            Dragger(view).apply {
                mouseDragged = { _, delta ->
                    val extreme = Point(fieldSize, fieldSize) * 0.5
                    position = (position + delta).coerceIn(-extreme, extreme)
                    startTangentLine.position = position
                    update()
                    rerender()
                }
            }
            clipCanvasToBounds = false
            bounds = Rectangle(10.0, 10.0)
            focusable = true

            focusChanged += { source, old, new ->
                println("focused changed to $new")
                startTangentLine.visible = new
            }
        }

        override fun render(canvas: Canvas) {
            canvas.circle(circle, Color.Blue)
        }

        override fun intersects(point: Point) = (point - position) in circle
    }
    val endCircle = object : FieldEntity() {
        private val circle = Circle(2.0)

        init {
            Dragger(this).apply {
                mouseDragged = { _, delta ->
                    val extreme = Point(fieldSize, fieldSize) * 0.5
                    position = (position + delta).coerceIn(-extreme, extreme)
                    update()
                    rerender()
                }
            }
            clipCanvasToBounds = false
            bounds = Rectangle(10.0, 10.0)
            position = Point(30.0, 30.0)
            focusable = true
        }

        override fun render(canvas: Canvas) {
            canvas.circle(circle, Color.Green)
        }

        override fun intersects(point: Point) = (point - position) in circle
    }
    val pathEntity = PathEntity(
        PathBuilder(Pose2d(startCircle.position.toVector2d(), startTangent))
            .splineTo(endCircle.position.toVector2d(), 0.deg)
            .build(), Stroke(Color.Black)
    )

    private fun update() {
        val start = startCircle.position
        val end = endCircle.position
        val newStroke = Stroke(LinearGradientPaint(Color.Blue, Color.Green, start, end))
        val newPath = PathBuilder(Pose2d(start.toVector2d(), startTangent))
            .splineTo(end.toVector2d(), 0.deg, startTangentMag, -1.0)
            .build()
        pathEntity.stroke = newStroke
        pathEntity.path = newPath
    }

    init {
        startTangentMag = startCircle.position.distanceFrom(endCircle.position)
        update()
    }

    fun getEntities() = listOf(pathEntity, startTangentLine, startCircle, endCircle)
}