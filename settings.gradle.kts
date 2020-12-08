pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        mavenCentral()
    }
}
rootProject.name = "mvu-program"

enableFeaturePreview("GRADLE_METADATA")

include(":program")

