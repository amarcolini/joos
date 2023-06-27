package com.amarcolini.joos.followers

import com.amarcolini.joos.drive.DriveSignal
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.AngleUnit
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.util.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * State-of-the-art path follower based on the [GuidingVectorField].
 *
 * @param maxVel maximum velocity
 * @param maxAccel maximum acceleration
 * @param admissibleError admissible/satisfactory pose error at the end of each move
 * @param kN normal vector weight (see [GuidingVectorField])
 * @param kOmega proportional heading gain
 * @param errorMapFunc error map function (see [GuidingVectorField])
 * @param clock clock
 */
@JsExport
class GVFFollower @JvmOverloads constructor(
    @JvmField var maxVel: Double,
    @JvmField var maxAccel: Double,
    admissibleError: Pose2d,
    @JvmField var kN: Double,
    @JvmField var kOmega: Double,
    private val errorMapFunc: (Double) -> Double = { it },
    clock: NanoClock = NanoClock.system()
) : PathFollower(admissibleError, clock) {
    private lateinit var gvf: GuidingVectorField
    private var lastUpdateTimestamp: Double = 0.0
    private var lastVel: Double = 0.0
    private var lastProjDisplacement: Double = 0.0

    override var lastError: Pose2d = Pose2d()

    override fun followPath(path: Path) {
        gvf = GuidingVectorField(path, kN, errorMapFunc)
        lastUpdateTimestamp = clock.seconds()
        lastVel = 0.0
        lastProjDisplacement = 0.0
        super.followPath(path)
    }

    override fun internalUpdate(currentPose: Pose2d): DriveSignal {
        val gvfResult = gvf.getExtended(currentPose.x, currentPose.y, lastProjDisplacement)

        val desiredHeading =
            Angle(
                atan2(gvfResult.vector.y, gvfResult.vector.x),
                AngleUnit.Radians
            )
        val headingError = desiredHeading - currentPose.heading

        // TODO: implement this or nah? ref eqs. (18), (23), and (24)
        val desiredOmega = Angle()
        val omega = desiredOmega + kOmega * headingError

        // basic online motion profiling
        val timestamp = clock.seconds()
        val dt = timestamp - lastUpdateTimestamp
        val remainingDistance = currentPose.vec() distTo path.end().vec()
        val maxVelToStop = sqrt(2 * maxAccel * remainingDistance)
        val maxVelFromLast = lastVel + maxAccel * dt
        val velocity = minOf(maxVelFromLast, maxVelToStop, maxVel)

        lastUpdateTimestamp = timestamp
        lastVel = velocity
        lastProjDisplacement = gvfResult.displacement

        val targetPose = path[gvfResult.displacement]

        lastError = Kinematics.calculateRobotPoseError(targetPose, currentPose)

        // TODO: GVF acceleration FF?
        return DriveSignal(Pose2d(velocity, 0.0, omega))
    }
}