import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val signingFile = providers.environmentVariable("BOOKORBIT_SIGNING_PROPERTIES").orNull
val releaseSecrets = Properties().apply {
    signingFile?.let { File(it).inputStream().use(::load) }
}

android {
    namespace = "app.bookorbit.mihonext"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "app.bookorbit.mihonext"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.6.1"
    }

    signingConfigs {
        if (signingFile != null) create("localRelease") {
            storeFile = file(releaseSecrets.getProperty("storeFile"))
            storePassword = releaseSecrets.getProperty("storePassword")
            keyAlias = releaseSecrets.getProperty("keyAlias")
            keyPassword = releaseSecrets.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingFile != null) signingConfig = signingConfigs.getByName("localRelease")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    // Host-supplied interfaces, not packaged inside APK.
    compileOnly("androidx.preference:preference:1.2.1")
    compileOnly("com.github.mihonapp:tachiyomix:1.6.0")
    compileOnly("com.squareup.okhttp3:okhttp:5.4.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    compileOnly("com.github.mihonapp:injekt:91edab2317")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("com.squareup.okhttp3:okhttp:5.4.0")
    testImplementation("com.squareup.okhttp3:mockwebserver3:5.4.0")
    testImplementation("com.squareup.okhttp3:okhttp-tls:5.4.0")
    testImplementation("junit:junit:4.13.2")
}
