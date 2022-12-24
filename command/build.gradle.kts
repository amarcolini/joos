plugins {
    `maven-publish`
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.dokka")
}

extra {
    extra["dokkaName"] = "Command"
}
repositories {
    google()
    maven { url = uri("https://maven.brott.dev/") }
    mavenCentral()
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")

    implementation("org.apache.commons:commons-math3:3.6.1")
    compileOnly("org.firstinspires.ftc:RobotCore:8.0.0")
    compileOnly("org.firstinspires.ftc:Hardware:8.0.0")
    api("com.acmerobotics.dashboard:dashboard:0.4.7")
    api(project(":navigation"))
    implementation(project(":command:annotation"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    testImplementation("org.knowm.xchart:xchart:3.8.2")
}

tasks.clean.configure {
    doFirst {
        delete("graphs")
    }
}

android {
    compileSdk = 30
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets {
        get("main").java.srcDirs("src/main/kotlin")
        get("test").java.srcDirs("src/test/kotlin")
    }
    defaultConfig {
        minSdk = 24
    }
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    from(android.sourceSets["main"].java.srcDirs)
    archiveClassifier.set("sources")
}

project.afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "command"
                artifact(sourcesJar.get())
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
}