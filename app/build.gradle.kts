plugins {
    id("com.android.application")
    kotlin("android")
}

dependencies {
    implementation(project(":chart-compose"))
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.compose.foundation:foundation:1.3.0")
    implementation("com.google.code.gson:gson:2.8.9")
}

android {
    compileSdk = 33
    defaultConfig {
        applicationId = "com.gmail.tiomamaster.chart"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "0.1"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
