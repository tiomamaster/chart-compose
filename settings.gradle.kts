pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Chart"
include(":app", ":chart-view", ":chart-compose")
