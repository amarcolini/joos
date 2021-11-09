package com.amarcolini.joos

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.trajectory.config.GenericConstraints
import com.amarcolini.joos.trajectory.config.TrajectoryConfig
import com.amarcolini.joos.trajectory.config.TrajectoryConfigManager
import org.junit.jupiter.api.Test
import java.io.File

const val CONFIG_DIR = "./config/"

class ConfigTest {
    @Test
    fun saveConfig() {
        val waypoints = listOf(
            TrajectoryConfig.Spline(
                TrajectoryConfig.SplineData(
                    Pose2d(30.0, -30.0, 0.0),
                )
            ),
            TrajectoryConfig.Wait(5.0),
            TrajectoryConfig.Turn(Math.toRadians(90.0))
        )
        val constraints = GenericConstraints(30.0, 30.0, Math.toRadians(180.0), Math.toRadians(180.0))
        val config = TrajectoryConfig(Pose2d(), 0.0, waypoints, constraints)
        TrajectoryConfigManager.saveConfig(config, File("${CONFIG_DIR}/test.yaml"))
    }

    @Test
    fun loadConfig() {
        saveConfig()
        val loadedConfig = TrajectoryConfigManager.loadConfig(File("${CONFIG_DIR}/test.yaml"))
        assert(loadedConfig != null)
        println(loadedConfig)
    }
}