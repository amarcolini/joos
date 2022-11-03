import org.beryx.jlink.util.JdkUtil.JdkDownloadOptions

plugins {
    kotlin("jvm")
    application
    `maven-publish`
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
    id("org.jetbrains.dokka")
}

extra {
    extra["dokkaName"] = "GUI"
}

application {
    mainClass.set("${rootProject.group}.gui.ApplicationKt")
    mainModule.set("joos.gui")
    applicationDefaultJvmArgs = listOf("--add-opens=javafx.graphics/javafx.scene=tornadofx")
}

javafx {
    version = "15.0.1"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.base")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    registerFeature("win") {
        usingSourceSet(sourceSets["main"])
//        capability("com.amarcolini.joos", "gui", lib_version)
    }
    registerFeature("mac") {
        usingSourceSet(sourceSets["main"])
//        capability("com.amarcolini.joos", "gui", lib_version)
    }
    registerFeature("linux") {
        usingSourceSet(sourceSets["main"])
//        capability("com.amarcolini.joos", "gui", lib_version)
    }
}

configurations {
    create("imageDependency") {
        extendsFrom(implementation.get())
        isCanBeResolved = true
    }

    "winRuntimeElements" {
        attributes {
            attribute(
                OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE,
                objects.named(OperatingSystemFamily.WINDOWS)
            )
        }
    }

    "winApiElements" {
        attributes {
            attribute(
                OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE,
                objects.named(OperatingSystemFamily.WINDOWS)
            )
        }
    }

    "macRuntimeElements" {
        attributes {
            attribute(
                OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE,
                objects.named(OperatingSystemFamily.MACOS)
            )
        }
    }

    "macApiElements" {
        attributes {
            attribute(
                OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE,
                objects.named(OperatingSystemFamily.MACOS)
            )
        }
    }

    "linuxRuntimeElements" {
        attributes {
            attribute(
                OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE,
                objects.named(OperatingSystemFamily.LINUX)
            )
        }
    }

    "linuxApiElements" {
        attributes {
            attribute(
                OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE,
                objects.named(OperatingSystemFamily.LINUX)
            )
        }
    }
}

dependencies {
    api(project(":navigation"))
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")

    implementation("io.github.classgraph:classgraph:4.8.147")

    "winImplementation"("org.openjfx:javafx-controls:$javafx.version:win")
    "macImplementation"("org.openjfx:javafx-controls:$javafx.version:mac")
    "linuxImplementation"("org.openjfx:javafx-controls:$javafx.version:linux")

    "imageDependency"("org.openjfx:javafx-graphics:$javafx.version:win")
    "imageDependency"("org.openjfx:javafx-graphics:$javafx.version:mac")
    "imageDependency"("org.openjfx:javafx-graphics:$javafx.version:linux")
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "11"
        apiVersion = "1.5"
    }
    destinationDirectory.set(tasks.compileJava.get().destinationDirectory)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            artifactId = "gui"
            pom {
                name.set("Joos")
                description.set("A comprehensive kotlin library designed for FTC.")
                url.set("https://github.com/amarcolini/joos")
                developers {
                    developer {
                        id.set("amarcolini")
                        name.set("Alessandro Marcolini")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "testRepo"
            url = uri("../testRepo")
        }
    }
}

jlink {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    configuration.set("imageDependency")
    addExtraDependencies("javafx")
    forceMerge("javafx")

    mergedModule {
        excludeProvides(mapOf("servicePattern" to "com.fasterxml.jackson.*"))
        uses("kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader")
        uses("kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition")
        uses("kotlin.reflect.jvm.internal.impl.util.ModuleVisibilityHelper")
    }

    launcher {
        name = "launcher"
        jvmArgs = listOf("--add-opens=javafx.graphics/javafx.scene=com.amarcolini.joos.merged.module")
        noConsole = true
    }

    targetPlatform("linux") {
        setJdkHome(
            jdkDownload(
                "https://api.adoptium.net/v3/binary/latest/11/ga/linux/x64/jdk/hotspot/normal/eclipse",
                closureOf<JdkDownloadOptions> {
                    downloadDir = "./jdks/$name/"
                    archiveExtension = "tar.gz"
                })
        )
    }

    targetPlatform("win") {
        setJdkHome(
            jdkDownload("https://api.adoptium.net/v3/binary/latest/11/ga/windows/x64/jdk/hotspot/normal/eclipse",
                closureOf<JdkDownloadOptions> {
                    downloadDir = "./jdks/$name/"
                    archiveExtension = "zip"
                })
        )
    }

    targetPlatform("mac") {
        setJdkHome(
            jdkDownload("https://api.adoptium.net/v3/binary/latest/11/ga/mac/x64/jdk/hotspot/normal/eclipse",
                closureOf<JdkDownloadOptions> {
                    downloadDir = "./jdks/$name/"
                    archiveExtension = "tar.gz"
                })
        )
    }
}