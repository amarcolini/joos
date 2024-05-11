package pathfinding

import com.amarcolini.joos.geometry.Vector2d
import kotlin.math.roundToInt

class GridSpace2D(
    val start: Vector2d,
    val end: Vector2d,
    val rows: Int,
    val columns: Int
) : DStarSpace<GridSpace2D.Vertex2D>() {
    val width = end.x - start.x
    val height = end.y - start.y

    constructor(
        start: Vector2d,
        end: Vector2d,
        resolution: Vector2d
    ) : this(
        start,
        end,
        ((end.x - start.x) / resolution.x).roundToInt(),
        ((end.y - start.y) / resolution.y).roundToInt()
    )

    init {
        assert(start.x < end.x && start.y < end.y) { "start must come before end." }
    }

    inner class Vertex2D(val pos: Vector2d) : DStarSpace<Vertex2D>.Vertex() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Vertex2D) return false
            return pos == other.pos
        }
    }

    private val model = List(columns) { x ->
        List(rows) { y ->
            Vertex2D(
                Vector2d(
                    x * (width / (columns - 1.0)) + start.x,
                    y * (height / (rows - 1.0)) + start.y
                )
            )
        }
    }

    override val vertices: List<Vertex2D> by lazy { model.flatten() }

    override fun predecessors(b: Vertex2D): List<Vertex2D> = successors(b)

    override fun successors(a: Vertex2D): List<Vertex2D> {
        val xIndex = ((a.pos.x - start.x) / width * (columns - 1.0)).roundToInt()
        val yIndex = ((a.pos.y - start.y) / height * (rows - 1.0)).roundToInt()
        return listOfNotNull(
            model.getOrNull(xIndex - 1)?.getOrNull(yIndex),
            model.getOrNull(xIndex - 1)?.getOrNull(yIndex - 1),
            model.getOrNull(xIndex - 1)?.getOrNull(yIndex + 1),
            model.getOrNull(xIndex)?.getOrNull(yIndex + 1),
            model.getOrNull(xIndex)?.getOrNull(yIndex - 1),
            model.getOrNull(xIndex + 1)?.getOrNull(yIndex),
            model.getOrNull(xIndex + 1)?.getOrNull(yIndex - 1),
            model.getOrNull(xIndex + 1)?.getOrNull(yIndex + 1),
        )
            .mapNotNull {
                if (it in obstacles) null else it
            }
    }

    fun getVertex(pos: Vector2d): Vertex2D {
        val xIndex = ((pos.x - start.x) / width * (columns - 1.0)).roundToInt().coerceIn(0..<columns)
        val yIndex = ((pos.y - start.y) / height * (rows - 1.0)).roundToInt().coerceIn(0..<rows)
        return model[xIndex][yIndex]
    }

    val obstacles = mutableListOf<Vertex2D>()

    override fun c(a: Vertex2D, b: Vertex2D): Double = (a.pos distTo b.pos)

    override fun h(a: Vertex2D, b: Vertex2D): Double = a.pos distTo b.pos
}