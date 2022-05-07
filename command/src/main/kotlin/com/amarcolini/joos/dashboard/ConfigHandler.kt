package com.amarcolini.joos.dashboard

import android.util.Log
import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.ValueProvider
import com.acmerobotics.dashboard.config.reflection.FieldProvider
import com.acmerobotics.dashboard.config.variable.BasicVariable
import com.acmerobotics.dashboard.config.variable.ConfigVariable
import com.acmerobotics.dashboard.config.variable.CustomVariable
import com.acmerobotics.dashboard.config.variable.VariableType
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.full.createInstance


/**
 * Singleton class that handles the [JoosConfig] annotation as well as FTC Dashboard configs
 * for [Pose2d], [Vector2d], [Angle], and other classes as specified by [WithConfig].
 */
@Suppress("UNCHECKED_CAST")
object ConfigHandler {
    @JvmStatic
    private val initTasks = ArrayList<() -> Unit>()

    private val results = try {
        Class.forName("com.amarcolini.joos.dashboard.ConfigResults").getDeclaredMethod("getResults")
            .invoke(null) as List<Pair<String, Field>>
    } catch (_: Exception) {
        null
    }

    @JvmStatic
    @OnCreateEventLoop
    fun init() {
        results?.forEach { (group, field) ->
            parseField(field, group)
        }

        initTasks.forEach { it() }
        initTasks.clear()
    }

    private fun parse(value: Any?): ConfigVariable<*>? {
        fun internalParse(field: Field, parent: Any?): ConfigVariable<*>? {
            val fieldClass = field.type
            //TODO: kotlin type reflection?
            return when (val type = VariableType.fromClass(fieldClass)) {
                VariableType.BOOLEAN, VariableType.INT, VariableType.DOUBLE, VariableType.STRING, VariableType.ENUM -> {
                    if (Modifier.isFinal(field.modifiers)) null
                    else BasicVariable(type, FieldProvider<Boolean>(field, parent))
                }
                VariableType.CUSTOM -> {
                    val providedConfig: ConfigVariable<*>? = try {
                        val provider = field.type.getAnnotation(WithConfig::class.java)?.provider
                        (provider?.createInstance() as? ConfigProvider<Any>)?.parse(field.get(null)!!)
                    } catch (_: Exception) {
                        null
                    }
                    if (providedConfig != null) return providedConfig
                    val customVariable = CustomVariable()
                    for (nestedField in fieldClass.fields) {
                        val name = nestedField.name
                        try {
                            val nestedVariable = internalParse(nestedField, field[parent])
                            if (nestedVariable != null) {
                                customVariable.putVariable(name, nestedVariable)
                            }
                        } catch (e: IllegalAccessException) {
                            Log.w("ConfigHandler", e)
                        }
                    }
                    customVariable
                }
                else -> throw RuntimeException(
                    "Unsupported field type: ${fieldClass.name}"
                )
            }
        }

        return when (value) {
            null -> null
            is Pose2d -> parse(value)
            is Vector2d -> parse(value)
            is Angle -> parse(value)
            is Double, is Int, is Boolean, is String, is Enum<*> -> null
            else -> {
                val providedConfig: ConfigVariable<*>? = try {
                    val provider = value::class.java.getAnnotation(WithConfig::class.java)?.provider
                    (provider?.createInstance() as? ConfigProvider<Any>)?.parse(value)
                } catch (_: Exception) {
                    null
                }
                if (providedConfig != null) return providedConfig
                val customVariable = CustomVariable()
                for (nestedField in value::class.java.fields) {
                    val name = nestedField.name
                    try {
                        val nestedVariable = internalParse(nestedField, value)
                        if (nestedVariable != null) {
                            customVariable.putVariable(name, nestedVariable)
                        }
                    } catch (e: IllegalAccessException) {
                        Log.w("ConfigHandler", e)
                    }
                }
                customVariable
            }
        }
    }

    private fun parseField(field: Field, group: String) {
        FtcDashboard.getInstance().withConfigRoot { configRoot ->
            val rootVariable = configRoot.getVariable(group) as? CustomVariable ?: CustomVariable().also {
                configRoot.putVariable(group, it)
            }
            val variable = parse(field.get(null))
            if (variable != null) {
                rootVariable.putVariable(field.name, variable)
            }
        }
    }

    /**
     * Adds the provided [value] to the FTC Dashboard config under the group 'Runtime'.
     */
    @JvmStatic
    fun <T : Any> createConfig(name: String, value: T): T {
        val dashboard = FtcDashboard.getInstance() ?: run {
            initTasks += { createConfig(name, value) }
            return value
        }
        val variable = parse(value) ?: return value
        dashboard.withConfigRoot { configRoot ->
            val group = configRoot.getVariable("Runtime") as? CustomVariable ?: CustomVariable().apply {
                configRoot.putVariable("Runtime", this)
            }
            group.putVariable(name, variable)
        }
        dashboard.updateConfig()
        return value
    }

    private fun parse(pose: Pose2d): ConfigVariable<*> = CustomVariable().apply {
        putVariable("heading", parse(pose.heading))
        putVariable("x", BasicVariable(PropertyProvider(pose::x)))
        putVariable("y", BasicVariable(PropertyProvider(pose::y)))
    }

    private fun parse(pos: Vector2d): ConfigVariable<*> = CustomVariable().apply {
        putVariable("x", BasicVariable(PropertyProvider(pos::x)))
        putVariable("y", BasicVariable(PropertyProvider(pos::y)))
    }

    private fun parse(angle: Angle): ConfigVariable<Double> =
        BasicVariable(VariableType.DOUBLE, object : ValueProvider<Double> {
            override fun get(): Double = angle.defaultValue

            override fun set(value: Double?) {
                if (value != null) {
                    angle.value = value
                    angle.units = Angle.defaultUnits
                }
            }
        })
}