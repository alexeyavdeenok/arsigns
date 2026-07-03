plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Плагин компилятора Compose для Kotlin 2.x
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.example.artrafficsign"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.artrafficsign"
        minSdk = 28 // Обновлено для поддержки Android 9 (API 28) и выше
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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

    // Настройка для поддержки устройств с размером страницы 16 КБ
    // useLegacyPackaging = true заставляет систему извлекать .so библиотеки при установке,
    // что решает проблему выравнивания сегментов LOAD для 16 КБ страниц.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

// Настройка компилятора Kotlin под AGP 8.8.0 без варнингов
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Подключаем наши внутренние модули (Архитектура Feature-by-Module)
    implementation(project(":core-data"))
    implementation(project(":feature-cv"))
    implementation(project(":feature-tts"))
    implementation(project(":domain"))

    // Базовые AndroidX зависимости
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation(libs.androidx.activity.compose)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Jetpack Compose (Интерфейс приложения)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // CameraX (Нужно для PreviewView в UI)
    implementation(libs.camera.view)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.camera2)
    implementation(libs.camera.core)

    // Dagger Hilt (Главный контейнер DI для всего приложения)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Тестирование
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
