plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    // Tests verify catalogues against the untouched ZPet asset pack and reference sources.
    systemProperty("zbattle.assetPack", rootProject.file("ZBattle-ZPet-Assets").absolutePath)
}
