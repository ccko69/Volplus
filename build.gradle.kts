// Root build.gradle.kts
plugins {
  id("com.android.application") version "9.1.0" apply false
  id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
