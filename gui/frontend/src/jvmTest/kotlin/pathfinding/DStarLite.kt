package pathfinding

import kotlin.math.min

class DStarLite(val space: DStarSpace, var start: DStarSpace.Vertex, val goal: DStarSpace.Vertex) {
    data class Key(
        val first: Double,
        val second: Double
    ) {
        constructor(pair: Pair<Double, Double>) : this(pair.first, pair.second)

        operator fun compareTo(other: Key): Int =
            if (first != other.first) first.compareTo(other.first)
            else second.compareTo(other.second)
    }

    private val queue = PriorityQueue<DStarSpace.Vertex> { a, b -> a.key.compareTo(b.key) }

    private var kM = 0.0
    private var sLast = start
    fun reset() {
        queue.clear()
        kM = 0.0
        space.vertices.forEach {
            it.rhs = Double.POSITIVE_INFINITY
            it.g = Double.POSITIVE_INFINITY
        }
        goal.rhs = 0.0
        queue += goal.also { it.key = Key(space.h(it, start) to 0.0) }
    }

    init {
        reset()
    }

    fun computeShortestPath() {
        var u = queue.pop()
        while (u.key <= calculateKey(start) || start.rhs > start.g) {
            val newKey = calculateKey(u)
            when {
                u.key < newKey -> queue.insert(u.also { it.key = newKey })
                u.g > u.rhs -> {
                    u.g = u.rhs
                    space.predecessors(u).forEach { s ->
                        if (s != goal) s.rhs = min(s.rhs, space.c(s, u) + u.g)
                        updateVertex(s)
                    }
                }
                else -> {
                    val oldG = u.g
                    u.g = Double.POSITIVE_INFINITY
                    (space.predecessors(u) + u).forEach { s ->
                        if (s != goal && s.rhs == space.c(s, u) + oldG) s.rhs = space.successors(s).minOf {
                            space.c(s, it) + it.g
                        }
                        updateVertex(s)
                    }
                }
            }
            u = queue.pop()
        }
    }

    private fun updateVertex(vertex: DStarSpace.Vertex) {

    }

    private fun calculateKey(vertex: DStarSpace.Vertex): Key = Key(
        min(vertex.g, vertex.rhs) + space.h(vertex, goal), min(vertex.g, vertex.rhs)
    )
}

abstract class DStarSpace {
    abstract val vertices: List<Vertex>

    abstract inner class Vertex protected constructor() {
        /**
         * A running estimate of the cost from the start vertex to the current vertex. Used internally.
         */
        var g: Double = 0.0
            internal set

        var rhs: Double = 0.0
            internal set

        var key: DStarLite.Key = DStarLite.Key(0.0 to 0.0)
            internal set
    }

    /**
     * A heuristic that estimates the distance from vertex [a] to [b]. Must obey the following constraints:
     * - non-negative
     * - consistent
     * - h(a,a) = 0
     * - always less than or equal to the true cost between the vertices
     * - always less than or equal to the cost to another vertex, and then [b]
     */
    abstract fun h(a: Vertex, b: Vertex): Double

    /**
     * Similar to [h], but works only on neighboring vertices.
     * Returns the true cost of traveling from one vertex to its neighbors.
     */
    abstract fun c(a: Vertex, b: Vertex): Double

    abstract fun successors(a: Vertex): List<Vertex>

    abstract fun predecessors(b: Vertex): List<Vertex>
}