plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val releaseStorePath = System.getenv("LIGHTALERT_KEYSTORE_PATH")
val releaseStorePassword = System.getenv("LIGHTALERT_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("LIGHTALERT_KEY_ALIAS")
val releaseKeyPassword = System.getenv("LIGHTALERT_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }


android {
    namespace = "app.lightalert"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.lightalert"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"

        ndk {
            // Keep the APK lean while supporting ARM phones and tablets.
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
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

    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
