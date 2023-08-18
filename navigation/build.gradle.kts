plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version Versions.kotlin
    `maven-publish`
    id("org.jetbrains.dokka")
}

extra["dokkaName"] = "Navigation"

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


    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val jvmMain by getting {
            dependencies {
                implementation("org.apache.commons:commons-math3:3.6.1")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.knowm.xchart:xchart:3.8.1")
                implementation("org.apache.commons:commons-math3:3.6.1")
                implementation("org.ejml:ejml-core:0.41")
                implementation("org.ejml:ejml-ddense:0.41")
                implementation("space.kscience:kmath-core:0.3.0")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("space.kscience:kmath-core:0.3.0")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
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
    commonMainImplementation(project(mapOf("path" to ":command:annotation")))
    commonMainApi("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    commonMainImplementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    commonTestImplementation(kotlin("test"))
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

//val sourcesJar = tasks.register<Jar>("sourcesJar") {
//    from(sourceSets["main"].allSource)
//    archiveClassifier.set("sources")
//}

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
        delete(file("graphs"), file("config"))
    }
}