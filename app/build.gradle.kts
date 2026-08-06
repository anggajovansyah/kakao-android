import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Membaca rahasia MAPS_API_KEY dari file .env atau local.properties
val envProperties = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    envProperties.load(FileInputStream(envFile))
}
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    envProperties.load(FileInputStream(localPropertiesFile))
}
val mapsApiKey: String = envProperties.getProperty("MAPS_API_KEY") ?: "YOUR_MAPS_API_KEY_HERE"


android {
    namespace = "com.beraucoal.kakao"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.beraucoal.kakao"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // CameraX (for capturing KTP photo)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ML Kit Text Recognition (on-device, free)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // ML Kit Object Detection & Tracking (on-device)
    implementation("com.google.mlkit:object-detection:17.0.2")

    // Networking (NocoBase API + WhatsApp OTP provider)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Maps for kebun mapping (pinning garden location & GIS utilities)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
}
