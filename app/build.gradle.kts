plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.georgeslebatoon.frenchvo"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.georgeslebatoon.frenchvo"
        ndk { abiFilters += "arm64-v8a" }
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }
}

android { kotlinOptions { jvmTarget = "17" } }
dependencies { implementation(files("libs/sherpa-onnx-1.13.8.aar")) }
dependencies { testImplementation("junit:junit:4.13.2") }
