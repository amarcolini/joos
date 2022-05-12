package com.amarcolini.joos.dashboard

import com.google.auto.service.AutoService
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import com.squareup.kotlinpoet.*
import kotlin.reflect.KProperty0
import kotlin.io.*
import kotlin.reflect.KClass

class ConfigSymbolProcessor(private val environment: SymbolProcessorEnvironment, private val logger: KSPLogger = environment.logger) : SymbolProcessor {
    private val results = ArrayList<Pair<String, String>>()
    private val processedClasses = ArrayList<KSClassDeclaration>()

    private var joosConfig: KSClassDeclaration? = null

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (joosConfig == null) {
            joosConfig = resolver.getClassDeclarationByName(joosConfigName)
        }
        val symbols = resolver.getSymbolsWithAnnotation(joosConfigName)
        val disabled = resolver.getSymbolsWithAnnotation(disabledName)
        val rejects = symbols.filter { !it.validate() || disabled.contains(it) }.toList()
        val visitor = ConfigVisitor()
        val goodSymbols = symbols - rejects
        if (goodSymbols.iterator().hasNext()) {
            goodSymbols.forEach {
                it.accept(visitor, resolver)
            }
        }
        return rejects
    }

    override fun finish() {
        val type = TypeSpec.objectBuilder(className)
            .addProperty(
                PropertySpec.builder("isKotlin", Boolean::class)
                    .addAnnotation(JvmField::class)
                    .initializer("true")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("results", typeNameOf<List<Pair<String, KProperty0<Any>>>>())
                    .addAnnotation(JvmStatic::class)
                    .initializer(
                        CodeBlock.builder()
                            .beginControlFlow("try")
                            .addStatement("listOf(${results.joinToString {
                                "\"${it.first}\"" + " to " + it.second
                            }})")
                            .nextControlFlow("catch(_: %T)", Exception::class)
                            .addStatement("emptyList()")
                            .endControlFlow()
                            .build()
                    )
                    .build()
            )
            .build()
        val file = environment.codeGenerator.createNewFile(Dependencies(false), classPackage, className)
        val writer = file.bufferedWriter()
        FileSpec.builder(classPackage, className)
            .addType(type)
            .build()
            .writeTo(writer)
        writer.close()
        file.close()
        environment.codeGenerator.associateWithClasses(processedClasses, classPackage, className)
    }

    inner class ConfigVisitor : KSEmptyVisitor<Resolver, Unit>() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Resolver) {
            processedClasses += classDeclaration
            when (classDeclaration.classKind) {
                ClassKind.INTERFACE, ClassKind.ENUM_CLASS, ClassKind.ENUM_ENTRY, ClassKind.ANNOTATION_CLASS -> return
                ClassKind.OBJECT -> {
                    classDeclaration.declarations.filterIsInstance<KSPropertyDeclaration>().forEach {
                        it.accept(this, data)
                    }
                }
                ClassKind.CLASS -> {
                    classDeclaration.declarations.forEach {
                        when (it) {
                            is KSClassDeclaration -> it.accept(this, data)
                            is KSPropertyDeclaration -> if (it.modifiers.contains(Modifier.JAVA_STATIC))
                                it.accept(this, data)
                        }
                    }
                }
            }
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Resolver) {
            if (!property.isPublic() || property.isAbstract() || property.modifiers.contains(Modifier.LATEINIT))
                return
            val enum = data.getClassDeclarationByName("kotlin.Enum")?.asStarProjectedType()
                ?.let { data.createKSTypeReferenceFromKSType(it) }
            when (property.type.resolve()) {
                data.builtIns.doubleType, data.builtIns.booleanType, data.builtIns.intType, data.builtIns.stringType, enum -> {
                    if (!property.isMutable || property.modifiers.contains(Modifier.FINAL)) return
                }
            }
            val closestClass = property.closestClassDeclaration()
            if (closestClass?.classKind != ClassKind.OBJECT && !property.modifiers.contains(Modifier.JAVA_STATIC)) return
            val parentClass = if (closestClass?.isCompanionObject == true) closestClass.parentDeclaration as KSClassDeclaration else closestClass
            val name = property.qualifiedName ?: return
            val parentClassName = parentClass?.simpleName?.asString()?.ifEmpty { null }
            val parentClassAltName = parentClass?.let { getAltName(it)?.ifEmpty { null } }
            val propertyAltName = getAltName(property)?.ifEmpty { null }
            logger.info(property.type.toString())
            results += (propertyAltName ?: parentClassAltName ?: parentClassName ?: return) to (name.getQualifier() + "::" + name.getShortName())
        }

        private fun getAltName(node: KSAnnotated): String? {
            val joosConfig = joosConfig
            return if (joosConfig != null) {
                node.annotations.find { it.shortName == joosConfig.simpleName && it.annotationType.resolve().declaration.qualifiedName == joosConfig.qualifiedName  }?.arguments?.get(0)?.value?.toString()
            } else null
        }

        override fun defaultHandler(node: KSNode, data: Resolver) = Unit
    }
}

@AutoService(SymbolProcessorProvider::class)
class ConfigSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ConfigSymbolProcessor(environment)
    }
}