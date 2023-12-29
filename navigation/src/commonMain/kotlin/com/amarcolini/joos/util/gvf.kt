package com.amarcolini.joos.util

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.CircularArc
import com.amarcolini.joos.path.LineSegment
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.PositionPath
import com.amarcolini.joos.util.GuidingVectorField.Query
import kotlin.jvm.JvmStatic
import kotlin.math.*

interface VectorField {
    /**
     * Returns the normalized value of the vector field at the given point.
     */
    operator fun get(x: Double, y: Double): Vector2d
}

/**
 * Guiding vector field for effective path following described in section III, eq. (9) of
 * [1610.04391.pdf](https://arxiv.org/pdf/1610.04391.pdf). Implementation note: the function phi (see eq. (2))
 * and its properties are split up into [Phi.target] and [Phi.tangent].
 */
interface GuidingVectorField : VectorField {
    /**
     * Path normal weight (see eq. (9)). Higher values make path convergence more aggressive.
     */
    val kN: Double

    /**
     * Custom error mapping (see eq. (4))
     */
    val errorMapFunc: (Double) -> Double

    /**
     * The starting position. Used for basic velocity profiling to accelerate accordingly.
     */
    val startPosition: Vector2d?

    /**
     * The final target position. Used for basic velocity profiling to decelerate accordingly.
     */
    val endPosition: Vector2d?

    class Query internal constructor(val point: Vector2d) {
        /**
         * @param target the nearest point on P (eq. (2))
         * @param tangent the tangent to the path (eq. (8))
         */
        inner class Phi(
            val target: Vector2d,
            val tangent: Vector2d
        ) {
            /**
             * *Note:* instead of calling [Vector2d.norm] on this value, use [error].
             */
            val pathToPoint = point - target

            /**
             * Whether [point] is on the left or right of [target].
             */
            val orientation by lazy {
                -sign(pathToPoint.x * tangent.y - pathToPoint.y * tangent.x)
            }

            val error by lazy {
                pathToPoint.norm()
            }

            val normal = Vector2d(-tangent.y, tangent.x)

            val query = this@Query
        }

        override fun toString(): String = "GVFQuery$point"
    }

    fun internalGet(query: Query): Query.Phi

    fun compute(phi: Query.Phi): Vector2d {
        val isFollowingPath =
            !((startPosition?.let { phi.target epsilonEquals it && !(phi.error epsilonEquals 0.0) } == true)
                    || (endPosition?.let { phi.target epsilonEquals it } == true))
        val signedError = phi.orientation * phi.error
        if (isFollowingPath) {
            val normal = phi.normal
            return phi.tangent - normal * kN * errorMapFunc(signedError)
        } else {
            val normal =
                if (phi.error epsilonEquals 0.0) Vector2d()
                else (phi.pathToPoint / phi.error)
            return normal * kN * errorMapFunc(-phi.error)
        }
    }

    /**
     * Returns the normalized value of the vector field at the given point.
     */
    override operator fun get(x: Double, y: Double): Vector2d {
        val query = Query(Vector2d(x, y))
        return compute(internalGet(query))
    }
}

interface FollowableGVF : GuidingVectorField {
    val path: Path
    val lastProjectDisplacement: Double

    override val startPosition: Vector2d
    override val endPosition: Vector2d

    /**
     * Any resetting that needs to happen before another follower uses this vector field should go here.
     */
    fun reset()
}

/**
 * An implementation of [GuidingVectorField] that follows the specified [path].
 */
class PathGVF(
    override val path: Path,
    override val kN: Double,
    override val errorMapFunc: (Double) -> Double = { it }
) : GuidingVectorField, FollowableGVF {
    override val startPosition: Vector2d = path.start().vec()
    override val endPosition: Vector2d = path.end().vec()

    override var lastProjectDisplacement = 0.0

    override fun internalGet(query: Query): Query.Phi {
        val displacement = path.fastProject(query.point, lastProjectDisplacement)
        val pathPoint = path[displacement]
        val tangent = path.deriv(displacement).vec()
        lastProjectDisplacement = displacement
        return query.Phi(pathPoint.vec(), tangent)
    }

    override fun reset() {
        lastProjectDisplacement = 0.0
    }
}

/**
 * An obstacle vector field (\(\mathcal{X}_{\mathcal{R}_i}\) eq. 4) as described in [this paper](https://arxiv.org/pdf/2205.12760.pdf).
 *
 * [gvf] should be a closed loop, and the tangent (in [Phi.tangent]) should point counterclockwise by default.
 *
 * *Note:* For both map functions, an input of 0 means the query point is fully outside \(\mathcal{R}_i\),
 * and an input of 1 means the query point is fully inside \(\mathcal{Q}_i\).
 *
 * @param insetDistance The distance to inset \(\mathcal{R}_i\) to define \(\mathcal{Q}_i\). This makes
 * the smooth bump functions much easier to implement.
 * @param zeroInMapFunc a smooth function with domain `[0, 1]` and range `[0, 1]`.
 * An output of 1 means fully weighting the path vector. Inputs of 0 and 1 should give outputs of 0 and 1,
 * respectively.
 * @param zeroOutMapFunc a smooth function with domain `[0, 1]` and range `[0, 1]`.
 * An output of 1 means fully weighting the obstacle vector. Inputs of 0 and 1 should give outputs of 0 and 1,
 *  * respectively.
 * @usesMathJax
 */
class GVFObstacle(
    val gvf: GuidingVectorField,
    val insetDistance: Double,
    val zeroInMapFunc: (Double) -> Double = { defaultMapFunction(it) },
    val zeroOutMapFunc: (Double) -> Double = { defaultMapFunction(it) }
) {
    companion object {
        /**
         * Default smooth map function using `tanh`.
         * @param x input in the range `[0, 1]`.
         * @param l1 weight towards 0. (l1 > 0)
         * @param l2 weight towards 1. (l2 > 0)
         * */
        fun defaultMapFunction(x: Double, l1: Double = 1.0, l2: Double = 1.0): Double {
            val x2 = x - 1
            return (1 - tanh((l1 * x + l2 * x2) / (x * x2) * 0.5)) * 0.5
        }
    }

    fun combineWith(otherPhi: Query.Phi, pathVec: Vector2d): Vector2d? = combineWith(
        gvf.internalGet(otherPhi.query), otherPhi, pathVec
    )

    fun combineWith(phi: Query.Phi, otherPhi: Query.Phi, pathVec: Vector2d): Vector2d? {
        val query = otherPhi.query
        return if (phi.orientation > 0) {
            if (phi.error > insetDistance) gvf.compute(phi)
            else {
                val t = phi.error / insetDistance
                val pathNorm = pathVec.norm()
                val obstacleVec =
                    if (phi.tangent dot otherPhi.tangent > 0.0) gvf.compute(phi)
                    else gvf.compute(query.Phi(phi.target, -phi.tangent))
                val obstacleNorm = obstacleVec.norm()
                val initialAngle = pathVec.angle()
                val zeroIn = zeroInMapFunc(1 - t)
                val zeroOut = zeroOutMapFunc(t)
                val vecDot = pathVec dot obstacleVec
                val midVec = if (abs(vecDot / (pathNorm * obstacleNorm)) epsilonEquals 1.0) {
                    Vector2d(-pathVec.y, pathVec.x)
                } else pathNorm * obstacleVec + obstacleNorm * pathVec
                val angle = atan2(pathVec cross obstacleVec, vecDot).rad
                    .let {
                        if (midVec dot otherPhi.tangent > 0) it
                        else Angle.circle * -sign(it) + it
                    }
                val target = angle * zeroOutMapFunc(t) + initialAngle
//                val target = obstacleVec.angle()
                (target).vec() * (pathNorm * zeroIn + obstacleNorm * zeroOut)
//                zeroInMapFunc(t) * other[point.x, point.y] + zeroOutMapFunc(t) * gvf.compute(phi)
            }
        } else null
    }

    fun combineWith(other: GuidingVectorField, point: Vector2d): Vector2d? {
        val query = Query(point)
        val otherPhi = other.internalGet(query)
        return combineWith(otherPhi, other.compute(otherPhi))
    }
}

/**
 * A composite guiding vector field that avoids [obstacles] and follows [pathGVF].
 *
 * @param correctionRadius If the robot is within this distance of the end of the path,
 * all obstacles will be ignored.
 */
class CompositeGVF(
    val pathGVF: PathGVF,
    val obstacles: Iterable<GVFObstacle>,
    val correctionRadius: Double = 5.0
) : FollowableGVF by pathGVF {
    override val path: Path = pathGVF.path
    override var lastProjectDisplacement: Double = 0.0
        private set

    constructor(pathGVF: PathGVF, vararg obstacles: GVFObstacle) : this(pathGVF, obstacles.toList())

    override fun reset() {
        pathGVF.reset()
        lastProjectDisplacement = pathGVF.lastProjectDisplacement
    }

    private data class ObstacleResult(
        val vector: Vector2d,
        val weight: Double,
        val zeroOutMapFunc: (Double) -> Double
    )

    override fun compute(phi: Query.Phi): Vector2d {
        val pathVec = pathGVF.compute(phi)
        lastProjectDisplacement = pathGVF.lastProjectDisplacement
        if (phi.target epsilonEquals pathGVF.endPosition && phi.error < correctionRadius) return pathVec
        var maxWeight = 0.0
        val currentObstacles = obstacles.mapNotNull {
            val obstaclePhi = it.gvf.internalGet(phi.query)
            if (obstaclePhi.orientation > 0) {
                val vector = it.combineWith(obstaclePhi, phi, pathVec)!!
                if (obstaclePhi.error > it.insetDistance)
                    return vector
                val weight = obstaclePhi.error / it.insetDistance
                maxWeight = max(maxWeight, weight)
                ObstacleResult(vector, weight, it.zeroOutMapFunc)
            }
            else null
        }
        if (currentObstacles.isEmpty()) return pathVec
        if (currentObstacles.size == 1 || maxWeight epsilonEquals 0.0) return currentObstacles[0].vector
        var vectorSum = Vector2d()
        var magSum = 0.0
        var weightSum = 0.0
        for ((vector, weight, zeroOutFunc) in currentObstacles) {
            val actualWeight = zeroOutFunc(weight)
            println("$weight, ${zeroOutFunc(weight)}")
            weightSum += actualWeight
            val mag = vector.norm()
            vectorSum += vector / mag * actualWeight
            magSum += mag * actualWeight
        }
        if (vectorSum epsilonEquals Vector2d()) vectorSum += Vector2d(EPSILON)
        val vector = vectorSum / vectorSum.norm() * magSum / weightSum
        return vector
        for (obstacle in obstacles) {
            val result = obstacle.combineWith(phi, pathVec)
            if (result != null) return result
        }
        return pathVec
    }

    override fun get(x: Double, y: Double): Vector2d = compute(internalGet(Query(Vector2d(x, y))))
}

class CircularGVF(
    val center: Vector2d,
    val radius: Double,
    override val kN: Double,
    override val errorMapFunc: (Double) -> Double = { it },
) : GuidingVectorField {
    override val startPosition: Vector2d? = null
    override val endPosition: Vector2d? = null

    override fun internalGet(query: Query): Query.Phi {
        val centerToPoint = query.point - center
        val normalized = centerToPoint / centerToPoint.norm()
        return query.Phi(
            normalized * radius + center,
            Vector2d(-normalized.y, normalized.x)
        )
    }
}

class PositionPathGVF(
    val path: PositionPath,
    override val kN: Double,
    override val errorMapFunc: (Double) -> Double = { it }
) : GuidingVectorField {
    companion object {
        @JvmStatic
        fun createRecticircle(center: Vector2d, dimensions: Vector2d, radius: Double): PositionPath {
            val radii = dimensions * 0.5
            return PositionPath(
                listOf(
                    LineSegment(
                        center + Vector2d(radii.x + radius, -radii.y),
                        center + Vector2d(radii.x + radius, radii.y)
                    ),
                    CircularArc(center + radii, radius, 0.rad, (0.5 * PI).rad),
                    LineSegment(
                        center + Vector2d(radii.x, radii.y + radius),
                        center + Vector2d(-radii.x, radii.y + radius)
                    ),
                    CircularArc(center + Vector2d(-radii.x, radii.y), radius, (0.5 * PI).rad, PI.rad),
                    LineSegment(
                        center + Vector2d(-radii.x - radius, radii.y),
                        center + Vector2d(-radii.x - radius, -radii.y)
                    ),
                    CircularArc(center - radii, radius, PI.rad, (1.5 * PI).rad),
                    LineSegment(
                        center + Vector2d(-radii.x, -radii.y - radius),
                        center + Vector2d(radii.x, -radii.y - radius)
                    ),
                    CircularArc(center + Vector2d(radii.x, -radii.y), radius, (-0.5 * PI).rad, 0.rad),
                )
            )
        }
    }

    override val startPosition: Vector2d = path.start()
    override val endPosition: Vector2d = path.end()

    override fun internalGet(query: Query): Query.Phi {
        val (t, segment) = path.compositeProject(query.point)
        return query.Phi(
            segment.internalGet(t),
            segment.internalDeriv(t).let { it / it.norm() }
        )
    }
}