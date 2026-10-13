
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.senati.mitiemposemanal.mitiemposemanal"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.senati.mitiemposemanal.aespinoza"
        minSdk = 24
        targetSdk = 36

        // Sprint 2
        // Sprint 3
        versionCode = 4
        versionName = "1.0.0"
        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

        // Permite utilizar LocalDateTime y Duration con API 24
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Desugaring para fechas y duración
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
