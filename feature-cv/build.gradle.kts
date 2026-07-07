plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.feature_cv"
    compileSdk = 36

    defaultConfig {
        minSdk = 28 // Updated for Android 9+ support
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    // КРИТИЧЕСКИ ВАЖНО ДЛЯ TENSORFLOW LITE:
    // Запрещаем сжимать tflite модели при сборке APK, иначе они не считаются из assets
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    // Базовые AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // ТОЧНО ТАК ЖЕ, КАК В :APP — Используем BOM
    implementation(platform(libs.androidx.compose.bom))

    // Теперь эти библиотеки берут те же самые версии, что и в главном модуле
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation) // Теперь он разрешится правильно!
    implementation(libs.androidx.compose.material3)

    // Dagger Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // CameraX (Захват кадров и ImageAnalysis)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // TensorFlow Lite
    implementation(libs.tf.lite)
    implementation(libs.tf.lite.support)
    implementation(libs.tf.lite.gpu)
    implementation(libs.tf.lite.gpu.api)

    // Тестирование
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(":domain"))
}
