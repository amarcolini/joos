package com.amarcolini.joos.dashboard

import android.content.Context
import android.util.Log
import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.variable.BasicVariable
import com.acmerobotics.dashboard.config.variable.ConfigVariable
import com.acmerobotics.dashboard.config.variable.CustomVariable
import com.acmerobotics.dashboard.config.variable.VariableType
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.deg
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
 * for [Pose2d], [Vector2d], [Angle], and other classes as specified by [ImmutableConfigProvider] and [MutableConfigProvider].
 */
@Suppress("UNCHECKED_CAST")
object ConfigHandler {
    private val initTasks = ArrayList<() -> Unit>()

    private val javaResults: List<Pair<String, Field>>?
    private val kotlinResults: List<Pair<String, KProperty0<Any?>>>?

    private data class MutableProviderData<T>(
        val function: (T) -> ConfigVariable<*>, val priority: Int, val isType: (KType) -> Boolean
    )

    private data class ImmutableProviderData<T>(
        val function: (() -> T, (T) -> Unit) -> ConfigVariable<*>, val priority: Int, val isType: (KType) -> Boolean
    )

    private val mutableConfigProviders: List<MutableProviderData<Any?>>
    private val immutableConfigProviders: List<ImmutableProviderData<Any?>>

    private var logs = listOf<String>()
    fun getLogs(): List<String> = logs
    internal val internalModel = CustomVariable()

    init {
        var kotlin: List<Pair<String, KProperty0<Any?>>>? = null
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
            logs += "successfully retrieved class data!"
            if (isKotlin) {
                logs += "Using KSP"
                kotlin = resultList as List<Pair<String, KProperty0<Any?>>>
                mutableProviders += (resultClass.getDeclaredMethod("getMutableProviders")
                    .invoke(null) as List<KFunction1<*, ConfigVariable<Any>>>).map {
                    MutableProviderData(
                        it as KFunction1<Any?, ConfigVariable<*>>,
                        it.findAnnotation<MutableConfigProvider>()?.priority ?: throw IllegalStateException(
                            "Config providers should have the ConfigProvider annotation."
                        ),
                        it.returnType::isSupertypeOf
                    )
                }
                logs += "successfully retrieved mutable config providers!"
                immutableProviders += (resultClass.getDeclaredMethod("getImmutableProviders")
                    .invoke(null) as List<KFunction1<*, ConfigVariable<Any>>>).map {
                    ImmutableProviderData(
                        it as (() -> Any?, (Any?) -> Unit) -> ConfigVariable<*>,
                        it.findAnnotation<ImmutableConfigProvider>()?.priority ?: throw IllegalStateException(
                            "Config providers should have the ConfigProvider annotation."
                        ),
                        it.returnType::isSupertypeOf
                    )
                }
                logs += "successfully retrieved immutable config providers!"
            } else {
                logs += "Using Java"
                java = resultList as List<Pair<String, Field>>
                mutableProviders += (resultClass.getDeclaredMethod("getMutableProviders")
                    .invoke(null) as List<Method>).map { method ->
                    MutableProviderData(
                        method::invoke as KFunction1<Any?, ConfigVariable<*>>,
                        method.annotations.filterIsInstance<MutableConfigProvider>().getOrNull(0)?.priority
                            ?: throw IllegalStateException(
                                "Config providers should have the ConfigProvider annotation."
                            )
                    ) { method.returnType.isAssignableFrom(it.jvmErasure.java) }
                }
                logs += "successfully retrieved mutable config providers!"
                immutableProviders += (resultClass.getDeclaredMethod("getImmutableProviders")
                    .invoke(null) as List<Method>).map { method ->
                    ImmutableProviderData(
                        method::invoke as (() -> Any?, (Any?) -> Unit) -> ConfigVariable<*>,
                        method.annotations.filterIsInstance<ImmutableConfigProvider>().getOrNull(0)?.priority
                            ?: throw IllegalStateException(
                                "Config providers should have the ConfigProvider annotation."
                            ),
                    ) { method.returnType.isAssignableFrom(it.jvmErasure.java) }
                }
                logs += "successfully retrieved immutable config providers!"
            }
        } catch (_: Exception) {
            logs += "Failed to initialize."
        }
        javaResults = java
        kotlinResults = kotlin
        mutableConfigProviders = mutableProviders
        immutableConfigProviders = immutableProviders
    }

    @OnCreateEventLoop
    @JvmStatic
    fun init(context: Context?, eventLoop: FtcEventLoop?) {
        try {
            javaResults?.forEach { (group, field) ->
                logs += "parsing field ${field.name}"
                try {
                    addVariableToDashboard(parseField(field), group, field.name)
                } catch (_: Exception) {
                    logs += "Failed to parse field ${field.name}."
                }
            }

            kotlinResults?.forEach { (group, property) ->
                logs += "parsing property ${property.name}"
                try {
                    addVariableToDashboard(parseProperty(property), group, property.name)
                } catch (_: Exception) {
                    logs += "Failed to parse property ${property.name}."
                }
            }

            initTasks.forEach { it() }
            initTasks.clear()

            FtcDashboard.getInstance()?.updateConfig()
        } catch (_: Exception) {
        }
    }

    fun createVariableFromArray(
        array: () -> Array<*>,
        arrayType: KClass<*>,
        parent: Any,
        indices: IntArray
    ): ConfigVariable<*>? {
        return when (val type = VariableType.fromClass(arrayType.java)) {
            VariableType.BOOLEAN, VariableType.INT, VariableType.DOUBLE, VariableType.STRING, VariableType.ENUM -> BasicVariable<Boolean>(
                type,
                BetterArrayProvider(array, *indices.copyOf())
            )

            VariableType.CUSTOM -> {
                return try {
                    var value: Any? = null
                    try {
                        value = BetterArrayProvider.getArrayRecursive(array(), indices)
                    } catch (ignored: ArrayIndexOutOfBoundsException) {
                    }
                    if (value == null) {
                        return CustomVariable(null)
                    }
                    val customVariable = CustomVariable()
                    if (arrayType.isSubclassOf(Array::class)) {
                        val newIndices: IntArray = indices.copyOf(indices.size + 1)
                        var i = 0
                        while (i < java.lang.reflect.Array.getLength(value)) {
                            newIndices[newIndices.size - 1] = i
                            customVariable.putVariable(
                                i.toString(),
                                createVariableFromArray(
                                    array,
                                    arrayType.java.componentType.kotlin,
                                    parent,
                                    newIndices
                                )
                            )
                            i++
                        }
                    } else {
                        for (nestedProperty in arrayType.declaredMemberProperties) {
                            customVariable.putVariable(
                                nestedProperty.name, parse(
                                    nestedProperty, value
                                )
                            )
                        }
                    }
                    customVariable
                } catch (e: IllegalAccessException) {
                    throw RuntimeException(e)
                }
            }

            else -> throw RuntimeException(
                "Unsupported field type: " + arrayType.simpleName
            )
        }
    }

    fun parse(property: KProperty1<*, *>, parent: Any): ConfigVariable<*>? {
        val castProperty = property as KProperty1<Any, Any>
        return parse(
            castProperty.returnType,
            { castProperty.get(parent) },
            if (castProperty is KMutableProperty1<*, *>) { it ->
                (castProperty.setter as KMutableProperty1.Setter<Any, Any?>).call(parent, it)
            } else null
        )
    }

    fun parse(type: KType, getter: () -> Any?, setter: ((Any?) -> Unit)? = null): ConfigVariable<*>? {
        val variableType = VariableType.fromClass(type.jvmErasure.java)
        //If the property can be modified, it could be a primitive, an immutable or regular old mutable object
        if (setter != null) {
            if (variableType != VariableType.CUSTOM) {
                return ConfigUtils.createVariable(getter, setter)
            } else when (type) {
                Long::class.createType() -> return ConfigUtils.createVariable(
                    { (getter() as Long).toDouble() },
                    { setter(it.toLong()) })

                Float::class.createType() -> return ConfigUtils.createVariable(
                    { (getter() as Float).toDouble() },
                    { setter(it.toFloat()) })

                Short::class.createType() -> return ConfigUtils.createVariable(
                    { (getter() as Short).toInt() },
                    { setter(it.toShort()) })

                Byte::class.createType() -> return ConfigUtils.createVariable(
                    { (getter() as Byte).toInt() },
                    { setter(it.toByte()) })
            }

            //Deal with both kinds of config providers
            val providedConfig = immutableConfigProviders.filter {
                it.isType(type)
            }.maxByOrNull { it.priority }?.function?.invoke(getter, setter)
            if (providedConfig != null) return providedConfig

            //Deal with immutable types without providers (just data classes rn)
            if (type.jvmErasure.isData) {
                val variable = CustomVariable()
                val params = type.jvmErasure.primaryConstructor?.parameters ?: return null
                val dataClass = (type.classifier as KClass<Any>)
                params.forEach { param ->
                    val paramProperty =
                        dataClass.declaredMemberProperties.find { it.name == param.name } ?: return null
                    if (paramProperty.visibility != KVisibility.PUBLIC) return@forEach

                    fun <T> addVariable(
                        preprocess: (Any?) -> T = { it as T },
                        postprocess: (T) -> Any? = { it }
                    ) {
                        variable.putVariable(param.name, ConfigUtils.createVariable(getter = {
                            preprocess(getter()?.let { paramProperty.get(it) })
                        }, setter = { value ->
                            val function = dataClass.declaredFunctions.find { it.name == "copy" }
                            val parameter = function?.parameters?.find { it.name == param.name }
                            val instanceParameter = function?.instanceParameter
                            val parameterMap = mutableMapOf<KParameter, Any?>()
                            if (parameter != null) {
                                parameterMap[parameter] = postprocess(value)
                                instanceParameter?.let { parameterMap[instanceParameter] = getter() }
                                setter(function.callBy(parameterMap))
                            }
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
                                    if (function?.visibility == KVisibility.PUBLIC) {
                                        val parameter = function.parameters.find { it.name == param.name }
                                        if (parameter != null) setter(
                                            function.callBy(
                                                mapOf(
                                                    parameter to value
                                                )
                                            )
                                        )
                                    }
                                }) ?: return null
                            )
                        }
                    }
                }
                return variable
            }
        }

        //Now dealing with regular mutable object
        return when (val value = getter()) {
            null -> null
            is Double, is Int, is Boolean, is String, is Enum<*> -> null
            else -> {
                val providedConfig = mutableConfigProviders.filter {
                    it.isType(value::class.starProjectedType)
                }.maxByOrNull { it.priority }?.function?.invoke(value)
                if (providedConfig != null) return providedConfig
                val customVariable = CustomVariable()
                if (value is Array<*>) {
                    logs += "yo we got an array"
                    for (i in 0 until value.size) {
                        customVariable.putVariable(
                            i.toString(),
                            createVariableFromArray(
                                { value },
                                type.jvmErasure.java.componentType.kotlin,
                                value,
                                intArrayOf(i)
                            )
                        )
                    }
                } else for (memberProperty in value::class.declaredMemberProperties) {
                    if (memberProperty.visibility != KVisibility.PUBLIC) continue
                    val name = memberProperty.name
                    try {
                        val nestedVariable = parse(memberProperty, value)
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

    fun parseField(field: Field): ConfigVariable<out Any>? {
        (field.kotlinProperty as? KProperty0<Any?>)?.let { return parseProperty(it) }
        if (!Modifier.isPublic(field.modifiers)) return null
        return parse(
            field.type.kotlin.starProjectedType,
            { field.get(null) },
            if (!Modifier.isFinal(field.modifiers)
//                && (field.type.kotlin.hasAnnotation<Immutable>() || field.annotations.any { it.annotationClass.hasAnnotation<Immutable>() })
            )
                { value ->
                    field.set(null, value)
                } else null
        )
    }

    fun parseProperty(property: KProperty0<Any?>): ConfigVariable<out Any>? {
        if (property.visibility != KVisibility.PUBLIC) return null
        return parse(
            property.returnType,
            property.getter,
            if (property is KMutableProperty0<Any?>
//                && (property.returnType.jvmErasure.hasAnnotation<Immutable>() || property.hasAnnotation<Immutable>())
            ) property.setter else null
        )
    }

    fun addVariableToDashboard(variable: ConfigVariable<out Any>?, group: String, name: String) {
        val func = { configRoot: CustomVariable ->
            val rootVariable = configRoot.getVariable(group) as? CustomVariable ?: CustomVariable().also {
                configRoot.putVariable(group, it)
            }
            if (variable != null) {
                rootVariable.putVariable(name, variable)
            }
        }
        FtcDashboard.getInstance()?.withConfigRoot(func)
        internalModel.apply(func)
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
        val variable = parse(value::class.starProjectedType, { value }) ?: return value
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
        ConfigUtils.createVariable({ getter().degrees }, { setter(it.deg) })
}