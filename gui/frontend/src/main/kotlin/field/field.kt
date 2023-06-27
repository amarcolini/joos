package field

import GUIApp
import animation.TimeManager
import com.amarcolini.joos.geometry.AngleUnit
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.rad
import io.nacular.doodle.core.Layout
import io.nacular.doodle.core.View
import io.nacular.doodle.core.renderProperty
import io.nacular.doodle.drawing.*
import io.nacular.doodle.event.PointerListener.Companion.pressed
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.image.Image
import io.nacular.doodle.utils.observable
import io.nacular.measured.units.Angle
import io.nacular.measured.units.Measure
import util.GROW
import util.ObservableMap
import kotlin.math.PI
import kotlin.math.min

fun Pose2d.toPoint() = Point(x, y)
fun Vector2d.toPoint() = Point(x, y)
operator fun Point.plus(other: Vector2d) = Point(x + other.x, y + other.y)
operator fun Point.minus(other: Vector2d) = Point(x - other.x, y - other.y)
fun Point.coerceIn(min: Point, max: Point) = Point(x.coerceIn(min.x, max.x), y.coerceIn(min.y, max.y))
operator fun Vector2d.plus(other: Point) = Vector2d(x + other.x, y + other.y)
operator fun Vector2d.minus(other: Point) = Vector2d(x - other.x, y - other.y)
fun Point.toVector2d() = Vector2d(x, y)
val View.origin get() = bounds.atOrigin.center
var View.cPos
    set(value) {
        position = value + (parent?.origin ?: Point()) - origin
    }
    get() = position - (parent?.origin ?: Point()) + origin

/**
 * The field container.
 */
object Field : View() {
    val backgrounds: MutableMap<String, Image?> = ObservableMap(mutableMapOf(), ::rerender)
    const val fieldSize = 144.0

    var renderBackground: Canvas.(fieldRect: Rectangle) -> Unit = {}

    fun setBackgroundPaint(paint: Paint) {
        renderBackground = {
            rect(boundingBox, paint)
        }
    }

    init {
        idealSize = GROW
        children += DraggableTrajectory
        children += Robot
        Robot.visible = false
        TimeManager.listeners += { _, new ->
            val s = new / TimeManager.duration * DraggableTrajectory.currentPath.length()
            val current = DraggableTrajectory.currentPath[s]
            Robot.pose = current
            rerenderNow()
        }
//        children += WaypointPopup()
        Dragger(this).apply {
            this.allowOSConsume = false
        }

        pointerChanged += pressed {
            if (!it.target.focusable || it.target is EntityGroup) {
                GUIApp.focusManager.requestFocus(this)
            }
        }

        this.layout = Layout.simpleLayout { container ->
            container.children.filterIsInstance<EntityGroup>().forEach {
                it.size = container.size
            }
        }

        boundsChanged += { _, _, bounds ->
            children.filterIsInstance<FieldEntity>().forEach {
                it.recomputeTransform()
            }
        }
    }

    override fun render(canvas: Canvas) {
        val size = min(width, height)
        val fieldRect = Rectangle((width - size) * 0.5, (height - size) * 0.5, size, size)
        canvas.clear()
        canvas.renderBackground(fieldRect)
        backgrounds["Generic"]?.let {
            canvas.image(it, fieldRect)
        } ?: run {
            canvas.rect(fieldRect, Stroke(thickness = 2.0))
        }
    }
}

abstract class FieldEntity : View() {
    protected var baseTransform: AffineTransform2D = AffineTransform.Identity
        private set
    var center: Point by observable(Point.Origin) { _, _ ->
        recomputeTransform()
    }
        protected set

    fun recomputeTransform() {
        val parent = parent ?: return
        val size = min(parent.width, parent.height)
        transform = AffineTransform().translate(parent.bounds.atOrigin.center - center)
            .rotate(center, Measure(-PI * 0.5, Angle.radians))
            .scale(center, size / Field.fieldSize, size / Field.fieldSize)
    }
}

abstract class PoseFieldEntity : FieldEntity() {
    var heading: Measure<Angle> by renderProperty(Measure(0.0, Angle.radians))
    var pose: Pose2d = Pose2d(position.toVector2d(), heading.`in`(Angle.radians).rad)
        set(value) {
            position = value.toPoint()
            heading = Measure(value.heading.radians, Angle.radians)
            field = value
        }
        get() = Pose2d(position.toVector2d(), heading.`in`(Angle.radians).rad)


    final override fun render(canvas: Canvas) {
        canvas.clear()
        canvas.rotate(center, heading) {
            rotateRender(this)
        }
    }

    abstract fun rotateRender(canvas: Canvas)
}

abstract class EntityGroup : View() {
    fun recomputeTransforms() {
        children.filterIsInstance<FieldEntity>().forEach {
            it.recomputeTransform()
        }
    }

    init {
        boundsChanged += { _, _, _ ->
            recomputeTransforms()
        }
    }
}