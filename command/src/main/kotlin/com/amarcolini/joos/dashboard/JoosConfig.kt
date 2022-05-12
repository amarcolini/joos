package com.amarcolini.joos.dashboard

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.Disabled

/**
 * An alternative to [Config] that automatically handles more complex types like trajectories, poses, and angles, or
 * even custom types using [WithConfig]. Its functionality is the same such that if a class has this annotation,
 * all of its public, static, non-final fields will be added to FTC Dashboard (unless the class also has the [Disabled] annotation).
 *
 * @param name the name that all the class's config variables will appear under. Defaults to the class's simple name.
 * @see ConfigHandler
 * @see ConfigProvider
 * @see WithConfig
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class JoosConfig(val name: String = "")