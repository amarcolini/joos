package com.amarcolini.joos.dashboard

import com.acmerobotics.dashboard.config.ValueProvider
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

/**
 * A custom value provider using [getter] and [setter] for [get] and [set].
 */
class CustomProvider<T>(private val getter: () -> T, private val setter: (T) -> Unit) : ValueProvider<T> {
    companion object {
        @JvmStatic
        fun <T> of(property: KMutableProperty0<T>): CustomProvider<T> = CustomProvider(property.getter, property.setter)

        @JvmStatic
        fun <T, V> of(property: KMutableProperty1<T, V>, parent: T): CustomProvider<V> = CustomProvider(
            { property.get(parent) },
            { value -> property.set(parent, value) }
        )
    }

    override fun get(): T = getter()

    override fun set(value: T) = setter(value)
}