plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "dev.jarvis.assistant"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.jarvis.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 1000
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val keystorePath = providers.environmentVariable("JARVIS_KEYSTORE_FILE").orNull
    if (keystorePath != null) {
        signingConfigs {
            create("jarvisRelease") {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("JARVIS_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("JARVIS_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("JARVIS_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("jarvisRelease")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.17.0")
    testImplementation("junit:junit:4.13.2")
}
