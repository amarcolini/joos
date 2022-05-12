package com.amarcolini.joos.dashboard

import com.acmerobotics.dashboard.config.reflection.FieldProvider
import com.acmerobotics.dashboard.config.variable.BasicVariable
import java.lang.reflect.Field
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

object ConfigUtils {
    /**
     * Creates a config variable from the provided kotlin [property].
     */
    @JvmStatic
    fun <T> createVariable(property: KMutableProperty0<T>): BasicVariable<T> =
        BasicVariable(CustomProvider.of(property))

    /**
     * Creates a config variable from the provided kotlin [property] and [parent].
     */
    @JvmStatic
    fun <T, V> createVariable(property: KMutableProperty1<T, V>, parent: T): BasicVariable<V> = BasicVariable(
        CustomProvider.of(property, parent)
    )

    /**
     * Convenience method for creating custom config variables.
     */
    @JvmStatic
    fun <T> createVariable(getter: () -> T, setter: (T) -> Unit): BasicVariable<T> =
        BasicVariable(CustomProvider(getter, setter))

    /**
     * Creates a config variable from the provided java [field] and [parent].
     */
    @JvmStatic
    fun <T> createVariable(field: Field, parent: Any?): BasicVariable<T> =
        BasicVariable(FieldProvider(field, parent))
}