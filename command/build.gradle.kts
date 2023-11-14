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
    compileOnly("org.firstinspires.ftc:RobotCore:${Versions.ftc}")
    compileOnly("org.firstinspires.ftc:Hardware:${Versions.ftc}")
    compileOnly("org.firstinspires.ftc:FtcCommon:${Versions.ftc}")
    api("com.acmerobotics.dashboard:dashboard:0.4.12") {
        exclude(group = "org.firstinspires.ftc")
    }
    api(project(":navigation"))
    api(project(":command:annotation"))

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.firstinspires.ftc:RobotCore:${Versions.ftc}")
    testImplementation("org.firstinspires.ftc:Hardware:${Versions.ftc}")
    testImplementation("org.firstinspires.ftc:FtcCommon:${Versions.ftc}")
    testImplementation("org.robolectric:robolectric:4.10.3")

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