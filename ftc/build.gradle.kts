plugins {
    `maven-publish`
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka")
}

extra {
    extra["dokkaName"] = "FTC"
}
repositories {
    google()
    maven { url = uri("https://maven.brott.dev/") }
    mavenCentral()
}

tasks.withType<Test> {
//    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {
    //noinspection GradleDependency
    api("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")

    compileOnly("org.firstinspires.ftc:RobotCore:${Versions.ftc}")
    compileOnly("org.firstinspires.ftc:Hardware:${Versions.ftc}")
    compileOnly("org.firstinspires.ftc:FtcCommon:${Versions.ftc}")
    api("com.acmerobotics.dashboard:dashboard:0.4.16") {
        exclude(group = "org.firstinspires.ftc")
    }
    api(project(":navigation"))
    api(project(":command"))
    api(project(":ftc:annotation"))

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

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    sourceSets {
        get("main").kotlin.srcDirs("src/main/kotlin")
        get("test").kotlin.srcDirs("src/test/kotlin")
    }
}

android {
    compileSdk = 30
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets {
//        get("main").java.srcDirs("src/main/kotlin")
        get("test").java.srcDirs("src/test/kotlin")
    }
    defaultConfig {
        minSdk = 24
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            // this doesn't work for some reason
//            withJavadocJar()
        }
    }
    kotlinOptions.jvmTarget = "1.8"
    buildToolsVersion = "30.0.3"
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                artifactId = "command"
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