rootProject.name = "joos"
include(":navigation")
include(":gui")
include(":gui:frontend")
include(":command")
include(":command:annotation")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}