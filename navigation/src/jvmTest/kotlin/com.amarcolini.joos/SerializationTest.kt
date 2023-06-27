package com.amarcolini.joos

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.heading.SplineHeading
import com.amarcolini.joos.path.heading.TangentHeading
import com.amarcolini.joos.serialization.*
import com.amarcolini.joos.util.deg
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

val format = Json { prettyPrint = false }

class SerializationTest {
    @Test
    fun testPose2d() {
        val data = Pose2d(10.0, 23.0, 45.deg)
        val string = format.encodeToString(data)
        println(string)
        val decoded = format.decodeFromString<Pose2d>(string)
        println(decoded)
        assert(data == decoded)
    }

    @Test
    fun testTrajectory() {
        val data = SerializableTrajectory(
            TrajectoryStart(Pose2d(10.0)),
            mutableListOf(
                LinePiece(
                    Vector2d(5.0, 3.0),
                    TangentHeading
                ),
                SplinePiece(
                    Vector2d(10.0, 10.0),
                    13.deg, -1.0, -1.0,
                    SplineHeading(45.deg)
                ),
                TurnPiece(10.deg),
                WaitPiece(3.0)
            )
        )
        val string = format.encodeToString(data)
        println(string)
        val decoded = format.decodeFromString<SerializableTrajectory>(string)
        println(decoded)
        assert(data == decoded)
    }
}