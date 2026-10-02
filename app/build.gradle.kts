plugins {
    id("com.android.application")
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
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
