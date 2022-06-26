package com.amarcolini.joos.dashboard

import android.content.Context
import android.util.Log
import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.reflection.FieldProvider
import com.acmerobotics.dashboard.config.variable.BasicVariable
import com.acmerobotics.dashboard.config.variable.ConfigVariable
import com.acmerobotics.dashboard.config.variable.CustomVariable
import com.acmerobotics.dashboard.config.variable.VariableType
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.qualcomm.ftccommon.FtcEventLoop
import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure


/**
 * Singleton class that handles the [JoosConfig] annotation as well as FTC Dashboard configs
 * for [Pose2d], [Vector2d], [Angle], and other classes as specified by [WithConfig].
 */
@Suppress("UNCHECKED_CAST")
object ConfigHandler {
    private val initTasks = ArrayList<() -> Unit>()

    private val javaResults: List<Pair<String, Field>>?
    private val kotlinResults: List<Pair<String, KProperty0<Any>>>?

    init {
        val (java, kotlin) = try {
            val resultClass = Class.forName("com.amarcolini.joos.dashboard.ConfigResults")
            val isKotlin = resultClass.getDeclaredField("isKotlin").get(null) as Boolean
            val resultList = resultClass.getDeclaredMethod("getResults").invoke(null)
            if (isKotlin) {
                null to resultList as List<Pair<String, KProperty0<Any>>>
            } else {
                resultList as List<Pair<String, Field>> to null
            }
        } catch (_: Exception) {
            null to null
        }
        javaResults = java
        kotlinResults = kotlin
    }

    @OnCreateEventLoop
    @JvmStatic
    fun init(context: Context, eventLoop: FtcEventLoop) {
        javaResults?.forEach { (group, field) ->
            parseField(field, group)
        }

        kotlinResults?.forEach { (group, property) ->
            parseProperty(property, group)
        }

        initTasks.forEach { it() }
        initTasks.clear()

        FtcDashboard.getInstance().updateConfig()
    }

    private fun parse(value: Any?): ConfigVariable<*>? {
        fun internalParse(property: KProperty1<Any, Any>, parent: Any): ConfigVariable<*>? {
            val propertyClass = property.returnType.jvmErasure
            return when (VariableType.fromClass(propertyClass.java)) {
                VariableType.BOOLEAN, VariableType.INT, VariableType.DOUBLE, VariableType.STRING, VariableType.ENUM -> {
                    if (property !is KMutableProperty<*> || property.setter.visibility != KVisibility.PUBLIC) null
                    else ConfigUtils.createVariable(property as KMutableProperty1<Any, *>, parent)
                }
                VariableType.CUSTOM -> {
                    val providedConfig: ConfigVariable<*>? = try {
                        val provider = propertyClass.java.getAnnotation(WithConfig::class.java)?.provider
                        (provider?.createInstance() as? ConfigProvider<Any>)?.parse(property.get(parent))
                    } catch (_: Exception) {
                        null
                    }
                    if (providedConfig != null) return providedConfig
                    val customVariable = CustomVariable()
                    for (memberProperty in propertyClass.declaredMemberProperties) {
                        if (property.visibility != KVisibility.PUBLIC || (property is KMutableProperty<*> && property.setter.visibility != KVisibility.PUBLIC)) continue
                        val name = memberProperty.name
                        try {
                            val nestedVariable =
                                internalParse(memberProperty as KProperty1<Any, Any>, property.get(parent))
                            if (nestedVariable != null) {
                                customVariable.putVariable(name, nestedVariable)
                            }
                        } catch (e: Exception) {
                            Log.w("ConfigHandler", e)
                        }
                    }
                    customVariable
                }
                else -> throw RuntimeException(
                    "Unsupported field type: ${propertyClass.qualifiedName}"
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
                for (memberProperty in value::class.declaredMemberProperties) {
                    if (memberProperty.visibility != KVisibility.PUBLIC || (memberProperty is KMutableProperty<*> && memberProperty.setter.visibility != KVisibility.PUBLIC)) continue
                    val name = memberProperty.name
                    try {
                        val nestedVariable = internalParse(memberProperty as KProperty1<Any, Any>, value)
                        if (nestedVariable != null) {
                            customVariable.putVariable(name, nestedVariable)
                        }
                    } catch (e: Exception) {
                        Log.w("ConfigHandler", e)
                    }
                }
                customVariable
            }
        }
    }

    private fun parseField(field: Field, group: String) {
        if (!Modifier.isPublic(field.modifiers)) return
        FtcDashboard.getInstance().withConfigRoot { configRoot ->
            val rootVariable = configRoot.getVariable(group) as? CustomVariable ?: CustomVariable().also {
                configRoot.putVariable(group, it)
            }
            val type = VariableType.fromClass(field.type)
            val variable = if (!Modifier.isFinal(field.modifiers) && type != VariableType.CUSTOM)
                BasicVariable(type, FieldProvider<Boolean>(field, null))
            else parse(field.get(null))
            if (variable != null) {
                rootVariable.putVariable(field.name, variable)
            }
        }
    }

    private fun parseProperty(property: KProperty0<Any>, group: String) {
        if (property.visibility != KVisibility.PUBLIC) return
        FtcDashboard.getInstance().withConfigRoot { configRoot ->
            val rootVariable = configRoot.getVariable(group) as? CustomVariable ?: CustomVariable().also {
                configRoot.putVariable(group, it)
            }
            val type = VariableType.fromClass(property.returnType.jvmErasure.java)
            val variable = if (property is KMutableProperty0<Any> && type != VariableType.CUSTOM)
                ConfigUtils.createVariable(property)
            else parse(property.get())
            if (variable != null) {
                rootVariable.putVariable(property.name, variable)
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
        putVariable("x", ConfigUtils.createVariable(pose::x))
        putVariable("y", ConfigUtils.createVariable(pose::y))
    }

    private fun parse(pos: Vector2d): ConfigVariable<*> = CustomVariable().apply {
        putVariable("x", ConfigUtils.createVariable(pos::x))
        putVariable("y", ConfigUtils.createVariable(pos::y))
    }

    private fun parse(angle: Angle): ConfigVariable<Double> = ConfigUtils.createVariable(angle::defaultValue) {
        angle.value = it
        angle.units = Angle.defaultUnits
    }
}