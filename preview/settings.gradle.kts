// Standalone JVM build: renders the shared Compose screens headlessly with Compose Multiplatform
// desktop and runs the :core tests, without the Android SDK or Google Maven.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
rootProject.name = "zbattle-preview"
