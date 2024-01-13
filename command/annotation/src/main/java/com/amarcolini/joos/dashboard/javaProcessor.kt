package com.amarcolini.joos.dashboard

import com.amarcolini.joos.dashboard.*
import com.squareup.javapoet.*
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(joosConfigName, immutableConfigProviderName, mutableConfigProviderName)
class ConfigProcessor : AbstractProcessor() {
    private val results = ArrayList<Pair<String, Pair<String, String>>>()
    private val mutableConfigProviders = ArrayList<String>()
    private val immutableConfigProviders = ArrayList<String>()

    private var joosConfig: TypeElement? = null
    private var disabled: TypeElement? = null
    private var immutableConfigProvider: TypeElement? = null
    private var mutableConfigProvider: TypeElement? = null
    private var configVariable: TypeMirror? = null
    private var postInitialized = false

    private fun info(msg: CharSequence) = processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, msg)

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (!postInitialized) {
            joosConfig = processingEnv.elementUtils.getTypeElement(joosConfigName)
            disabled = processingEnv.elementUtils.getTypeElement(disabledName)
            mutableConfigProvider = processingEnv.elementUtils.getTypeElement(mutableConfigProviderName)
            immutableConfigProvider = processingEnv.elementUtils.getTypeElement(immutableConfigProviderName)
            configVariable = processingEnv.typeUtils.getDeclaredType(
                processingEnv.elementUtils.getTypeElement(configVariableName),
                processingEnv.typeUtils.getWildcardType(null, null)
            )
            postInitialized = true
        }
        if (joosConfig != null) {
            val disabledElements = roundEnv.getElementsAnnotatedWith(disabled)
            val potentialProviders = (mutableConfigProvider?.let { roundEnv.getElementsAnnotatedWith(it) }
                ?: emptyList()) + (immutableConfigProvider?.let { roundEnv.getElementsAnnotatedWith(it) }
                ?: emptyList())
            (roundEnv.getElementsAnnotatedWith(joosConfig) + potentialProviders - disabledElements).forEach { element ->
                element.accept(ConfigElementVisitor(), joosConfig)
            }
        }
        if (roundEnv.processingOver()) {
            val resultObject = TypeSpec.classBuilder(className).addField(
                FieldSpec.builder(TypeName.BOOLEAN, "isKotlin")
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC, Modifier.FINAL).initializer("false").build()
            ).addMethod(MethodSpec.methodBuilder("getResults").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(List::class.java).beginControlFlow("try").addStatement("return \$T.asList(${
                    results.joinToString { (group, field) -> "new kotlin.Pair<>(\"$group\", ${field.first}.class.getField(\"${field.second}\"))" }
                })", Arrays::class.java).nextControlFlow("catch (\$T e)", Exception::class.java)
                .addStatement("return \$T.emptyList()", Collections::class.java).endControlFlow().build()).addMethod(
                MethodSpec.methodBuilder("getMutableConfigProviders").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(List::class.java).beginControlFlow("try").addStatement(
                        "return \$T.asList(${
                            mutableConfigProviders.joinToString()
                        })", Arrays::class.java
                    ).nextControlFlow("catch (\$T e)", Exception::class.java)
                    .addStatement("return \$T.emptyList()", Collections::class.java).endControlFlow().build()
            ).addMethod(
                MethodSpec.methodBuilder("getImmutableConfigProviders").addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(List::class.java).beginControlFlow("try").addStatement(
                        "return \$T.asList(${
                            immutableConfigProviders.joinToString()
                        })", Arrays::class.java
                    ).nextControlFlow("catch (\$T e)", Exception::class.java)
                    .addStatement("return \$T.emptyList()", Collections::class.java).endControlFlow().build()
            ).build()
            JavaFile.builder(classPackage, resultObject).build().writeTo(processingEnv.filer)
        }
        return true
    }

    inner class ConfigElementVisitor : ElementVisitor<Unit, TypeElement?> {
        override fun visit(p0: Element?, p1: TypeElement?) {
        }

        override fun visitPackage(p0: PackageElement?, p1: TypeElement?) {
        }

        override fun visitType(p0: TypeElement?, p1: TypeElement?) {
            if (p0?.modifiers?.contains(Modifier.PUBLIC) != true) return
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
            results += configMembers
        }

        override fun visitVariable(p0: VariableElement, p1: TypeElement?) {
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
                val parent = possibleParent ?: return
                if (!parent.modifiers.contains(Modifier.PUBLIC)) return
                results += listOf(
                    (getAltName(p0, p1) ?: getAltName(parent, p1)
                    ?: parent.simpleName.toString()) to (parent.qualifiedName.toString() to p0.simpleName.toString())
                )
            }
        }

        private fun getAltName(element: Element?, joosConfig: TypeElement?): String? {
            if (element == null || joosConfig == null) return null
            val annotation =
                element.annotationMirrors?.find { it.annotationType.toString() == joosConfig.qualifiedName?.toString() }
                    ?: return null
            return annotation.elementValues?.values?.firstOrNull()?.value?.toString()?.ifEmpty { null }
        }

        override fun visitExecutable(p0: ExecutableElement?, p1: TypeElement?) {
            if (p0 == null) return
            info("Processing element ${p0.simpleName}")
            if (p0.kind != ElementKind.METHOD) return
            if (!p0.modifiers.contains(Modifier.STATIC)) return
            if (p0.modifiers.contains(Modifier.ABSTRACT) || !p0.modifiers.contains(Modifier.PUBLIC)) return
            if (p0.typeParameters?.isNotEmpty() == true) return
            val receiver: TypeMirror? = p0.receiverType
            if (!(receiver == null || receiver.kind == TypeKind.NONE)) return
            if (p0.parameters.size != 1) return
            println(p0.annotationMirrors?.get(0)?.annotationType.toString())
            val isMutableProvider =
                p0.annotationMirrors?.any { it.annotationType.toString() == joosConfig?.qualifiedName?.toString() }
            if (isMutableProvider == null) {
                info("Comparison failed.")
                return
            }
            if (isMutableProvider) {
                info("Element is for a mutable config provider.")
                if (!processingEnv.typeUtils.isSubtype(
                        p0.returnType, configVariable ?: return
                    )
                ) return
            } else {
                info("Function is for an immutable provider.")
                if (p0.parameters.size != 2) {
                    info("Function does not accept 2 parameters.")
                    return
                }
                val param1 = p0.parameters[0].enclosingElement.let {
                    if (it.kind == ElementKind.METHOD) it as ExecutableElement else null
                }
                val param2 = p0.parameters[1].enclosingElement.let {
                    if (it.kind == ElementKind.METHOD) it as ExecutableElement else null
                }
                if (param1 == null || param2 == null) {
                    info("Function parameters are not lambdas.")
                    return
                }
                if (param1.parameters.isNotEmpty()) {
                    info("First parameter has a parameter.")
                    return
                }
                if (param2.parameters.size != 1) {
                    info("Second parameter does not have only one parameter.")
                    return
                }
                if (!processingEnv.typeUtils.isAssignable(
                        param2.returnType,
                        processingEnv.typeUtils.getNoType(TypeKind.VOID)
                    )
                ) {
                    info("Second parameter returns a value.")
                    return
                }
                if (param1.returnType != param2.parameters[0].asType() && param1.returnType != null) {
                    info("Parameter types don't match.")
                    return
                }
            }
            val parent = p0.enclosingElement as? TypeElement ?: return
            processingEnv.messager.printMessage(
                Diagnostic.Kind.NOTE, "${parent.qualifiedName}.class.getMethod(${p0.simpleName})"
            )
            if (isMutableProvider) mutableConfigProviders += "${parent.qualifiedName}.class.getMethod(${p0.simpleName})"
            else immutableConfigProviders += "${parent.qualifiedName}.class.getMethod(${p0.simpleName})"
        }

        override fun visitTypeParameter(
            p0: TypeParameterElement?, p1: TypeElement?
        ) {
        }

        override fun visitUnknown(p0: Element?, p1: TypeElement?) {}
    }
}