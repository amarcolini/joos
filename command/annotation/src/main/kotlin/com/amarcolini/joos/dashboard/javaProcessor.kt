package com.amarcolini.joos.dashboard

import com.squareup.javapoet.*
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import kotlin.collections.ArrayList

@SupportedSourceVersion(SourceVersion.RELEASE_8)
class ConfigProcessor : AbstractProcessor() {
    private val results = ArrayList<Pair<String, Pair<String, String>>>()

    override fun getSupportedAnnotationTypes() = setOf(joosConfigName, withConfigName)

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val utils = processingEnv.elementUtils
        val joosConfig: TypeElement? = utils.getTypeElement(joosConfigName)
        val disabled: TypeElement? = utils.getTypeElement(disabledName)
        if (joosConfig != null) {
            val disabledElements = roundEnv.getElementsAnnotatedWith(disabled)
            (roundEnv.getElementsAnnotatedWith(joosConfig) - disabledElements).forEach { element ->
                results += element.accept(ConfigElementVisitor(), joosConfig)
            }
        }
        if (roundEnv.processingOver()) {
            val resultObject = TypeSpec.classBuilder(className)
                .addField(
                    FieldSpec.builder(TypeName.BOOLEAN, "isKotlin")
                        .addModifiers(Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL)
                        .initializer("false")
                        .build()
                )
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
            JavaFile.builder(classPackage, resultObject)
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
        if (p0?.modifiers?.contains(Modifier.PUBLIC) != true) return emptyList()
        val configMembers = ArrayList<Pair<String, Pair<String, String>>>()
        val name = getAltName(p0, p1) ?: p0.simpleName.toString()
        p0.enclosedElements?.forEach { member ->
            if (member.kind == ElementKind.FIELD && member.modifiers.containsAll(
                    listOf(
                        Modifier.PUBLIC, Modifier.STATIC
                    )
                )
            ) {
                configMembers += (getAltName(member, p1)
                    ?: name) to (p0.qualifiedName.toString() to member.simpleName.toString())
            }
        }
        return configMembers
    }

    override fun visitVariable(p0: VariableElement, p1: TypeElement?): List<Pair<String, Pair<String, String>>> {
        if (p0.kind == ElementKind.FIELD && p0.modifiers.containsAll(
                listOf(
                    Modifier.PUBLIC, Modifier.STATIC
                )
            )
        ) {
            var traverser: Element? = p0.enclosingElement
            var possibleParent: TypeElement? = traverser as? TypeElement
            if (traverser != null && possibleParent == null) {
                while (traverser !is TypeElement) {
                    traverser = traverser?.enclosingElement ?: break
                    if (traverser is TypeElement) {
                        possibleParent = traverser
                    }
                }
            }
            val parent = possibleParent ?: return emptyList()
            if (!parent.modifiers.contains(Modifier.PUBLIC)) return emptyList()
            return listOf(
                (getAltName(p0, p1) ?: getAltName(parent, p1)
                ?: parent.simpleName.toString()) to (parent.qualifiedName.toString() to p0.simpleName.toString())
            )
        }
        return emptyList()
    }

    private fun getAltName(element: Element?, joosConfig: TypeElement?): String? {
        if (element == null || joosConfig == null) return null
        val annotation =
            element.annotationMirrors?.find { it.annotationType.toString() == joosConfig.qualifiedName?.toString() }
                ?: return null
        return annotation.elementValues?.values?.firstOrNull()?.value?.toString()?.ifEmpty { null }
    }

    override fun visitExecutable(p0: ExecutableElement?, p1: TypeElement?): List<Pair<String, Pair<String, String>>> =
        emptyList()

    override fun visitTypeParameter(
        p0: TypeParameterElement?,
        p1: TypeElement?
    ): List<Pair<String, Pair<String, String>>> = emptyList()

    override fun visitUnknown(p0: Element?, p1: TypeElement?): List<Pair<String, Pair<String, String>>> = emptyList()
}