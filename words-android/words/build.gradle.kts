plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hius74.words"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hius74.words"
        minSdk = 34
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
}

