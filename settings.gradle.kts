rootProject.name = "joos"
include(":navigation")
include(":gui")
include(":gui:frontend")
include(":ftc")
include(":ftc:annotation")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
include(":command")
