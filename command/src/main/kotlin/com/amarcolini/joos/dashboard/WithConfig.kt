package com.amarcolini.joos.dashboard

import kotlin.reflect.KClass


/**
 * Specifies a custom [ConfigProvider] for any classes with this annotation.
 *
 * @param provider the [ConfigProvider] to use for this class
 * @see ConfigHandler
 * @see ConfigProvider
 * @see JoosConfig
 */
@Target(AnnotationTarget.CLASS)
annotation class WithConfig(val provider: KClass<out ConfigProvider<*>>)