plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "app.lightalert"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.lightalert"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        val telegramApiId = providers.gradleProperty("TELEGRAM_API_ID").orNull
            ?: providers.environmentVariable("TELEGRAM_API_ID").orNull
            ?: "0"
        val telegramApiHash = providers.gradleProperty("TELEGRAM_API_HASH").orNull
            ?: providers.environmentVariable("TELEGRAM_API_HASH").orNull
            ?: ""
        buildConfigField("int", "TELEGRAM_API_ID", "${telegramApiId.toIntOrNull() ?: 0}")
        val escapedTelegramApiHash = telegramApiHash.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "")\n        buildConfigField("String", "TELEGRAM_API_HASH", "\"${escapedTelegramApiHash}\"")

        ndk {
            // Keep the APK lean while supporting ARM phones and tablets.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.gson)
    implementation(libs.tdlib.core)
    implementation(libs.tdlib.ktx)

    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
