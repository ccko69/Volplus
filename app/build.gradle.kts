plugins {
  // AGP 9.0+ has built-in Kotlin support, so the separate kotlin.android plugin is no longer needed.
  // However, the Compose Compiler plugin is still required for Compose to work.
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

android {
  namespace = "ccko.volplus"
  compileSdk = 37

  defaultConfig {
    applicationId = "ccko.volplus"
    minSdk = 30
    targetSdk = 34
    versionCode = 43
    versionName = "0.43"
  }

  buildTypes {
    getByName("debug") {
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    // Added release build type (no signing config, so it won't fail in CI)
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures {
    viewBinding = true
    buildConfig = true
    compose = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  implementation("androidx.activity:activity-compose:1.13.0")
  implementation("androidx.compose.material3:material3:1.4.0")
  implementation("androidx.compose.ui:ui:1.12.1")
  implementation("androidx.compose.foundation:foundation:1.12.1")
}