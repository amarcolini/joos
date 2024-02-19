plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version Versions.kotlin
    `maven-publish`
    id("org.jetbrains.dokka")
}

extra["dokkaName"] = "Navigation"

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
            dependencies {
                implementation("org.knowm.xchart:xchart:3.8.1")
                implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.5.0")
                implementation("org.jetbrains.lets-plot:lets-plot-image-export:4.1.0")
                implementation("org.apache.commons:commons-math3:3.6.1")
                implementation("org.ejml:ejml-core:0.41")
                implementation("org.ejml:ejml-ddense:0.41")
                implementation("space.kscience:kmath-core:0.3.0")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
                implementation("space.kscience:kmath-core:0.3.0")
            }
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
    commonMainImplementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    commonTestImplementation(kotlin("test"))
    dokkaPlugin("org.jetbrains.dokka:mathjax-plugin:${Versions.dokka}")
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

tasks.clean.configure {
    doFirst {
        delete(file("graphs"), file("config"), file("lets-plot-images"))
    }
}