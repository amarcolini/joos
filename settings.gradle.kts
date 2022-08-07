rootProject.name = "joos"
include(":navigation")
include(":gui")
include(":command")
include(":command:annotation")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}