package com.amarcolini.joos

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.util.deg
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

val format = Json { prettyPrint = true }

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
}