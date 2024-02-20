import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask

plugins {
//    kotlin("jvm") version Versions.kotlin apply false
    id("com.android.library") version Versions.android apply false
    id("org.jetbrains.kotlin.android") version Versions.kotlin apply false
    id("org.jetbrains.dokka") version Versions.dokka
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:${Versions.dokka}")
    }
}

tasks.dokkaHtmlMultiModule {
    moduleName.set("")
    outputDirectory.set(buildDir.resolve("kotlin_docs"))
    pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
        customStyleSheets = listOf(file("/docAssets/logo-styles.css"))
        footerMessage = "Made by Alessandro Marcolini"
    }
}

tasks.register("dokkaHtmlMultiModuleJava") {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    doLast {
        tasks.dokkaHtmlMultiModule {
            outputDirectory.set(buildDir.resolve("java_docs"))
        }
        project.dependencies {
            dokkaHtmlMultiModulePlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${Versions.dokka}")
        }
        subprojects.forEach {
            if (it.configurations.findByName("dokkaHtmlPartialPlugin") == null) return@forEach
            it.dependencies {
                "dokkaHtmlPartialPlugin"("org.jetbrains.dokka:kotlin-as-java-plugin:${Versions.dokka}")
            }
        }
    }
    finalizedBy(tasks.dokkaHtmlMultiModule)
}

subprojects.forEach {
    it.afterEvaluate {
        if (!extra.has("dokkaName")) {
            tasks.withType<AbstractDokkaLeafTask>().configureEach {
                moduleName.set("null")
                dokkaSourceSets {
                    configureEach {
                        suppress.set(true)
                    }
                }
            }
            return@afterEvaluate
        }
        val dokkaName = extra["dokkaName"] as String
        tasks.withType<AbstractDokkaLeafTask>().configureEach {
            moduleName.set(dokkaName)
            outputDirectory.set(buildDir.resolve("dokka"))
            suppressInheritedMembers.set(true)
            dokkaSourceSets {
                configureEach {
                    if (name in listOf("main", "commonMain", "jvmMain")) {
                        includes.from("module.md")
                        reportUndocumented.set(true)
                        documentedVisibilities.set(
                            setOf(
                                org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC,
                                org.jetbrains.dokka.DokkaConfiguration.Visibility.PROTECTED
                            )
                        )
                        pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
                            footerMessage = "Made by Alessandro Marcolini"
                        }
                    } else {
                        suppress.set(true)
                    }
                }
            }
        }
    }
}

allprojects {
    group = "com.amarcolini.joos"
    version = Versions.library
    description = "A comprehensive kotlin library designed for FTC."

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjvm-default=all") //all or all-compatibility
        }
    }

    rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
        rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion = "16.0.0"
    }
//    tasks.findByPath(":gui:mergeClasses")?.configure<Copy> {
//        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//    }

//    tasks.withType(GenerateModuleMetadata) {
//        enabled = false
//    }
}