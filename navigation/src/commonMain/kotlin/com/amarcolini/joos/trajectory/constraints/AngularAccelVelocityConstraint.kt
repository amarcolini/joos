package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A velocity constraint that prevents over-accelerating to ensure its corresponding angular and translational acceleration
 * constraints are satisfied.
 */
class AngularAccelVelocityConstraint(
    val maxAngAccel: Angle,
    val maxTranslationAccel: Double
) : TrajectoryVelocityConstraint {
    /**
     * Constructs an [AngularAccelVelocityConstraint] where [maxAngAccel] is in degrees or radians as specified by
     * [Angle.defaultUnits].
     */
    constructor(maxAngAccel: Double, maxTranslationalAccel: Double) : this(Angle(maxAngAccel), maxTranslationalAccel)

    private val aW = maxAngAccel.radians
    private val aT = maxTranslationAccel
    override fun get(pose: Pose2d, deriv: Pose2d, lastDeriv: Pose2d, ds: Double, baseRobotVel: Pose2d): Double {
        val currentCurvature = deriv.heading.radians
        val lastCurvature = lastDeriv.heading.radians

        if (currentCurvature == lastCurvature) return Double.POSITIVE_INFINITY

        // Confused? See http://www2.informatik.uni-freiburg.de/~lau/students/Sprunk2008.pdf#section.B.2
        return when {
            currentCurvature > 0 && lastCurvature >= 0 -> when {
                currentCurvature > lastCurvature -> sqrt(
                    (2 * ds * (aW + aT * currentCurvature).pow(2)) /
                            ((aT * (currentCurvature + lastCurvature) + 2 * aW) * (currentCurvature - lastCurvature))
                )
                currentCurvature < lastCurvature -> {
                    val thresh1 = (8 * currentCurvature * aW * ds) / (currentCurvature + lastCurvature).pow(2)
                    val tmp1 = (4 * currentCurvature * ds * (currentCurvature * aT + aW)) /
                            (lastCurvature - currentCurvature).pow(2)
                    val tmp2 = (2 * ds * (currentCurvature * aT + aW).pow(2)) /
                            ((lastCurvature - currentCurvature) * (2 * aW + (currentCurvature + lastCurvature) * aT))
                    val thresh2 = min(tmp1, tmp2)
                    val tmp3 = (2 * aW * ds) / lastCurvature
                    val thresh3 = min(tmp3, 2 * aT * ds)
                    val tmp4 = min(
                        tmp3, (2 * ds * (currentCurvature * aT - aW).pow(2)) /
                                ((lastCurvature - currentCurvature) * (2 * aW - (currentCurvature + lastCurvature) * aT))
                    )
                    val thresh4 =
                        if (tmp4 > (-4 * currentCurvature * ds * (aW + currentCurvature * aT)) / ((lastCurvature - currentCurvature) * (lastCurvature + currentCurvature)) && tmp4 > 2 * aT * ds)
                            tmp4 else Double.NEGATIVE_INFINITY
                    sqrt(max(max(thresh1, thresh2), max(thresh3, thresh4)))
                }
                else -> Double.POSITIVE_INFINITY
            }
            currentCurvature < 0 && lastCurvature <= 0 -> when {
                currentCurvature > lastCurvature -> {
                    val thresh1 = (-8 * currentCurvature * aW * ds) / (currentCurvature + lastCurvature).pow(2)
                    val tmp1 = (-4 * currentCurvature * ds * (aW - currentCurvature * aT)) /
                            ((lastCurvature + currentCurvature) * (lastCurvature - currentCurvature))
                    val tmp2 = (-2 * ds * (aW - currentCurvature * aT).pow(2)) /
                            ((lastCurvature - currentCurvature) * (2 * aW - (currentCurvature + lastCurvature) * aT))
                    val thresh2 = min(tmp1, tmp2)
                    val tmp3 = (-2 * aW * ds) / lastCurvature
                    val thresh3 = min(tmp3, 2 * aT * ds)
                    val tmp4 = min(
                        tmp3, (-2 * ds * (currentCurvature * aT + aW).pow(2)) /
                                ((lastCurvature - currentCurvature) * (2 * aW + (currentCurvature + lastCurvature) * aT))
                    )
                    val thresh4 =
                        if (tmp4 > (-4 * currentCurvature * ds * (aW - currentCurvature * aT)) / ((lastCurvature - currentCurvature) * (lastCurvature + currentCurvature)) && tmp4 > 2 * aT * ds)
                            tmp4 else Double.NEGATIVE_INFINITY
                    sqrt(max(max(thresh1, thresh2), max(thresh3, thresh4)))
                }
                currentCurvature < lastCurvature -> sqrt(
                    (-2 * ds * (aW - aT * currentCurvature).pow(2)) /
                            ((aT * (currentCurvature + lastCurvature) - 2 * aW) * (lastCurvature - currentCurvature))
                )
                else -> Double.POSITIVE_INFINITY
            }
            currentCurvature < 0 && lastCurvature > 0 -> {
                val v2starpos = (2 * ds * aW) / lastCurvature
                val precond = if (lastCurvature + currentCurvature < 0)
                    (-4 * currentCurvature * ds * (currentCurvature * aT - aW)) /
                            ((lastCurvature + currentCurvature) * (lastCurvature - currentCurvature))
                else Double.POSITIVE_INFINITY
                val tmpThresh = max(
                    min(
                        precond, (-2 * ds * (aT * currentCurvature - aW).pow(2)) /
                                ((aT * (currentCurvature + lastCurvature) - 2 * aW) * (currentCurvature - lastCurvature))
                    ),
                    2 * ds * aT
                )
                sqrt(min(tmpThresh, v2starpos))
            }
            currentCurvature > 0 && lastCurvature < 0 -> {
                val v1starpos = (-2 * ds * aW) / lastCurvature
                val precond = if (lastCurvature + currentCurvature > 0)
                    (-4 * currentCurvature * ds * (currentCurvature * aT + aW)) /
                            ((lastCurvature + currentCurvature) * (lastCurvature - currentCurvature))
                else Double.POSITIVE_INFINITY
                val tmpThresh = max(
                    min(
                        precond, (-2 * ds * (aT * currentCurvature + aW).pow(2)) /
                                ((aT * (currentCurvature + lastCurvature) + 2 * aW) * (currentCurvature - lastCurvature))
                    ),
                    2 * ds * aT
                )
                sqrt(min(tmpThresh, v1starpos))
            }
            currentCurvature == 0.0 -> {
                when {
                    lastCurvature > 0 -> {
                        val v2hatpos = (2 * ds * aW) / lastCurvature
                        val tmpThresh = max(
                            2 * ds * aT,
                            (-2 * ds * aW * aW) / (lastCurvature * aT - 2 * aW)
                        )
                        sqrt(min(v2hatpos, tmpThresh))
                    }
                    lastCurvature < 0 -> {
                        val v1hatpos = -(2 * ds * aW) / lastCurvature
                        val tmpThresh = max(
                            2 * ds * aT,
                            (-2 * ds * aW * aW) / (lastCurvature * aT + 2 * aW)
                        )
                        sqrt(min(v1hatpos, tmpThresh))
                    }
                    else -> Double.POSITIVE_INFINITY
                }
            }
            else -> Double.POSITIVE_INFINITY
        }
    }
}