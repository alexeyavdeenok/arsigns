plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.ksp) // Оставляем рабочий alias для генерации кода Hilt
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.example.feature_tts"
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
}

// Настройка компилятора Kotlin под AGP 8.8.0 без варнингов
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Базовые AndroidX зависимости
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Dagger Hilt (Обязателен для внедрения TTS-сервиса в UI-слой приложения)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Тестирование
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(":domain"))
}
