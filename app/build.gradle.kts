plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    namespace = "com.zeus97x.zbattle"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zeus97x.zbattle"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-ui-foundation"
    }

    sourceSets {
        getByName("main") {
            // Shared Compose screens are also compiled by the JVM layout harness in preview/.
            java.srcDirs("src/main/kotlin", "../ui/src/main/kotlin")
            // Existing ZPet creature PNGs are packaged in place, unchanged (monsters/*.png).
            // Future ChatGPT artwork goes under app/src/main/assets/art/ (see ArtCatalog).
            assets.srcDirs("src/main/assets", "../ZBattle-ZPet-Assets/assets")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint {
        abortOnError = true
        // Placeholder art is drawn in code; no launcher bitmap until ChatGPT branding is approved.
        disable += "MissingApplicationIcon"
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
}
