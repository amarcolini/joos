package com.amarcolini.joos.dashboard

import com.acmerobotics.dashboard.config.variable.ConfigVariable

/**
 * Class used by [ConfigHandler] to provide custom config variables for classes.
 *
 * @see WithConfig
 */
interface ConfigProvider<T> {
    /**
     * Creates a [ConfigVariable] for [value].
     */
    fun parse(value: T): ConfigVariable<Any>
}