package com.amarcolini.joos.dashboard

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.reflect.KProperty0

class ConfigSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger: KSPLogger = environment.logger
    private val results = ArrayList<Pair<String, String>>()
    private val mutableConfigProviders = ArrayList<KSName>()
    private val immutableConfigProviders = ArrayList<KSName>()
    private val processedSymbols = ArrayList<KSNode>()

    private var joosConfig: KSClassDeclaration? = null
    private var configVariable: KSClassDeclaration? = null

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (joosConfig == null) joosConfig = resolver.getClassDeclarationByName(joosConfigName)
        if (configVariable == null) configVariable = resolver.getClassDeclarationByName(configVariableName)
        val symbols = resolver.getSymbolsWithAnnotation(joosConfigName) + resolver.getSymbolsWithAnnotation(
            mutableConfigProviderName
        ) + resolver.getSymbolsWithAnnotation(immutableConfigProviderName)
        val disabled = resolver.getSymbolsWithAnnotation(disabledName)
        val rejects = symbols.filter { !it.validate() || disabled.contains(it) }.toList()
        val visitor = ConfigVisitor()
        val goodSymbols = symbols - rejects.toSet()
        logger.info("goodSymbols: $goodSymbols")
        goodSymbols.forEach {
            logger.info("Visiting symbol $it.")
            it.accept(visitor, resolver)
        }
        return rejects
    }

    override fun finish() {
        val mutableFunctions = mutableConfigProviders.map {
            ClassName(it.getQualifier(), it.getShortName()).constructorReference()
        }
        val immutableFunctions = immutableConfigProviders.map {
            ClassName(it.getQualifier(), it.getShortName()).constructorReference()
        }
        val type = TypeSpec.objectBuilder(className).addProperty(
            PropertySpec.builder("isKotlin", Boolean::class).addAnnotation(JvmField::class).initializer("true").build()
        ).addProperty(PropertySpec.builder("results", typeNameOf<List<Pair<String, KProperty0<Any>>>>())
            .addAnnotation(JvmStatic::class)
            .initializer(CodeBlock.builder().beginControlFlow("try").addStatement("listOf(${
                results.joinToString {
                    "\"${it.first}\"" + " to " + it.second
                }
            })").nextControlFlow("catch(_: %T)", Exception::class).addStatement("emptyList()").endControlFlow().build())
            .build()).addProperty(
            PropertySpec.builder(
                "mutableProviders", List::class.asTypeName().parameterizedBy(
                    ClassName("kotlin.reflect", "KFunction1").parameterizedBy(
                        STAR, ClassName.bestGuess(configVariableName).parameterizedBy(STAR)
                    )
                )
            ).addAnnotation(JvmStatic::class).initializer(
                CodeBlock.builder().beginControlFlow("try").addStatement(
                    "listOf(${
                        List(mutableConfigProviders.size) { "%L" }.joinToString()
                    })", *mutableFunctions.toTypedArray()
                ).nextControlFlow("catch(_: %T)", Exception::class).addStatement("emptyList()").endControlFlow().build()
            ).build()
        ).addProperty(
            PropertySpec.builder(
                "immutableProviders", List::class.asTypeName().parameterizedBy(
                    ClassName("kotlin.reflect", "KFunction2").parameterizedBy(
                        ClassName("kotlin.reflect", "KFunction0").parameterizedBy(STAR), ClassName("kotlin.reflect", "KFunction1").parameterizedBy(STAR, UNIT), ClassName.bestGuess(
                            configVariableName
                        ).parameterizedBy(STAR)
                    )
                )
            ).addAnnotation(JvmStatic::class).initializer(
                CodeBlock.builder().beginControlFlow("try").addStatement(
                    "listOf(${
                        List(immutableConfigProviders.size) { "%L" }.joinToString()
                    })", *immutableFunctions.toTypedArray()
                ).nextControlFlow("catch(_: %T)", Exception::class).addStatement("emptyList()").endControlFlow().build()
            ).build()
        ).build()
        val dependencies = Dependencies(true, *processedSymbols.map { it.containingFile!! }.toTypedArray())
        val file = environment.codeGenerator.createNewFile(dependencies, classPackage, className)
        val writer = file.bufferedWriter()
        FileSpec.builder(classPackage, className).addType(type).build().writeTo(writer)
        writer.close()
        file.close()
//        environment.codeGenerator.associateWithClasses(processedClasses, classPackage, className)
    }

    inner class ConfigVisitor : KSEmptyVisitor<Resolver, Unit>() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Resolver) {
            processedSymbols += classDeclaration
            logger.info("Visiting class ${classDeclaration.simpleName.asString()}.")
            if (!classDeclaration.isPublic()) return
            logger.info("Class is public.")
            when (classDeclaration.classKind) {
                ClassKind.INTERFACE, ClassKind.ENUM_CLASS, ClassKind.ENUM_ENTRY, ClassKind.ANNOTATION_CLASS -> return
                ClassKind.OBJECT -> {
                    logger.info("Class is an object + usable.")
                    classDeclaration.declarations.filterIsInstance<KSPropertyDeclaration>().forEach {
                        it.accept(this, data)
                    }
                }
                ClassKind.CLASS -> {
                    logger.info("Class is normal + usable.")
                    classDeclaration.declarations.forEach {
                        when (it) {
                            is KSClassDeclaration -> it.accept(this, data)
                            is KSPropertyDeclaration -> if (it.modifiers.contains(Modifier.JAVA_STATIC)) it.accept(
                                this, data
                            )
                        }
                    }
                }
            }
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Resolver) {
            if (!property.isPublic() || property.isAbstract() || property.modifiers.contains(Modifier.LATEINIT)) return
            logger.info("Property ${property.simpleName.asString()} passed first round.")
            val enum = data.getClassDeclarationByName("kotlin.Enum")?.asStarProjectedType()
                ?.let { data.createKSTypeReferenceFromKSType(it) }
            when (property.type.resolve()) {
                data.builtIns.doubleType, data.builtIns.booleanType, data.builtIns.intType, data.builtIns.stringType, enum -> {
                    if (!property.isMutable || property.modifiers.contains(Modifier.FINAL)) return
                }
            }
            logger.info("Property isn't an immutable primitive.")
            if (property.type.annotations.any {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == immutableName
                } && (!property.isMutable || property.modifiers.contains(Modifier.FINAL))) {
                logger.info("Property type has Immutable annotation but isn't mutable (fail).")
                return
            }
            val closestClass = property.closestClassDeclaration()
            if (closestClass?.classKind != ClassKind.OBJECT && !property.modifiers.contains(Modifier.JAVA_STATIC)) return
            logger.info("Property is static.")
            val parentClass =
                if (closestClass?.isCompanionObject == true) closestClass.parentDeclaration as KSClassDeclaration else closestClass
            val name = property.qualifiedName ?: return
            logger.info("Property passed.")
            val parentClassName = parentClass?.simpleName?.asString()?.ifEmpty { null }
            val parentClassAltName = parentClass?.let { getAltName(it)?.ifEmpty { null } }
            val propertyAltName = getAltName(property)?.ifEmpty { null }
            results += (propertyAltName ?: parentClassAltName ?: parentClassName
            ?: return) to (name.getQualifier() + "::" + name.getShortName())
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Resolver) {
            processedSymbols += function
            logger.info("Visiting potential config provider ${function.simpleName.getShortName()}.")
            if (!(function.functionKind == FunctionKind.STATIC || function.functionKind == FunctionKind.TOP_LEVEL)) {
                logger.info("Function is not top-level or static.")
                return
            }
            if (function.isAbstract || !function.isPublic()) {
                logger.info("Function is abstract or not public.")
                return
            }
            val isMutableProvider = function.annotations.any {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == mutableConfigProviderName
            }
            if (isMutableProvider) {
                logger.info("Function is for a mutable provider.")
                if (function.parameters.size != 1) {
                    logger.info("Function does not accept 1 parameter.")
                    return
                }
            } else {
                logger.info("Function is for an immutable provider.")
                if (function.parameters.size != 2) {
                    logger.info("Function does not accept 2 parameters.")
                    return
                }
                val param1 = function.parameters[0].type.resolve() as? KSFunction
                val param2 = function.parameters[1].type.resolve() as? KSFunction
                if (param1 == null || param2 == null) {
                    logger.info("Function parameters are not lambdas.")
                    return
                }
                if (param1.parameterTypes.isNotEmpty()) {
                    logger.info("First parameter has a parameter.")
                    return
                }
                if (param2.parameterTypes.size != 1) {
                    logger.info("Second parameter does not have only one parameter.")
                    return
                }
                if (param2.returnType?.isAssignableFrom(data.builtIns.unitType) != true) {
                    logger.info("Second parameter returns a value.")
                    return
                }
                if (param1.returnType != param2.parameterTypes[0] && param1.returnType != null) {
                    logger.info("Parameter types don't match.")
                    return
                }
            }
            if (configVariable?.asStarProjectedType()
                    ?.isAssignableFrom(function.returnType?.resolve() ?: return) == false
            ) {
                logger.info("Function does not return a ConfigVariable")
                return
            }
            val name = function.qualifiedName ?: return
            if (isMutableProvider) mutableConfigProviders += name
            else immutableConfigProviders += name
        }

        private fun getAltName(node: KSAnnotated): String? {
            val joosConfig = joosConfig
            return if (joosConfig != null) {
                node.annotations.find { it.shortName == joosConfig.simpleName && it.annotationType.resolve().declaration.qualifiedName == joosConfig.qualifiedName }?.arguments?.get(
                    0
                )?.value?.toString()
            } else null
        }

        override fun defaultHandler(node: KSNode, data: Resolver) = Unit
    }
}

class ConfigSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ConfigSymbolProcessor(environment)
    }
}