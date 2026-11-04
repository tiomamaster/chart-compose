plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.gmail.tiomamaster.chart"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.gmail.tiomamaster.chart"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":chart-compose"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.gson)
}
