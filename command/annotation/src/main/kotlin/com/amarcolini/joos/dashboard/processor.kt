package com.amarcolini.joos.dashboard

import com.google.auto.service.AutoService
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.io.File
import java.lang.reflect.TypeVariable
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.tools.Diagnostic
import kotlin.collections.ArrayList

//TODO: kotlin-specific type scanning?
@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class Processor : AbstractProcessor() {
    private val joosConfigName = "com.amarcolini.joos.dashboard.JoosConfig"
    private val results = ArrayList<Pair<String, Pair<String, String>>>()

    override fun getSupportedAnnotationTypes() = setOf(joosConfigName)

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val utils = processingEnv.elementUtils
        val joosConfig: TypeElement? = utils.getTypeElement(joosConfigName)
        val disabled: TypeElement? = utils.getTypeElement("com.qualcomm.robotcore.eventloop.opmode.Disabled")
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "o: " + processingEnv.options)
        if (joosConfig != null) {
            val disabledElements = roundEnv.getElementsAnnotatedWith(disabled)
            roundEnv.getElementsAnnotatedWith(joosConfig).forEach { element ->
                if (element in disabledElements) return@forEach
                results += element.accept(ConfigElementVisitor(), joosConfig)
            }
        }
        if (roundEnv.processingOver()) {
            val resultObject = TypeSpec.classBuilder("ConfigResults")
                .addMethod(
                    MethodSpec.methodBuilder("getResults")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(List::class.java)
                        .beginControlFlow("try")
                        .addStatement("return \$T.asList(${
                            results.joinToString { (group, field) -> "new kotlin.Pair<>(\"$group\", ${field.first}.class.getField(\"${field.second}\"))" }
                        })", Arrays::class.java)
                        .nextControlFlow("catch (\$T e)", Exception::class.java)
                        .addStatement("return \$T.emptyList()", Collections::class.java)
                        .endControlFlow()
                        .build()
                )
                .build()
            JavaFile.builder("com.amarcolini.joos.dashboard", resultObject)
                .build()
                .writeTo(processingEnv.filer)
        }
        return true
    }
}

class ConfigElementVisitor : ElementVisitor<List<Pair<String, Pair<String, String>>>, TypeElement?> {
    override fun visit(p0: Element?, p1: TypeElement?): List<Pair<String, Pair<String, String>>> = emptyList()

    override fun visitPackage(p0: PackageElement?, p1: TypeElement?): List<Pair<String, Pair<String, String>>> =
        emptyList()

    override fun visitType(p0: TypeElement?, p1: TypeElement?): List<Pair<String, Pair<String, String>>> {
        val configMembers = ArrayList<Pair<String, Pair<String, String>>>()
        val annotation = p0?.annotationMirrors?.find { it.annotationType.enclosingType == p1?.asType() }
        val name = annotation?.elementValues?.values?.first()?.value?.toString() ?: p0?.simpleName.toString()
        p0?.enclosedElements?.forEach { member ->
            if (member.kind == ElementKind.FIELD && member.modifiers.containsAll(
                    listOf(Modifier.PUBLIC, Modifier.STATIC)
                )
            ) {
                configMembers += name to (p0.qualifiedName.toString() to member.simpleName.toString())
            }
        }
        return configMembers
    }

    override fun visitVariable(p0: VariableElement, p1: TypeElement?): List<Pair<String, Pair<String, String>>> =
        emptyList()

    override fun visitExecutable(p0: ExecutableElement?, p1: TypeElement?): List<Pair<String, Pair<String, String>>> =
        emptyList()

    override fun visitTypeParameter(
        p0: TypeParameterElement?,
        p1: TypeElement?
    ): List<Pair<String, Pair<String, String>>>? = emptyList()

    override fun visitUnknown(p0: Element?, p1: TypeElement?): List<Pair<String, Pair<String, String>>> = emptyList()
}