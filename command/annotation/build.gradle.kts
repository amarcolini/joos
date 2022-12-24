plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.jetbrains.dokka")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.compileKotlin.configure {
    kotlinOptions {
        jvmTarget = "1.8"
        apiVersion = "1.5"
    }
}

group = "$group.command"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")

    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.20-1.0.7")

    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.squareup:kotlinpoet:1.12.0")
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