plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.xssscanner"
    compileSdk = 34 // TODO(verify-version): use the latest stable SDK in the current Android Studio template

    defaultConfig {
        applicationId = "com.example.xssscanner"
        minSdk = 24
        targetSdk = 34 // TODO(verify-version): same as compileSdk
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // TODO: needs confirmation — enable R8 for release builds?
        }
    }

    buildFeatures {
        buildConfig = true // required: BuildConfig.VERSION_NAME is used in exports (§3.2)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.okhttp3)
    implementation(libs.jsoup)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.webkit)
}
