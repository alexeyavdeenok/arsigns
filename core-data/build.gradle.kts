plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.example.core_data"
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

    // Возвращаем классический синтаксис, так как встроенный Котлин отключен
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Базовые AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Dagger Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room DB
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore & Crypto
    implementation(libs.datastore.pref)
    implementation(libs.androidx.security.crypto)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Тестирование
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(":domain"))
}
