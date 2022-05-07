package com.amarcolini.joos.dashboard

import com.acmerobotics.dashboard.config.ValueProvider
import kotlin.reflect.KMutableProperty0

/**
 * Value provider backed by kotlin property.
 */
class PropertyProvider<T : Any>(private val property: KMutableProperty0<T>) : ValueProvider<T> {
    override fun get(): T = property.get()

    override fun set(value: T): Unit = property.set(value)
}