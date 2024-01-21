package com.amarcolini.joos.dashboard

import com.acmerobotics.dashboard.config.variable.BasicVariable
import com.acmerobotics.dashboard.config.variable.ConfigVariable
import com.acmerobotics.dashboard.config.variable.CustomVariable
import org.junit.Test
import java.util.*
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KProperty0


class DashboardTest {
    @Test
    fun testDashboard() {
        try {
            ConfigHandler.init(null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val initialBoolean = ConfigOpMode.myDataClass.isHappy
        ConfigHandler.internalModel.get<CustomVariable>("ConfigOpMode")
            ?.get<CustomVariable>("myDataClass")
            ?.get<BasicVariable<Boolean>>("isHappy")
            ?.set(!initialBoolean)
        assert(ConfigOpMode.myDataClass.isHappy == !initialBoolean)

        ConfigHandler.internalModel.get<CustomVariable>("ConfigOpMode")
            ?.get<CustomVariable>("myArray")
            ?.get<BasicVariable<Double>>("0")
            ?.set(6.9)
        assert(ConfigOpMode.myArray[0] == 6.9)

        ConfigHandler.internalModel.get<CustomVariable>("ConfigOpMode")
            ?.get<BasicVariable<Double>>("myFloat")
            ?.set(6.9)
        assert(ConfigOpMode.myFloat == 6.9f)

        printConfigVariable(ConfigHandler.internalModel)
        ConfigHandler.getLogs().forEach(::println)
    }
}

operator fun <T: ConfigVariable<*>> CustomVariable.get(name: String, consumer: T.() -> Unit = {}): T? {
    val value = try {
        this.getVariable(name) as? T
    } catch (_: Exception) { null }
    if (value != null) consumer(value)
    return value
}

fun <T> BasicVariable<T>.set(value: T) {
    this.update(ConfigUtils.createVariable({ value }) {})
}

fun printConfigVariable(variable: ConfigVariable<*>, prefix: String = "") {
    when (variable) {
        is BasicVariable -> println(prefix + variable.type.name.lowercase(Locale.getDefault()))
        is CustomVariable -> {
            for (entry in variable.entrySet().sortedBy { it.key }) {
                println(prefix + entry.key + ": ")
                printConfigVariable(entry.value, "$prefix   ")
            }
        }
    }
}

object ConfigResults {
    @JvmField
    val isKotlin: Boolean = true

    @JvmStatic
    val results: List<Pair<String, KProperty0<Any>>> = try {
        listOf(
            "ConfigOpMode" to ConfigOpMode.Companion::myArray,
            "ConfigOpMode" to ConfigOpMode.Companion::myFloat,
            "ConfigOpMode" to ConfigOpMode.Companion::myDataClass,
        )
    } catch (_: Exception) {
        emptyList()
    }


    @JvmStatic
    val mutableProviders: List<KFunction1<*, ConfigVariable<*>>> = try {
        listOf()
    } catch (_: Exception) {
        emptyList()
    }


    @JvmStatic
    val immutableProviders:
            List<KFunction2<KFunction0<*>, KFunction1<*, Unit>, ConfigVariable<*>>> = try {
        listOf()
    } catch (_: Exception) {
        emptyList()
    }
}

class ConfigOpMode {
    companion object {
        var myArray = arrayOf(0.0, 1.0, 1.5)

        var myFloat = 8f

        var myDataClass = Billy(false)
    }

    data class Billy(
        val isHappy: Boolean
    )
}