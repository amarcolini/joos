import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.DoubleProgression
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.drawing.paint
import io.nacular.doodle.geometry.Circle
import io.nacular.doodle.geometry.Point
import pathfinding.DStarLite
import pathfinding.GridSpace2D
import kotlin.test.Test

class PathFindingTest {
    class Rect(
        val start: Vector2d,
        val end: Vector2d
    ) {
        constructor(pair: Pair<Vector2d, Vector2d>) : this(pair.first, pair.second)

        operator fun contains(point: Vector2d) = (point.x in start.x..end.x) && (point.y in start.y..end.y)
    }

    @Test
    fun testPathFinding() {
        val space = GridSpace2D(
            Vector2d(-60.0, -60.0),
            Vector2d(60.0, 60.0),
            7, 7
        )
        val rects = listOf(
            Vector2d(-20.0, -10.0) to Vector2d(20.0, 10.0)
        ).map(::Rect)
        space.obstacles += space.vertices.mapNotNull { u ->
            if (rects.any { u.pos in it }) u else null
        }
        val search = DStarLite(space, space.getVertex(Vector2d(-30.0, -30.0)), space.getVertex(Vector2d(40.0, 40.0)))
        search.computeShortestPath()
        val path = search.getShortestPath()
        println(path?.map { it.pos })
        JoosGUI.customRender = {
            if (path != null) it.path(path.map { u ->
                Point(u.pos.x, u.pos.y)
            }, Stroke(Color.Red))
            for (u in space.vertices) {
                val color = when {
                    u == search.start -> Color.Magenta.paint
                    u == search.goal -> Color.Pink.paint
                    path != null && u in path -> Color.Red.paint
                    u in space.obstacles -> Color.Black.paint
                    else -> Color.Blue.paint
                }
                it.circle(Circle(Point(u.pos.x, u.pos.y), 2.0), color)
            }
        }
        JoosGUI.launch()
        try {
            while (GUIApp.activeState != GUIApp.State.Shutdown) {
                Thread.sleep(500)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}