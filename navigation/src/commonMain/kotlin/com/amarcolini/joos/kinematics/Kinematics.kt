package com.amarcolini.joos.kinematics

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmStatic
import kotlin.math.cos
import kotlin.math.sin

/**
 * A collection of methods for various kinematics-related tasks.
 */
@JsExport
object Kinematics {

    /**
     * Returns the robot pose velocity corresponding to [fieldPose] and [fieldVel].
     */
    @JvmStatic
    fun fieldToRobotVelocity(fieldPose: Pose2d, fieldVel: Pose2d): Pose2d =
        Pose2d(fieldVel.vec().rotated(-fieldPose.heading), fieldVel.heading)

    /**
     * Returns the robot pose acceleration corresponding to [fieldPose], [fieldVel], and [fieldAccel].
     */
    @JvmStatic
    fun fieldToRobotAcceleration(fieldPose: Pose2d, fieldVel: Pose2d, fieldAccel: Pose2d): Pose2d =
        Pose2d(
            fieldAccel.vec().rotated(-fieldPose.heading),
            fieldAccel.heading
        ) + Pose2d(
            -fieldVel.x * sin(fieldPose.heading) + fieldVel.y * cos(fieldPose.heading),
            -fieldVel.x * cos(fieldPose.heading) - fieldVel.y * sin(fieldPose.heading),
            0.0
        ) * fieldVel.heading.radians

    /**
     * Returns the error between [targetFieldPose] and [currentFieldPose] in the field frame.
     */
    @JvmStatic
    fun calculateFieldPoseError(targetFieldPose: Pose2d, currentFieldPose: Pose2d): Pose2d =
        Pose2d(
            (targetFieldPose - currentFieldPose).vec(),
            (targetFieldPose.heading - currentFieldPose.heading).normDelta()
        )

    /**
     * Returns the error between [targetFieldPose] and [currentFieldPose] in the robot frame.
     */
    @JvmStatic
    fun calculateRobotPoseError(targetFieldPose: Pose2d, currentFieldPose: Pose2d): Pose2d {
        val errorInFieldFrame = calculateFieldPoseError(targetFieldPose, currentFieldPose)
        return Pose2d(
            errorInFieldFrame.vec().rotated(-currentFieldPose.heading),
            errorInFieldFrame.heading
        )
    }

    /**
     * Performs a relative odometry update. Note: this assumes that the robot moves with constant velocity over the
     * measurement interval.
     */
    @JvmStatic
    fun relativeOdometryUpdate(fieldPose: Pose2d, robotPoseDelta: Pose2d): Pose2d {
        val dtheta = robotPoseDelta.heading.radians
        val (sineTerm, cosTerm) = if (dtheta epsilonEquals 0.0)
            1.0 - dtheta * dtheta / 6.0 to dtheta / 2.0
        else
            sin(dtheta) / dtheta to (1 - cos(dtheta)) / dtheta

        val fieldPositionDelta = Vector2d(
            sineTerm * robotPoseDelta.x - cosTerm * robotPoseDelta.y,
            cosTerm * robotPoseDelta.x + sineTerm * robotPoseDelta.y
        )

        val fieldPoseDelta =
            Pose2d(fieldPositionDelta.rotated(fieldPose.heading), robotPoseDelta.heading)

        return Pose2d(
            fieldPose.x + fieldPoseDelta.x,
            fieldPose.y + fieldPoseDelta.y,
            (fieldPose.heading + fieldPoseDelta.heading).norm()
        )
    }
}