package com.amarcolini.joos.followers

import com.amarcolini.joos.control.PIDCoefficients
import com.amarcolini.joos.control.PIDController
import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.AngleUnit
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.util.*
import kotlin.js.JsExport
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * State-of-the-art path follower for holonomic drives based on the [GuidingVectorField].
 *
 * @param maxVel maximum velocity
 * @param maxAccel maximum acceleration
 * @param maxDecel maximum deceleration
 * @param admissibleError admissible/satisfactory pose error at the end of each move
 * @param kN normal vector weight (see [GuidingVectorField])
 * @param kOmega proportional direction velocity gain
 * @param pidCoeffs heading PID coefficients
 * @param errorMapFunc error map function (see [GuidingVectorField])
 * @param clock clock
 */
@JsExport
class HolonomicGVFFollower @JvmOverloads constructor(
    @JvmField var maxVel: Double,
    @JvmField var maxAccel: Double,
    @JvmField var maxDecel: Double,
    @JvmField var maxAngVel: Angle,
    @JvmField var maxAngAccel: Angle,
    admissibleError: Pose2d,
    @JvmField var kN: Double,
    @JvmField var kOmega: Double,
    @JvmField var kX: Double,
    @JvmField var kY: Double,
    @JvmField val pidCoeffs: PIDCoefficients,
    @JvmField var correctionDistance: Double = 5.0,
    @JvmField var useCurvatureControl: Boolean = false,
    private val errorMapFunc: (Double) -> Double = { it },
    clock: NanoClock = NanoClock.system
) : PathFollower(admissibleError, clock) {
    private lateinit var gvf: FollowableGVF
    private var lastUpdateTimestamp: Double = 0.0
    private var lastVel: Double = 0.0
    private var lastPosition: Vector2d? = null
    private var lastAngVel: Angle = 0.rad

    override var lastError: Pose2d = Pose2d()

    override fun followPath(path: Path) {
        gvf = PathGVF(path, kN, errorMapFunc)
        init()
        super.followPath(path)
    }

    fun followGVF(gvf: FollowableGVF) {
        gvf.reset()
        this.gvf = gvf
        init()
        super.followPath(gvf.path)
    }

    private fun init() {
        lastUpdateTimestamp = clock.seconds()
        lastVel = 0.0
        lastAngVel = 0.rad
        lastPosition = null
        lastDesiredHeading = null
    }

    private var lastDesiredHeading: Angle? = null
    private val headingController = PIDController(
        pidCoeffs, clock = clock
    ).apply {
        setInputBounds(-PI, PI)
    }

    override fun internalUpdate(currentPose: Pose2d, currentRobotVel: Pose2d?): DriveSignal {
        val output =
            gvf.internalGet(GuidingVectorField.Query(currentPose.vec()))
        val gvfResult = gvf.compute(output)

        val desiredHeading =
            Angle(
                atan2(gvfResult.y, gvfResult.x),
                AngleUnit.Radians
            )
        val pathEnd = path.end()
        val pathTarget = path[gvf.lastProjectDisplacement]
        val remainingDistance = (currentPose.vec() distTo pathEnd.vec())
        headingController.targetPosition = pathTarget.heading.radians

        val timestamp = clock.seconds()
        val dt = timestamp - lastUpdateTimestamp

        // ref eqs. (18), (23), and (24)
        val lastDesiredHeading = lastDesiredHeading
        val desiredOmega =
            lastDesiredHeading?.let { (desiredHeading - it).normDelta() / dt }
        val omega = if (desiredOmega != null) {
            (if (remainingDistance > correctionDistance) desiredOmega * kOmega else 0.rad)
//            + headingController.update(currentPose.heading.radians).rad
        } else 0.rad
        this.lastDesiredHeading = desiredHeading

        // basic online motion profiling
        val maxVelToStop = sqrt(2 * maxDecel * remainingDistance)
        val maxVelFromLast = lastVel + maxAccel * dt
        val maxAngVelFromLast =
            if (desiredOmega != null) lastAngVel + maxAngAccel * dt * sign(desiredOmega - lastAngVel)
            else 0.rad
        val angVel = if (maxAngVelFromLast >= 0.rad) minOf(maxAngVelFromLast, maxAngVel)
        else maxOf(maxAngVelFromLast, -maxAngVel)
        val maxVelForCurvature =
            if (useCurvatureControl && output.error < correctionDistance && lastVel > 5.0 && lastDesiredHeading != null) {
                val targetOmega = path.segment(gvf.lastProjectDisplacement).run {
                    first.curve.tangentAngleDeriv(second)
                }
                if (targetOmega epsilonEquals 0.rad) Double.POSITIVE_INFINITY
                else lastVel * (angVel / targetOmega).absoluteValue
            } else Double.POSITIVE_INFINITY
        val velocity = minOf(maxVelFromLast, maxVelToStop, maxVel, maxVelForCurvature).coerceAtLeast(0.0)

        lastUpdateTimestamp = timestamp
        lastVel = velocity
        val targetVel = Kinematics.fieldToRobotVelocity(
            currentPose, Pose2d(gvfResult.norm().let {
                if (it epsilonEquals 0.0) Vector2d() else gvfResult / it
            })
        ).vec() * velocity
        val currentVel = currentRobotVel?.vec() ?: lastPosition?.let {
            Kinematics.fieldToRobotVelocity(
                currentPose, Pose2d((currentPose.vec() - it) / dt)
            )
        }?.vec()
        val velFeedback = if (currentVel != null) {
            val velError = targetVel - currentVel
            Vector2d(
                velError.x * kX,
                velError.y * kY
            )
        } else Vector2d()

        lastPosition = currentPose.vec()
        lastError = Kinematics.calculateRobotPoseError(pathTarget, currentPose)

        // TODO: GVF acceleration FF?
        return DriveSignal(
            Pose2d(
                targetVel.rotated(omega * dt) + velFeedback,
                headingController.update(currentPose.heading.radians).rad
            )
        )
    }
}