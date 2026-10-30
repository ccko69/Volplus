plugins {
  // Removed the Kotlin Android plugin. AGP 9.0+ provides Kotlin support out of the box.
  id("com.android.application")
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
      isMinifyEnabled = true
      isShrinkResources = true
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