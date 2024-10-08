import dev.icerock.gradle.MRVisibility

plugins {
    kotlin("multiplatform")
    id("dev.icerock.mobile.multiplatform-resources") version Versions.moko
//    application
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

kotlin {
    js(IR) {
        binaries.executable()
        browser()
    }

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }

    val doodleVersion = "0.10.2"
    val coroutinesVersion = "1.8.1"

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.nacular.doodle:core:$doodleVersion")
                implementation("io.nacular.doodle:animation:$doodleVersion")
                implementation("io.nacular.doodle:controls:$doodleVersion")
                implementation("io.nacular.doodle:themes:$doodleVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                api(project(":navigation"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
                implementation("dev.icerock.moko:resources:${Versions.moko}")

                // Optional
                // implementation ("io.nacular.doodle:controls:$doodleVersion" )
                // implementation ("io.nacular.doodle:animation:$doodleVersion")
                // implementation ("io.nacular.doodle:themes:$doodleVersion"   )
            }
        }

        val commonTest by getting {
            dependencies {
                implementation("dev.icerock.moko:resources-test:${Versions.moko}")
            }
        }

        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation("io.nacular.doodle:browser:$doodleVersion")
            }
        }

        val jvmMain by getting {
            dependsOn(commonMain)
            dependencies {
                val osName = System.getProperty("os.name")
                val targetOs = when {
                    osName == "Mac OS X" -> "macos"
                    osName.startsWith("Win") -> "windows"
                    osName.startsWith("Linux") -> "linux"
                    else -> error("Unsupported OS: $osName")
                }

                val targetArch = when (val osArch = System.getProperty("os.arch")) {
                    "x86_64", "amd64" -> "x64"
                    "aarch64" -> "arm64"
                    else -> error("Unsupported arch: $osArch")
                }

                val target = "$targetOs-$targetArch"

                implementation("io.nacular.doodle:desktop-jvm-$target:$doodleVersion") // Desktop apps are tied to specific platforms
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

multiplatformResources {
    resourcesPackage.set("com.amarcolini.joos.frontend") // required
    resourcesVisibility.set(MRVisibility.Public) // optional, default Public
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}