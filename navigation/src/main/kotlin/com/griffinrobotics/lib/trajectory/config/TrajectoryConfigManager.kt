package com.griffinrobotics.lib.trajectory.config

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.dataformat.yaml.util.StringQuotingChecker
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.griffinrobotics.lib.trajectory.Trajectory
import com.griffinrobotics.lib.trajectory.TrajectoryBuilder
import java.io.File
import java.io.InputStream

/**
 * Class containing methods for saving (loading) trajectory configurations to (from) YAML files.
 */
object TrajectoryConfigManager {
    @JvmField
    @Suppress("MayBeConst")
    val GROUP_FILENAME = "_group.yaml"

    private val MAPPER: YAMLMapper

    init {
        val factory = YAMLFactory.builder()
            .stringQuotingChecker(object : StringQuotingChecker() {
                override fun needToQuoteName(name: String?) =
                    if (name.equals("y")) false
                    else isReservedKeyword(name) || looksLikeYAMLNumber(name)

                override fun needToQuoteValue(value: String?) =
                    isReservedKeyword(value) || looksLikeYAMLNumber(value)
            })
            .build()
        MAPPER = YAMLMapper(factory)
        MAPPER.registerKotlinModule()
        MAPPER.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
    }

    /**
     * Saves a [TrajectoryConfig] to [file].
     */
    @JvmStatic
    fun saveConfig(trajectoryConfig: TrajectoryConfig, file: File) {
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, trajectoryConfig)
    }

    /**
     * Saves a [TrajectoryConstraints] to [dir].
     */
    @JvmStatic
    fun saveConstraints(trajectoryConfig: TrajectoryConstraints, dir: File) {
        MAPPER.writerWithDefaultPrettyPrinter()
            .writeValue(File(dir, GROUP_FILENAME), trajectoryConfig)
    }

    /**
     * Loads a [TrajectoryConfig] from [file].
     */
    @JvmStatic
    fun loadConfig(file: File): TrajectoryConfig? =
        MAPPER.readValue(file, TrajectoryConfig::class.java)

    /**
     * Loads a [TrajectoryConfig] from [inputStream].
     */
    @JvmStatic
    fun loadConfig(inputStream: InputStream): TrajectoryConfig? =
        MAPPER.readValue(inputStream, TrajectoryConfig::class.java)

    /**
     * Loads the [TrajectoryConstraints] inside [dir].
     */
    @JvmStatic
    fun loadConstraints(dir: File): TrajectoryConstraints? {
        val groupFile = File(dir, GROUP_FILENAME)
        if (!groupFile.exists()) {
            return null
        }
        return MAPPER.readValue(groupFile, TrajectoryConstraints::class.java)
    }

    /**
     * Loads the [TrajectoryConstraints] from [inputStream].
     */
    @JvmStatic
    fun loadConstraints(inputStream: InputStream) =
        MAPPER.readValue(inputStream, TrajectoryConstraints::class.java)

    /**
     * Loads a [TrajectoryBuilder] from [file].
     */
    @JvmStatic
    fun loadBuilder(file: File): TrajectoryBuilder? {
        val config = loadConfig(file) ?: return null
        return config.toTrajectoryBuilder()
    }

    /**
     * Loads a [Trajectory] from [file].
     */
    @JvmStatic
    fun load(file: File) = loadBuilder(file)?.build()
}
