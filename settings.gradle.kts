rootProject.name = "joos"
include(":navigation")
include(":command")
include(":ftc")
include(":ftc:annotation")
include(":gui")
include(":gui:frontend")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
