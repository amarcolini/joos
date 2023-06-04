plugins {
    kotlin("js")
}

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        binaries.executable()
        browser()
    }

    val doodleVersion = "0.9.2"
    val coroutinesVersion = "1.6.4"

    dependencies {
        implementation("io.nacular.doodle:core:$doodleVersion")
        implementation("io.nacular.doodle:browser:$doodleVersion")
        implementation("io.nacular.doodle:animation:$doodleVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
        implementation(project(":navigation"))

        // Optional
        // implementation ("io.nacular.doodle:controls:$doodleVersion" )
        // implementation ("io.nacular.doodle:animation:$doodleVersion")
        // implementation ("io.nacular.doodle:themes:$doodleVersion"   )
    }
}