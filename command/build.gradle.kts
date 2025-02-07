plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("org.jetbrains.dokka")
}

extra["dokkaName"] = "Command"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
//                apiVersion = "1.8.20"
            }
        }
//        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useFirefox()
                }
            }
        }
        binaries.executable()
        generateTypeScriptDefinitions()
    }
    withSourcesJar()


    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val jvmTest by getting {
        }
        val jsTest by getting {
        }
        all {
            languageSettings.apply {
                optIn("kotlin.js.ExperimentalJsExport")
            }
        }
    }
}

dependencies {
    commonMainApi("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    commonMainApi(project(":navigation"))
//    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
//    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.3")
//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
//
//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
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
            url = uri("../testRepo")
        }
    }
}