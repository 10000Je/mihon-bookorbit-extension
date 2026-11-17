plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.bookorbit.mihonext"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.bookorbit.mihonext"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-dev"
    }

    buildTypes {
        release { isMinifyEnabled = false }
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
    // Host-supplied interfaces, not packaged inside APK.
    compileOnly("com.github.mihonapp:tachiyomix:1.6")
    testImplementation("junit:junit:4.13.2")
}
