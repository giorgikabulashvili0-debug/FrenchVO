plugins { id("com.android.application") }
android {
 namespace = "com.georgeslebatoon.frenchvo"
 compileSdk = 35
 defaultConfig { applicationId = "com.georgeslebatoon.frenchvo.expressive"; minSdk = 29; targetSdk = 35; versionCode = 3; versionName = "3.0" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { testImplementation("junit:junit:4.13.2") }
