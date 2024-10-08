import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.jetbrains.dokka")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    sourceSets {
        get("main").kotlin.srcDirs("src/main/kotlin")
        get("test").kotlin.srcDirs("src/test/kotlin")
    }
    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
}

group = "$group.command"

dependencies {
    //noinspection GradleDependency
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.20-1.0.14")
    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.squareup:kotlinpoet:1.13.1")
}
repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            artifactId = "annotation"
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
            url = uri("../../testRepo")
        }
    }
}