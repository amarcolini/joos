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
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinProperty


/**
 * Singleton class that handles the [JoosConfig] annotation as well as FTC Dashboard configs
 * for [Pose2d], [Vector2d], [Angle], and other classes as specified by [ConfigProvider].
 */
@Suppress("UNCHECKED_CAST")
object ConfigHandler {
    private val initTasks = ArrayList<() -> Unit>()

    private val javaResults: List<Pair<String, Field>>?
    private val kotlinResults: List<Pair<String, KProperty0<Any>>>?

    private data class MutableProviderData<T>(
        val function: (T) -> ConfigVariable<*>, val priority: Int, val isType: (KType) -> Boolean
    )

    private data class ImmutableProviderData<T>(
        val function: (() -> T, (T) -> Unit) -> ConfigVariable<*>, val priority: Int, val isType: (KType) -> Boolean
    )

    private val mutableConfigProviders: List<MutableProviderData<Any?>>
    private val immutableConfigProviders: List<ImmutableProviderData<Any?>>

    init {
        var kotlin: List<Pair<String, KProperty0<Any>>>? = null
        var java: List<Pair<String, Field>>? = null

        val mutableProviders: MutableList<MutableProviderData<Any?>> = mutableListOf()
        val immutableProviders: MutableList<ImmutableProviderData<Any?>> = mutableListOf(
            ImmutableProviderData(
                ::parsePose, 0
            ) { it.isSubtypeOf(Pose2d::class.starProjectedType) } as ImmutableProviderData<Any?>,
            ImmutableProviderData(
                ::parseVector, 0
            ) { it.isSubtypeOf(Vector2d::class.starProjectedType) } as ImmutableProviderData<Any?>,
            ImmutableProviderData(
                ::parseAngle, 0
            ) { it.isSubtypeOf(Angle::class.starProjectedType) } as ImmutableProviderData<Any?>,
        )

        try {
            val resultClass = Class.forName("com.amarcolini.joos.dashboard.ConfigResults")
            val isKotlin = resultClass.getDeclaredField("isKotlin").get(null) as Boolean
            val resultList = resultClass.getDeclaredMethod("getResults").invoke(null)
            if (isKotlin) {
                kotlin = resultList as List<Pair<String, KProperty0<Any>>>
                mutableProviders += (resultClass.getDeclaredMethod("getMutableConfigProviders")
                    .invoke(null) as List<KFunction1<*, ConfigVariable<Any>>>).map {
                    MutableProviderData(
                        it as KFunction1<Any?, ConfigVariable<*>>,
                        it.findAnnotation<MutableConfigProvider>()?.priority ?: throw IllegalStateException(
                            "Config providers should have the ConfigProvider annotation."
                        ),
                        it.returnType::isSupertypeOf
                    )
                }
                immutableProviders += (resultClass.getDeclaredMethod("getImmutableConfigProviders")
                    .invoke(null) as List<KFunction1<*, ConfigVariable<Any>>>).map {
                    ImmutableProviderData(
                        it as (() -> Any?, (Any?) -> Unit) -> ConfigVariable<*>,
                        it.findAnnotation<ImmutableConfigProvider>()?.priority ?: throw IllegalStateException(
                            "Config providers should have the ConfigProvider annotation."
                        ),
                        it.returnType::isSupertypeOf
                    )
                }
            } else {
                java = resultList as List<Pair<String, Field>>
                mutableProviders += (resultClass.getDeclaredMethod("getConfigProviders")
                    .invoke(null) as List<Method>).map { method ->
                    MutableProviderData(
                        method::invoke as KFunction1<Any?, ConfigVariable<*>>,
                        method.annotations.filterIsInstance<MutableConfigProvider>().getOrNull(0)?.priority
                            ?: throw IllegalStateException(
                                "Config providers should have the ConfigProvider annotation."
                            )
                    ) { method.returnType.isAssignableFrom(it.jvmErasure.java) }
                }
                immutableProviders += (resultClass.getDeclaredMethod("getImmutableConfigProviders")
                    .invoke(null) as List<Method>).map { method ->
                    ImmutableProviderData(
                        method::invoke as (() -> Any?, (Any?) -> Unit) -> ConfigVariable<*>,
                        method.annotations.filterIsInstance<ImmutableConfigProvider>().getOrNull(0)?.priority ?: throw IllegalStateException(
                                "Config providers should have the ConfigProvider annotation."
                            ),
                    ) { method.returnType.isAssignableFrom(it.jvmErasure.java) }
                }
            }
        } catch (_: Exception) {
        }
        javaResults = java
        kotlinResults = kotlin
        mutableConfigProviders = mutableProviders
        immutableConfigProviders = immutableProviders
    }

    @OnCreateEventLoop
    @JvmStatic
    fun init(context: Context, eventLoop: FtcEventLoop) {
        javaResults?.forEach { (group, field) ->
            try {
                parseField(field, group)
            } catch (_: Exception) {
            }
        }

        kotlinResults?.forEach { (group, property) ->
            try {
                parseProperty(property, group)
            } catch (_: Exception) {
            }
        }

        initTasks.forEach { it() }
        initTasks.clear()

        FtcDashboard.getInstance()?.updateConfig()
    }

    private fun parse(value: Any?): ConfigVariable<*>? {
        fun internalParse(property: KProperty1<Any, Any?>, parent: Any): ConfigVariable<*>? {
            val propertyClass = property.returnType.jvmErasure
            return when (VariableType.fromClass(propertyClass.java)) {
                VariableType.BOOLEAN, VariableType.INT, VariableType.DOUBLE, VariableType.STRING, VariableType.ENUM -> {
                    if (property !is KMutableProperty<*> || property.setter.visibility != KVisibility.PUBLIC) null
                    else ConfigUtils.createVariable(property as KMutableProperty1<Any, *>, parent)
                }
                VariableType.CUSTOM -> {
                    val providedConfig = mutableConfigProviders.filter {
                        it.isType(property.returnType)
                    }.maxByOrNull { it.priority }?.function?.invoke(property.get(parent))
                    if (providedConfig != null) return providedConfig
                    val customVariable = CustomVariable()
                    for (memberProperty in propertyClass.declaredMemberProperties) {
                        if (property.visibility != KVisibility.PUBLIC || (property is KMutableProperty<*> && property.setter.visibility != KVisibility.PUBLIC)) continue
                        val name = memberProperty.name
                        try {
                            val nestedVariable =
                                internalParse(
                                    memberProperty as KProperty1<Any, Any>,
                                    property.get(parent) ?: return null
                                )
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
            is Double, is Int, is Boolean, is String, is Enum<*> -> null
            else -> {
                val providedConfig = mutableConfigProviders.filter {
                    it.isType(value::class.starProjectedType)
                }.maxByOrNull { it.priority }?.function?.invoke(value)
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
        parseProperty(field.kotlinProperty as KProperty0<Any?>, group)
    }

    private fun parseProperty(property: KProperty0<Any?>, group: String) {
        if (property.visibility != KVisibility.PUBLIC) return
        FtcDashboard.getInstance()?.withConfigRoot { configRoot ->
            val rootVariable = configRoot.getVariable(group) as? CustomVariable ?: CustomVariable().also {
                configRoot.putVariable(group, it)
            }
            val type = VariableType.fromClass(property.returnType.jvmErasure.java)
            val variable =
                if (property is KMutableProperty0<*> && type != VariableType.CUSTOM) ConfigUtils.createVariable(
                    property
                )
                else if (property.returnType.hasAnnotation<Immutable>() && property is KMutableProperty0<*>) {
                    fun parse(type: KType, getter: () -> Any?, setter: (Any?) -> Unit): ConfigVariable<*>? {
                        val providedConfig = immutableConfigProviders.filter {
                            it.isType(type)
                        }.maxByOrNull { it.priority }?.function?.invoke(getter, setter)
                        if (providedConfig != null) return providedConfig
                        if (type.jvmErasure.isData) {
                            val variable = CustomVariable()
                            val params = type.jvmErasure.primaryConstructor?.parameters ?: return null
                            val dataClass = (type.classifier as KClass<Any>)
                            params.forEach { param ->
                                val paramProperty =
                                    dataClass.declaredMemberProperties.find { it.name == param.name } ?: return null

                                fun <T> addVariable(
                                    preprocess: (Any?) -> T = { it as T },
                                    postprocess: (T) -> Any? = { it }
                                ) {
                                    variable.putVariable(param.name, ConfigUtils.createVariable(getter = {
                                        preprocess(getter()?.let { paramProperty.get(it) })
                                    }, setter = { value ->
                                        val function = dataClass.declaredFunctions.find { it.name == "copy" }
                                        val parameter = function?.parameters?.find { it.name == param.name }
                                        if (parameter != null) function.callBy(
                                            mapOf(
                                                parameter to postprocess(
                                                    value
                                                )
                                            )
                                        )
                                    }))
                                }
                                when (param.type) {
                                    Long::class.createType() -> addVariable(
                                        { (it as Long).toDouble() },
                                        { it.toLong() })
                                    Float::class.createType() -> addVariable(
                                        { (it as Float).toDouble() },
                                        { it.toFloat() })
                                    Short::class.createType() -> addVariable(
                                        { (it as Short).toInt() },
                                        { it.toShort() })
                                    Byte::class.createType() -> addVariable(
                                        { (it as Byte).toInt() },
                                        { it.toByte() })
                                    Int::class.createType() -> addVariable<Int>()
                                    Double::class.createType() -> addVariable<Double>()
                                    String::class.createType() -> addVariable<String>()
                                    Boolean::class.createType() -> addVariable<Boolean>()
                                    else -> {
                                        if (param.type.isSubtypeOf(Enum::class.starProjectedType)) addVariable<Enum<*>>()
                                        else variable.putVariable(
                                            param.name,
                                            parse(param.type, { getter()?.let { paramProperty.get(it) } }, { value ->
                                                val function = dataClass.declaredFunctions.find { it.name == "copy" }
                                                val parameter = function?.parameters?.find { it.name == param.name }
                                                if (parameter != null) function.callBy(
                                                    mapOf(
                                                        parameter to value
                                                    )
                                                )
                                            }) ?: return null
                                        )
                                    }
                                }
                            }
                            return variable
                        }
                        return null
                    }
                    parse(property.returnType, property.getter, property.setter as (Any?) -> Unit)
                } else parse(property.get())
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

    private fun parsePose(getter: () -> Pose2d, setter: (Pose2d) -> Unit): ConfigVariable<*> = CustomVariable().apply {
        putVariable("heading", parseAngle({
            getter().heading
        }, { setter(getter().copy(heading = it)) }))
        putVariable("x", ConfigUtils.createVariable({
            getter().x
        }, { setter(getter().copy(x = it)) }))
        putVariable("y", ConfigUtils.createVariable({
            getter().y
        }, { setter(getter().copy(y = it)) }))
    }

    private fun parseVector(getter: () -> Vector2d, setter: (Vector2d) -> Unit): ConfigVariable<*> =
        CustomVariable().apply {
            putVariable("x", ConfigUtils.createVariable({
                getter().x
            }, { setter(getter().copy(x = it)) }))
            putVariable("y", ConfigUtils.createVariable({
                getter().y
            }, { setter(getter().copy(y = it)) }))
        }

    private fun parseAngle(getter: () -> Angle, setter: (Angle) -> Unit): ConfigVariable<Double> =
        ConfigUtils.createVariable({ getter().defaultValue }, { setter(Angle(it)) })
}