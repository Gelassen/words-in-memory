import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "io.github.gelassen.workinmemory"
        minSdk = 21
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = true
        dataBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.room:room-runtime:2.4.3")
    implementation("androidx.room:room-ktx:2.4.3")
    implementation("com.google.android.material:material:1.7.0")
    annotationProcessor("androidx.room:room-compiler:2.4.3")

    kapt("androidx.room:room-compiler:2.4.3")

    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.databinding:databinding-runtime:4.2.2")


    /* DI */
    implementation("com.google.dagger:dagger:2.42")
    implementation("com.google.dagger:dagger-android-support:2.42")

    kapt("com.google.dagger:dagger-compiler:2.42")
    /* kapt android tests */
    kaptAndroidTest("com.google.dagger:dagger-compiler:2.42")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    testImplementation("org.mockito:mockito-inline:3.12.4")
    testImplementation("org.mockito:mockito-core:3.12.4")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
