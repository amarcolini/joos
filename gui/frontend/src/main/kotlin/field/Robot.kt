package field

import com.amarcolini.joos.geometry.Vector2d
import io.nacular.doodle.core.renderProperty
import io.nacular.doodle.drawing.*
import io.nacular.doodle.geometry.*
import io.nacular.doodle.system.Cursor
import io.nacular.doodle.system.SystemPointerEvent.Type
import io.nacular.measured.units.Angle
import io.nacular.measured.units.Measure

//class Robot(
//    dimensions: Vector2d = Vector2d(18.0, 18.0),
//    var paint: Paint = ColorPaint(Color.Black),
//    private val thickness: Double = 1.5
//) : FieldEntity() {
object Robot : PoseFieldEntity() {
    private val paint: Paint = ColorPaint(Color.Black)
    private const val thickness: Double = 1.5

    var dimensions: Vector2d = Vector2d(18.0, 18.0)
        set(value) {
            computeGeometry()
            rerender()
            field = value
        }

    private var mouseState by renderProperty(Type.Exit)
    private val padding = 5.0

    override fun intersects(point: Point): Boolean = (point - position) in clickBounds

    private fun computeGeometry() {
        val dimensions = dimensions - Vector2d(thickness, thickness) * 0.5
        val half = dimensions * 0.5
        val paddingOffset = Point(padding * 0.5, padding * 0.5)
        clickBounds = Rectangle(
            Point.Origin + paddingOffset,
            Size(dimensions.x, dimensions.y)
        )
        bounds = Rectangle(dimensions.x + padding, dimensions.y + padding)
        box = ConvexPolygon(
            Point(0.0, 0.0) + paddingOffset,
            Point(dimensions.x, 0.0) + paddingOffset,
            Point(dimensions.x, dimensions.y) + paddingOffset,
            Point(0.0, dimensions.y) + paddingOffset,
        )
            .rounded(3.0)
        center = Point(half.x, half.y) + paddingOffset
        headingLine = path(Point(dimensions.x, half.y) + paddingOffset)
            .lineTo(center)
            .finish()
    }

    private var box: Path = path(Point.Origin).finish()
    private var headingLine: Path = path(Point.Origin).finish()
    private var clickBounds: Rectangle = Rectangle()

    init {
        computeGeometry()
        clipCanvasToBounds = false
    }

    override fun rotateRender(canvas: Canvas) {
        val shadowColor = when (mouseState) {
            Type.Exit -> Color.Transparent
            Type.Click, Type.Down, Type.Drag -> Color.Green
            Type.Enter, Type.Move, Type.Up -> Color.Red
        }
        canvas.shadow(OuterShadow(0.0, 0.0, 2.0, shadowColor)) {
            this.path(box, Stroke(paint, thickness), ColorPaint(Color.Transparent))
            this.path(headingLine, Stroke(paint, thickness), ColorPaint(Color.Transparent))
            this.circle(
                Circle(center, thickness * 0.5),
                Stroke(thickness = 0.0),
                paint
            )
        }
    }
}