package com.amarcolini.joos

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.trajectory.constraints.GenericConstraints
import com.amarcolini.joos.trajectory.config.TrajectoryConfig
import com.amarcolini.joos.trajectory.config.TrajectoryConfigManager
import com.amarcolini.joos.util.deg
import org.junit.jupiter.api.Test
import java.io.File

const val CONFIG_DIR: String = "./config/"

class ConfigTest {
    @Test
    fun saveConfig(): TrajectoryConfig {
        val waypoints = listOf(
            TrajectoryConfig.Spline(
                TrajectoryConfig.SplineData(
                    Pose2d(30.0, -30.0, 0.deg),
                )
            ),
            TrajectoryConfig.Wait(5.0),
            TrajectoryConfig.Turn(90.deg)
        )
        val constraints =
            GenericConstraints(40.0, 30.0, 180.0.deg, 180.deg)
        val config = TrajectoryConfig(Pose2d(), 0.deg, waypoints, constraints)
        val file = File("${CONFIG_DIR}/test.yaml")
        file.parentFile.mkdirs()
        TrajectoryConfigManager.saveConfig(config, file)
        return config
    }

    @Test
    fun loadConfig() {
        val savedConfig = saveConfig()
        val loadedConfig = TrajectoryConfigManager.loadConfig(File("${CONFIG_DIR}/test.yaml"))!!
        assert(loadedConfig == savedConfig)
    }
}