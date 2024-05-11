package pathfinding

import kotlin.math.min

class DStarLite<T : DStarSpace<T>.Vertex>(val space: DStarSpace<T>, var start: T, val goal: T) {
    data class Key(
        val first: Double,
        val second: Double
    ) {
        constructor(pair: Pair<Double, Double>) : this(pair.first, pair.second)

        operator fun compareTo(other: Key): Int =
            if (first != other.first) first.compareTo(other.first)
            else second.compareTo(other.second)
    }

    private val queue = PriorityQueue<T> { a, b -> a.key.compareTo(b.key) }
    private val queueItems: MutableMap<T, PriorityQueue<T>.Item> = mutableMapOf()

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
        queueItems[goal] = queue.insert(goal.also { it.key = Key(space.h(start, goal) to 0.0) })
    }

    init {
        reset()
    }

    fun getShortestPath(): List<T>? {
        if (start.g == Double.POSITIVE_INFINITY) return null
        var current = start
        val list = mutableListOf(current)
        for (i in 1..space.vertices.size) {
            val result = space.successors(current).map {
                it to space.c(current, it) + it.g
            }.minBy { it.second }
//            if (result.second == Double.POSITIVE_INFINITY) return null
            current = result.first
            list += current
            if (current == goal) return list
        }
        println("timed out!")
        return list
    }

    fun computeShortestPath() {
        var u = queue.pop()
        queueItems.remove(u)
        while (u.key <= calculateKey(start) || start.rhs > start.g) {
            val newKey = calculateKey(u)
            when {
                u.key < newKey -> queueItems[u] = queue.insert(u.also { it.key = newKey })
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
            queueItems.remove(u)
        }
    }

    private fun updateVertex(u: T) {
        val item = queueItems.getOrDefault(u, null)
        if (item != null) queue.remove(item)
        if (u.g != u.rhs) {
            queueItems[u] = queue.insert(u.also { it.key = calculateKey(goal) })
        }
    }

    private fun calculateKey(vertex: T): Key = Key(
        min(vertex.g, vertex.rhs) + space.h(start, vertex), min(vertex.g, vertex.rhs)
    )
}

abstract class DStarSpace<T : DStarSpace<T>.Vertex> {
    abstract val vertices: List<T>

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
    abstract fun h(a: T, b: T): Double

    /**
     * Similar to [h], but works only on neighboring vertices.
     * Returns the true cost of traveling from one vertex to its neighbors.
     */
    abstract fun c(a: T, b: T): Double

    abstract fun successors(a: T): List<T>

    abstract fun predecessors(b: T): List<T>
}