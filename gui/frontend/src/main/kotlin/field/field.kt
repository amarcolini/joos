package field

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.PathBuilder
import com.amarcolini.joos.util.rad
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.AffineTransform
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.image.Image
import io.nacular.doodle.utils.ObservableList
import io.nacular.doodle.utils.ObservableSet
import io.nacular.measured.units.Angle
import io.nacular.measured.units.Measure
import kotlin.math.min

fun Pose2d.toPoint() = Point(x, y)
fun Vector2d.toPoint() = Point(x, y)
fun Point.toVector2d() = Vector2d(x, y)

/**
 * The field container.
 */
object Field : View() {
    val backgrounds: MutableMap<String, Image?> = ObservableMap(mutableMapOf(), ::rerender)

    init {
        children += listOf(
            PathEntity(
                PathBuilder(Pose2d(4.0, 4.0))
                    .splineTo(Vector2d(30.0, 30.0), 0.rad)
                    .build()
            ),
            Robot()
        )
    }

    override fun render(canvas: Canvas) {
        val size = min(width, height)
        val fieldRect = Rectangle((width - size) * 0.5, 0.0, size, size)
        canvas.clear()
        backgrounds["Generic"]?.let {
            canvas.image(it, fieldRect)
        }
        children.filterIsInstance<FieldEntity>().forEach {
            it.transform =
                AffineTransform()
                    .translate(bounds.center - it.center)
                    .rotate(it.center, Measure(-90.0, Angle.degrees))
                    .scale(it.center, size / 144.0, size / 144.0)
                    .rotate(it.center + it.position, -it.heading)
        }
    }
}

abstract class FieldEntity : View() {
    var heading: Measure<Angle> = Measure(0.0, Angle.radians)
    open val center: Point = Point.Origin
}

class ObservableMap<K, V>(
    private val base: MutableMap<K, V>,
    private val onUpdate: () -> Unit
) : MutableMap<K, V> by base {
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = ObservableSet(base.entries).apply {
            changed += { _, _, _ ->
                onUpdate()
            }
        }

    override val keys: MutableSet<K>
        get() = ObservableSet(base.keys).apply {
            changed += { _, _, _ ->
                onUpdate()
            }
        }

    override val values: MutableCollection<V>
        get() = ObservableList(base.values.toList()).apply {
            changed += { _, _, _, _ ->
                onUpdate()
            }
        }

    override fun clear() {
        if (size > 0) {
            base.clear()
            onUpdate()
        }
    }

    override fun put(key: K, value: V): V? {
        val prev = base.put(key, value)
        if (prev !== value)
            onUpdate()
        return prev
    }

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from)
            put(key, value)
    }

    override fun remove(key: K): V? {
        if (containsKey(key)) {
            val prev = base.remove(key)
            onUpdate()
            return prev
        }
        return null
    }

    override fun equals(other: Any?): Boolean = base == other

    override fun hashCode(): Int = base.hashCode()

    override fun toString(): String = base.toString()
}