package com.amarcolini.joos.dashboard

import com.acmerobotics.dashboard.config.variable.ConfigVariable

/**
 * Specifies a custom config provider to be used by [ConfigHandler]. Functions with this annotation should take in one
 * argument (the value to generate a config for) and return a [ConfigVariable]. They should also be static (In Kotlin,
 * this means using [JvmStatic], a companion object, or a top-level function).
 *
 * @param priority The priority of this provider. This helps with multiple providers for one class,
 * where the provider with the highest priority will be selected.
 * @see ConfigHandler
 * @see ImmutableConfigProvider
 * @see JoosConfig
 */
@Target(AnnotationTarget.FUNCTION)
annotation class MutableConfigProvider(val priority: Int = 0)

/**
 * Specifies a custom config provider to be used by [ConfigHandler]. Functions with this annotation should take in two
 * arguments (the property getter and setter, in that order) and return a [ConfigVariable]. They should also be static (In Kotlin,
 * this means using [JvmStatic], a companion object, or a top-level function).
 *
 * @param priority The priority of this provider. This helps with multiple providers for one class,
 * where the provider with the highest priority will be selected.
 * @see ConfigHandler
 * @see MutableConfigProvider
 * @see JoosConfig
 */
@Target(AnnotationTarget.FUNCTION)
annotation class ImmutableConfigProvider(val priority: Int = 0)