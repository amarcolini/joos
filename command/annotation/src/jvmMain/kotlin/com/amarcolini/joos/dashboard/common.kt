package com.amarcolini.joos.dashboard

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

const val joosConfigName: String = "com.amarcolini.joos.dashboard.JoosConfig"
const val mutableConfigProviderName: String = "com.amarcolini.joos.dashboard.MutableConfigProvider"
const val immutableConfigProviderName: String = "com.amarcolini.joos.dashboard.ImmutableConfigProvider"
const val disabledName: String = "com.qualcomm.robotcore.eventloop.opmode.Disabled"
const val configVariableName: String = "com.acmerobotics.dashboard.config.variable.ConfigVariable"

const val className: String = "ConfigResults"
const val classPackage: String = "com.amarcolini.joos.dashboard"

inline fun <reified T> KSType.isAssignableFrom(resolver: Resolver): Boolean {
    val classDeclaration = requireNotNull(resolver.getClassDeclarationByName<T>()) {
        "Unable to resolve ${KSClassDeclaration::class.simpleName} for type ${T::class.simpleName}"
    }
    return isAssignableFrom(classDeclaration.asStarProjectedType())
}