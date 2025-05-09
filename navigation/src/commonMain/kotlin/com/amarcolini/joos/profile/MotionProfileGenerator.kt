package com.amarcolini.joos.profile

import com.amarcolini.joos.util.DoubleProgression
import com.amarcolini.joos.util.epsilonEquals
import com.amarcolini.joos.util.minus
import com.amarcolini.joos.util.solveQuadratic
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.*

/**
 * Motion profile generator with arbitrary start and end motion states and either dynamic constraints or jerk limiting.
 */
@JsExport
@Suppress("LargeClass", "ComplexMethod", "NestedBlockDepth")
object MotionProfileGenerator {

    /**
     * Generates a simple motion profile with constant [maxVel], [maxAccel], and [maxJerk]. If [maxJerk] is zero, an
     * acceleration-limited profile will be generated instead of a jerk-limited one. If constraints can't be obeyed,
     * there are two possible fallbacks: If [overshoot] is true, then two profiles will be concatenated (the first one
     * overshoots the goal and the second one reverses back to reach the goal). Otherwise, the highest order constraint
     * (e.g., max jerk for jerk-limited profiles) is repeatedly violated until the goal is achieved.
     *
     * @param start start motion state
     * @param goal goal motion state
     * @param maxVel maximum velocity
     * @param maxAccel maximum acceleration
     * @param maxJerk maximum jerk
     * @param overshoot if true overshoot otherwise violate constraints (see description above)
     */
    @Suppress("LongParameterList")
    @JvmStatic
    @JvmOverloads
    fun generateSimpleMotionProfile(
        start: MotionState,
        goal: MotionState,
        maxVel: Double,
        maxAccel: Double,
        maxJerk: Double = 0.0,
        overshoot: Boolean = false
    ): MotionProfile {
        // ensure the goal is always after the start; plan the flipped profile otherwise
        if (goal.x < start.x) {
            return generateSimpleMotionProfile(
                start.flipped(),
                goal.flipped(),
                maxVel,
                maxAccel,
                maxJerk
            ).flipped()
        }

        if (maxJerk epsilonEquals 0.0) {
            // acceleration-limited profile (trapezoidal)
            val requiredAccel = (goal.v * goal.v - start.v * start.v) / (2 * (goal.x - start.x))

            val accelProfile = generateAccelProfile(start, maxVel, maxAccel)
            val decelProfile = generateAccelProfile(
                MotionState(
                    goal.x,
                    goal.v,
                    goal.a,
                    goal.j
                ),
                -maxVel,
                maxAccel,
                -maxJerk
            ).reversed()

            val noCoastProfile = accelProfile + decelProfile
            val remainingDistance = goal.x - noCoastProfile.end().x

            if (remainingDistance >= 0.0) {
                // normal 3-segment profile works
                val deltaT2 = remainingDistance / maxVel

                return MotionProfileBuilder(start)
                    .appendProfile(accelProfile)
                    .appendAccelerationControl(0.0, deltaT2)
                    .appendProfile(decelProfile)
                    .build()
            } else if (abs(requiredAccel) > maxAccel) {
                return if (overshoot) {
                    // TODO: is this most efficient? (do we care?)
                    noCoastProfile + generateSimpleMotionProfile(
                        noCoastProfile.end(),
                        goal,
                        maxVel,
                        maxAccel,
                        overshoot = true
                    )
                } else {
                    // single segment profile
                    val dt = (goal.v - start.v) / requiredAccel
                    MotionProfileBuilder(start)
                        .appendAccelerationControl(requiredAccel, dt)
                        .build()
                }
            } else if (start.v > maxVel && goal.v > maxVel) {
                // decel, accel
                val roots = solveQuadratic(
                    -maxAccel,
                    2 * start.v,
                    (goal.v * goal.v - start.v * start.v) / (2 * maxAccel) - goal.x + start.x
                )
                val deltaT1 = roots.filter { it >= 0.0 }.minOrNull()!!
                val deltaT3 = abs(start.v - goal.v) / maxAccel + deltaT1

                return MotionProfileBuilder(start)
                    .appendAccelerationControl(-maxAccel, deltaT1)
                    .appendAccelerationControl(maxAccel, deltaT3)
                    .build()
            } else {
                // accel, decel
                val roots = solveQuadratic(
                    maxAccel,
                    2 * start.v,
                    (start.v * start.v - goal.v * goal.v) / (2 * maxAccel) - goal.x + start.x
                )
                val deltaT1 = roots.filter { it >= 0.0 }.minOrNull()!!
                val deltaT3 = abs(start.v - goal.v) / maxAccel + deltaT1

                return MotionProfileBuilder(start)
                    .appendAccelerationControl(maxAccel, deltaT1)
                    .appendAccelerationControl(-maxAccel, deltaT3)
                    .build()
            }
        } else {
            // jerk-limited profile (S-curve)
            val accelerationProfile = generateAccelProfile(start, maxVel, maxAccel, maxJerk)
            // we leverage symmetry here; deceleration profiles are just reversed acceleration ones with the goal
            // acceleration flipped
            val decelerationProfile = generateAccelProfile(
                MotionState(
                    goal.x,
                    goal.v,
                    goal.a,
                    -goal.j
                ),
                -maxVel,
                maxAccel,
                maxJerk
            ).reversed()
            println(decelerationProfile)

            val noCoastProfile = accelerationProfile + decelerationProfile
            val remainingDistance = goal.x - noCoastProfile.end().x
            println(remainingDistance)

            if (remainingDistance >= 0.0) {
                // we just need to add a coast segment of appropriate duration
                val deltaT4 = remainingDistance / maxVel

                return MotionProfileBuilder(start)
                    .appendProfile(accelerationProfile)
                    .appendJerkControl(0.0, deltaT4)
                    .appendProfile(decelerationProfile)
                    .build()
            } else {
                // the profile never reaches maxV
                // thus, we need to compute the peak velocity (0 < peak vel < max vel)
                // we *could* construct a large polynomial expression (i.e., a nasty cubic) and solve it using Cardano's
                // method, some kind of inclusion method like modified Anderson-Bjorck-King, or a host of other methods
                // (see https://link.springer.com/content/pdf/bbm%3A978-3-642-05175-3%2F1.pdf for modified ABK)
                // instead, however, we conduct a binary search as it's sufficiently performant for this use case,
                // requires less code, and is overall significantly more comprehensible
                var upperBound = maxVel
                var lowerBound = 0.0
                var iterations = 0
                while (iterations < 1000) {
                    val peakVel = (upperBound + lowerBound) / 2

                    val searchAccelProfile = generateAccelProfile(start, peakVel, maxAccel, maxJerk)
                    val searchDecelProfile = generateAccelProfile(goal, peakVel, maxAccel, maxJerk)
                        .reversed()

                    val searchProfile = searchAccelProfile + searchDecelProfile

                    val error = goal.x - searchProfile.end().x

                    if (error epsilonEquals 0.0) {
                        return searchProfile
                    }

                    if (error > 0.0) {
                        // we undershot so shift the lower bound up
                        lowerBound = peakVel
                    } else {
                        // we overshot so shift the upper bound down
                        upperBound = peakVel
                    }

                    iterations++
                }

                // constraints are not satisfiable
                return if (overshoot) {
                    noCoastProfile + generateSimpleMotionProfile(
                        noCoastProfile.end(),
                        goal,
                        maxVel,
                        maxAccel,
                        maxJerk,
                        overshoot = true
                    )
                } else {
                    // violate max jerk first
                    generateSimpleMotionProfile(
                        start,
                        goal,
                        maxVel,
                        maxAccel,
                        overshoot = false
                    )
                }
            }
        }
    }

    private fun generateAccelProfile(
        start: MotionState,
        maxVel: Double,
        maxAccel: Double,
        maxJerk: Double = 0.0
    ): MotionProfile =
        if (maxJerk epsilonEquals 0.0) {
            // acceleration-limited
            val deltaT1 = abs(start.v - maxVel) / maxAccel
            val builder = MotionProfileBuilder(start)
            if (start.v > maxVel) {
                // we need to decelerate
                builder.appendAccelerationControl(-maxAccel, deltaT1)
            } else {
                builder.appendAccelerationControl(maxAccel, deltaT1)
            }
            builder.build()
        } else {
            // jerk-limited
            // compute the duration and velocity of the first segment
            val (deltaT1, deltaV1) = if (start.a > maxAccel) {
                // slow down and see where we are
                val deltaT1 = (start.a - maxAccel) / maxJerk
                val deltaV1 = start.a * deltaT1 - 0.5 * maxJerk * deltaT1 * deltaT1
                Pair(deltaT1, deltaV1)
            } else {
                // otherwise accelerate
                val deltaT1 = (maxAccel - start.a) / maxJerk
                val deltaV1 = start.a * deltaT1 + 0.5 * maxJerk * deltaT1 * deltaT1
                Pair(deltaT1, deltaV1)
            }

            // compute the duration and velocity of the third segment
            val deltaT3 = maxAccel / maxJerk
            val deltaV3 = maxAccel * deltaT3 - 0.5 * maxJerk * deltaT3 * deltaT3

            // compute the velocity change required in the second segment
            val deltaV2 = maxVel - start.v - deltaV1 - deltaV3

            if (deltaV2 < 0.0) {
                // there is no constant acceleration phase
                // the second case checks if we're going to exceed max vel
                if (start.a > maxAccel || (start.v - maxVel) > (start.a * start.a) / (2 * maxJerk)) {
                    // problem: we need to cut down on our acceleration but we can't cut our initial decel
                    // solution: we'll lengthen our initial decel to -max accel and similarly with our final accel
                    // if this results in an over correction, decel instead to a good accel
                    val newDeltaT1 = (start.a + maxAccel) / maxJerk
                    val newDeltaV1 = start.a * newDeltaT1 - 0.5 * maxJerk * newDeltaT1 * newDeltaT1

                    val newDeltaV2 = maxVel - start.v - newDeltaV1 + deltaV3

                    if (newDeltaV2 > 0.0) {
                        // we decelerated too much
                        val roots = solveQuadratic(
                            -maxJerk,
                            2 * start.a,
                            start.v - maxVel - start.a * start.a / (2 * maxJerk)
                        )
                        val finalDeltaT1 = roots.filter { it >= 0.0 }.minOrNull()!!
                        val finalDeltaT3 = finalDeltaT1 - start.a / maxJerk

                        MotionProfileBuilder(start)
                            .appendJerkControl(-maxJerk, finalDeltaT1)
                            .appendJerkControl(maxJerk, finalDeltaT3)
                            .build()
                    } else {
                        // we're almost good
                        val newDeltaT2 = newDeltaV2 / -maxAccel

                        MotionProfileBuilder(start)
                            .appendJerkControl(-maxJerk, newDeltaT1)
                            .appendJerkControl(0.0, newDeltaT2)
                            .appendJerkControl(maxJerk, deltaT3)
                            .build()
                    }
                } else {
                    // cut out the constant accel phase and find a shorter delta t1 and delta t3
                    val roots = solveQuadratic(
                        maxJerk,
                        2 * start.a,
                        start.v - maxVel + start.a * start.a / (2 * maxJerk)
                    )
                    val newDeltaT1 = roots.filter { it >= 0.0 }.minOrNull()!!
                    val newDeltaT3 = newDeltaT1 + start.a / maxJerk

                    MotionProfileBuilder(start)
                        .appendJerkControl(maxJerk, newDeltaT1)
                        .appendJerkControl(-maxJerk, newDeltaT3)
                        .build()
                }
            } else {
                // there is a constant acceleration phase
                val deltaT2 = deltaV2 / maxAccel

                val builder = MotionProfileBuilder(start)
                if (start.a > maxAccel) {
                    builder.appendJerkControl(-maxJerk, deltaT1)
                } else {
                    builder.appendJerkControl(maxJerk, deltaT1)
                }
                builder.appendJerkControl(0.0, deltaT2)
                    .appendJerkControl(-maxJerk, deltaT3)
                    .build()
            }
        }

    /**
     * Generates a motion profile with dynamic maximum velocity and acceleration. Uses the algorithm described in
     * section 3.2 of [Sprunk2008.pdf](http://www2.informatik.uni-freiburg.de/~lau/students/Sprunk2008.pdf). **Warning:**
     * Profiles may be generated incorrectly if the endpoint velocity/acceleration values preclude the obedience of the
     * motion constraints. To protect against this, verify the continuity of the generated profile or keep the start and
     * goal velocities at 0.
     *
     * @param start start motion state
     * @param goal goal motion state
     * @param velocityConstraint velocity constraints
     * @param accelerationConstraint acceleration constraints
     * @param resolution separation between constraint samples
     */
    @JvmStatic
    @JvmOverloads
    fun generateMotionProfile(
        start: MotionState,
        goal: MotionState,
        velocityConstraint: VelocityConstraint,
        accelerationConstraint: AccelerationConstraint,
        decelerationConstraint: AccelerationConstraint = accelerationConstraint,
        resolution: Double = 0.25
    ): MotionProfile {
        if (goal.x < start.x) {
            return generateMotionProfile(
                start.flipped(),
                goal.flipped(),
                { s, ds -> velocityConstraint[-s, -ds] },
                { s, ds, lastVel -> accelerationConstraint[-s, -ds, -lastVel] },
                { s, ds, lastVel -> decelerationConstraint[-s, -ds, -lastVel] },
                resolution
            ).flipped()
        }

        val length = goal.x - start.x
        // ds is an adjusted resolution that fits nicely within length
        val samples = max(2, ceil(length / resolution).toInt())

        val s = DoubleProgression.fromClosedInterval(0.0, length, samples)
        // compute initial velocity constraints
        val velocityConstraints =
            (s + start.x).map {
                velocityConstraint[it, s.step]
            }

        // compute the forward states
        val forwardStates = forwardPass(
            start,
            s + start.x,
            velocityConstraints,
            accelerationConstraint
        ).toMutableList()

        // compute the backward states
        val backwardStates = forwardPass(
            goal,
            goal.x - s,
            velocityConstraints.reversed(),
            decelerationConstraint,
        ).map { (motionState, ds) ->
            Pair(afterDisplacement(motionState, ds), ds)
        }.map { (motionState, ds) ->
            Pair(
                MotionState(
                    abs(motionState.x),
                    motionState.v,
                    motionState.a
                ),
                -ds
            )
        }.reversed().toMutableList()

        forwardStates.add(goal to 0.0)
        backwardStates.add(goal to 0.0)

        // merge the forward and backward states
        val finalStates = mutableListOf<Pair<MotionState, Double>>()

        var i = 0
        while (i < forwardStates.size && i < backwardStates.size) {
            // retrieve the start states and displacement deltas
            var (forwardStartState, forwardDx) = forwardStates[i]
            var (backwardStartState, backwardDx) = backwardStates[i]

            // if there's a discrepancy in the displacements, split the longer chunk in two and add the second
            // to the corresponding list; this guarantees that segments are always aligned
            if (!(forwardDx epsilonEquals backwardDx)) {
                if (forwardDx > backwardDx) {
                    // forward longer
                    forwardStates.add(
                        i + 1,
                        Pair(
                            afterDisplacement(forwardStartState, backwardDx),
                            forwardDx - backwardDx
                        )
                    )
                    forwardDx = backwardDx
                } else {
                    // backward longer
                    backwardStates.add(
                        i + 1,
                        Pair(
                            afterDisplacement(backwardStartState, forwardDx),
                            backwardDx - forwardDx
                        )
                    )
                    backwardDx = forwardDx
                }
            }

            // compute the end states (after alignment)
            val forwardEndState = afterDisplacement(forwardStartState, forwardDx)
            val backwardEndState = afterDisplacement(backwardStartState, backwardDx)

            if (forwardStartState.v <= backwardStartState.v) {
                // forward start lower
                if (forwardEndState.v <= backwardEndState.v) {
                    // forward end lower
                    finalStates.add(Pair(forwardStartState, forwardDx))
                } else {
                    // backward end lower
                    val intersection = intersection(
                        forwardStartState,
                        backwardStartState
                    )
                    finalStates.add(Pair(forwardStartState, intersection))
                    finalStates.add(
                        Pair(
                            afterDisplacement(backwardStartState, intersection),
                            backwardDx - intersection
                        )
                    )
                }
            } else {
                // backward start lower
                if (forwardEndState.v >= backwardEndState.v) {
                    // backward end lower
                    finalStates.add(Pair(backwardStartState, backwardDx))
                } else {
                    // forward end lower
                    val intersection = intersection(
                        forwardStartState,
                        backwardStartState
                    )
                    finalStates.add(Pair(backwardStartState, intersection))
                    finalStates.add(
                        Pair(
                            afterDisplacement(forwardStartState, intersection),
                            forwardDx - intersection
                        )
                    )
                }
            }
            i++
        }

        // turn the final states into actual time-parameterized motion segments
        val motionSegments = mutableListOf<MotionSegment>()
        for ((state, stateDx) in finalStates.dropLast(1)) {
            val dt = if (state.a epsilonEquals 0.0) {
                stateDx / state.v
            } else {
                val discriminant = state.v * state.v + 2 * state.a * stateDx
                if (discriminant epsilonEquals 0.0) {
                    -state.v / state.a
                } else {
                    val positive = (sqrt(discriminant) - state.v) / state.a
                    val negative = (-sqrt(discriminant) - state.v) / state.a
                    if (positive >= 0) positive
                    else negative
                }
            }
            motionSegments.add(MotionSegment(state, dt))
        }

        return MotionProfile(motionSegments)
    }

    // execute a forward pass that consists of applying maximum acceleration starting at min(last velocity, max vel)
    // on a segment-by-segment basis
    private fun forwardPass(
        start: MotionState,
        displacements: DoubleProgression,
        velocityConstraints: List<Double>,
        accelerationConstraint: AccelerationConstraint,
    ): List<Pair<MotionState, Double>> {
        // List of forward states as pairs of a motion state and ds.
        val forwardStates = mutableListOf<Pair<MotionState, Double>>()

        val ds = displacements.step

        var lastState = start
        displacements
            .zip(velocityConstraints)
            .dropLast(1)
            .forEach { (displacement, maxVel) ->
                lastState = if (lastState.v >= maxVel) {
                    // the last velocity exceeds max vel so we just coast
                    val state = MotionState(displacement, maxVel, 0.0)
                    forwardStates.add(Pair(state, ds))
                    afterDisplacement(state, ds)
                } else {
                    // compute the final velocity assuming max accel
                    val finalVel =
                        accelerationConstraint[displacement, abs(ds), lastState.v]
                    val accel = (finalVel * finalVel - lastState.v * lastState.v) /
                            (2 * ds)
                    if (finalVel <= maxVel) {
                        // we're still under max vel so we're good
                        val state = MotionState(displacement, lastState.v, accel)
                        forwardStates.add(Pair(state, ds))
                        afterDisplacement(state, ds)
                    } else {
                        // we went over max vel so now we split the segment
                        val accelDx =
                            (maxVel * maxVel - lastState.v * lastState.v) / (2 * accel)
                        val accelState = MotionState(displacement, lastState.v, accel)
                        val coastState = MotionState(displacement + accelDx, maxVel, 0.0)
                        forwardStates.add(Pair(accelState, accelDx))
                        forwardStates.add(Pair(coastState, ds - accelDx))
                        afterDisplacement(coastState, ds - accelDx)
                    }
                }
            }

        return forwardStates
    }

    private fun afterDisplacement(state: MotionState, ds: Double): MotionState {
        val discriminant = state.v * state.v + 2 * state.a * ds
        return if (discriminant epsilonEquals 0.0) {
            MotionState(state.x + ds, 0.0, state.a)
        } else {
            MotionState(state.x + ds, sqrt(discriminant), state.a)
        }
    }

    private fun intersection(state1: MotionState, state2: MotionState): Double {
        return (state1.v * state1.v - state2.v * state2.v) / (2 * state2.a - 2 * state1.a)
    }
}