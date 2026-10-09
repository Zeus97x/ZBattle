plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.compose") version "2.0.21"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

val composeVersion = "1.5.12"
val osArch = System.getProperty("os.arch").let { if (it == "aarch64" || it == "arm64") "arm64" else "x64" }
val osName = System.getProperty("os.name").lowercase().let {
    when {
        it.contains("mac") -> "macos"
        it.contains("win") -> "windows"
        else -> "linux"
    }
}

sourceSets {
    main { kotlin.srcDirs("src/main/kotlin", "../core/src/main/kotlin", "../ui/src/main/kotlin") }
    test { kotlin.srcDirs("src/test/kotlin", "../core/src/test/kotlin") }
}

dependencies {
    implementation("org.jetbrains.compose.ui:ui-desktop:$composeVersion")
    implementation("org.jetbrains.compose.foundation:foundation-desktop:$composeVersion")
    implementation("org.jetbrains.compose.material3:material3-desktop:$composeVersion")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:$composeVersion")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-$osName-$osArch:0.7.85.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    val assetPack = rootProject.file("../ZBattle-ZPet-Assets").absolutePath
    systemProperty("zbattle.assetPack", assetPack)
    systemProperty("zbattle.screenshots", layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
    systemProperty("java.awt.headless", "true")
    maxHeapSize = "3g"
}
