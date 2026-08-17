plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mathprogress.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mathprogress.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.3.2"
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
    // Намеренно без сторонних библиотек: первая версия полностью локальная.
}
