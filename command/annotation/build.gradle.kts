plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("org.jetbrains.dokka")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                apiVersion = "1.5"
            }
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR).browser()

    sourceSets {
        val commonMain by getting
        val jvmMain by getting {
            dependencies {
                implementation("com.google.devtools.ksp:symbol-processing-api:1.8.10-1.0.9")
                implementation("com.squareup:javapoet:1.13.0")
                implementation("com.squareup:kotlinpoet:1.12.0")
            }
        }
    }
}

group = "$group.command"

dependencies {
    commonMainImplementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    commonMainImplementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
}
repositories {
    mavenCentral()
}

publishing {
    publications.filterIsInstance<MavenPublication>().forEach {
        it.pom {
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

    repositories {
        maven {
            name = "testRepo"
            url = uri("../../testRepo")
        }
    }
}