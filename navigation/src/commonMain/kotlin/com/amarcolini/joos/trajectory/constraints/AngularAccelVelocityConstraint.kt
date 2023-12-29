package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.util.epsilonEquals
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
                    val thresh1 = sqrt((8 * currentCurvature * aW * ds) / (currentCurvature + lastCurvature).pow(2))
                    val tmp1 = sqrt(
                        (4 * currentCurvature * ds * (currentCurvature * aT + aW)) /
                                (lastCurvature - currentCurvature).pow(2)
                    )
                    val tmp2 = sqrt(
                        (2 * ds * (currentCurvature * aT + aW).pow(2)) /
                                ((lastCurvature - currentCurvature) * (2 * aW + (currentCurvature + lastCurvature) * aT))
                    )
                    val threshTmp1 = min(tmp1, tmp2)
                    val threshTmp2 = min(sqrt((2 * aW * ds) / lastCurvature), sqrt(2 * aT * ds))
                    var threshTmp3 = Double.NEGATIVE_INFINITY
                    val tmp = min(
                        (2 * aW * ds) / lastCurvature,
                        (2 * ds * (currentCurvature * aT - aW).pow(2)) / ((lastCurvature - currentCurvature) * (2 * aW - (lastCurvature + currentCurvature) * aT))
                    )
                    if (tmp > (-4 * currentCurvature * ds * (currentCurvature * aT - aW)) / ((lastCurvature - currentCurvature) * (lastCurvature + currentCurvature)) && tmp > 2 * aT * ds)
                        threshTmp3 = sqrt(tmp)
                    max(max(thresh1, threshTmp1), max(threshTmp2, threshTmp3))
                }

                else -> Double.POSITIVE_INFINITY
            }

            currentCurvature < 0 && lastCurvature <= 0 -> when {
                currentCurvature > lastCurvature -> {
                    val thresh1 = sqrt((-8 * currentCurvature * aW * ds) / (currentCurvature + lastCurvature).pow(2))
                    val tmp1 = sqrt(
                        (-4 * currentCurvature * ds * (aW - currentCurvature * aT)) /
                                ((lastCurvature + currentCurvature) * (lastCurvature - currentCurvature))
                    )
                    val tmp2 = sqrt(
                        (-2 * ds * (aW - currentCurvature * aT).pow(2)) /
                                ((lastCurvature - currentCurvature) * (2 * aW - (currentCurvature + lastCurvature) * aT))
                    )
                    val threshTmp1 = min(tmp1, tmp2)
                    val threshTmp2 = min(sqrt((-2 * aW * ds) / lastCurvature), sqrt(2 * aT * ds))
                    var threshTmp3 = Double.NEGATIVE_INFINITY
                    val tmp = min(
                        (-2 * aW * ds) / lastCurvature,
                        (-2 * ds * (aW + currentCurvature * aT).pow(2)) / ((lastCurvature - currentCurvature) * (2 * aW + (lastCurvature + currentCurvature) * aT))
                    )
                    if (tmp > (-4 * currentCurvature * ds * (aW + currentCurvature * aT)) / ((lastCurvature - currentCurvature) * (lastCurvature + currentCurvature)) && tmp > 2 * aT * ds)
                        threshTmp3 = sqrt(tmp)
                    max(max(thresh1, threshTmp1), max(threshTmp2, threshTmp3))
                }

                currentCurvature < lastCurvature -> sqrt(
                    (-2 * ds * (aW - aT * currentCurvature).pow(2)) /
                            ((aT * (currentCurvature + lastCurvature) - 2 * aW) * (lastCurvature - currentCurvature))
                )

                else -> Double.POSITIVE_INFINITY
            }

            currentCurvature < 0 && lastCurvature > 0 -> {
                val v2starpos = sqrt((2 * ds * aW) / lastCurvature)
                val precond = if (lastCurvature + currentCurvature < 0)
                    sqrt(
                        (-4 * currentCurvature * ds * (currentCurvature * aT - aW)) /
                                ((lastCurvature + currentCurvature) * (lastCurvature - currentCurvature))
                    )
                else Double.POSITIVE_INFINITY
                val threshTmp = max(
                    min(
                        precond, sqrt(
                            (-2 * ds * (aT * currentCurvature - aW).pow(2)) /
                                    ((aT * (lastCurvature + currentCurvature) - 2 * aW) * (lastCurvature - currentCurvature))
                        )
                    ),
                    sqrt(2 * ds * aT)
                )
                min(threshTmp, v2starpos)
            }

            currentCurvature > 0 && lastCurvature < 0 -> {
                val v1starpos = sqrt((-2 * ds * aW) / lastCurvature)
                val precond = if (lastCurvature + currentCurvature > 0)
                    sqrt(
                        (-4 * currentCurvature * ds * (currentCurvature * aT + aW)) /
                                ((lastCurvature + currentCurvature) * (lastCurvature - currentCurvature))
                    )
                else Double.POSITIVE_INFINITY
                val threshTmp = max(
                    min(
                        precond, sqrt(
                            (-2 * ds * (aT * currentCurvature + aW).pow(2)) /
                                    ((aT * (lastCurvature + currentCurvature) + 2 * aW) * (lastCurvature - currentCurvature))
                        )
                    ),
                    sqrt(2 * ds * aT)
                )
                min(threshTmp, v1starpos)
            }

            currentCurvature epsilonEquals 0.0 -> {
                when {
                    lastCurvature > 0 -> {
                        val v2hatpos = sqrt((2 * ds * aW) / lastCurvature)
                        val threshTmp = max(
                            sqrt(2 * ds * aT),
                            sqrt(
                                (-2 * ds * aW * aW) / (lastCurvature * (lastCurvature * aT - 2 * aW))
                            )
                        )
                        min(v2hatpos, threshTmp)
                    }

                    lastCurvature < 0 -> {
                        val v1hatpos = sqrt(-(2 * ds * aW) / lastCurvature)
                        val threshTmp = max(
                            sqrt(2 * ds * aT),
                            sqrt((-2 * ds * aW * aW) / (lastCurvature * (lastCurvature * aT + 2 * aW)))
                        )
                        min(v1hatpos, threshTmp)
                    }

                    else -> Double.POSITIVE_INFINITY
                }
            }

            else -> Double.POSITIVE_INFINITY
        }
    }
}