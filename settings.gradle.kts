pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ZBattle"
// :core holds platform-free catalogues/state; :app is the Android UI.
// The JVM-only layout harness lives in preview/ as a separate build (see preview/README.md).
include(":core", ":app")
