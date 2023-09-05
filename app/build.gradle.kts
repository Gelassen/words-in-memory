import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("io.sentry.android.gradle") version "3.11.1"
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "io.github.gelassen.wordsinmemory.debug"
        minSdk = 21
        targetSdk = 33
        versionCode = 7
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] =
                    "$projectDir/schemas"
            }
        }
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
    testOptions {
        animationsDisabled = false
    }
    namespace = "io.github.gelassen.wordinmemory"
}

dependencies {

//    implementation fileTree(dir: 'libs', include: ['*.jar'])
    implementation(files("$projectDir/libs/pipinyin-0.9.1.jar"))

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    implementation("androidx.work:work-runtime:2.8.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.databinding:databinding-runtime:8.1.0")
    implementation("androidx.preference:preference:1.2.1")

    implementation("com.google.mlkit:translate:17.0.1")
//    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
//    implementation("com.google.android.gms:play-services-mlkit-text-recognition-chinese:16.0.0")
    implementation("com.google.mlkit:translate:17.0.1")
    implementation("com.google.android.gms:play-services-mlkit-language-id:17.0.0")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    implementation("com.google.android.material:material:1.9.0")
    implementation("de.siegmar:fastcsv:2.2.2")
    /* Sentry */
    implementation("io.sentry:sentry-android:6.25.0") {
        exclude(group = "androidx.lifecycle", module = "lifecycle-process")
        exclude(group = "androidx.lifecycle", module = "lifecycle-common-java8")
    }
    /* DI */
    implementation("com.google.dagger:dagger:2.42")
    implementation("com.google.dagger:dagger-android-support:2.42")

    annotationProcessor("androidx.room:room-compiler:2.5.2")

    kapt("androidx.room:room-compiler:2.5.2")
    kapt("com.google.dagger:dagger-compiler:2.42")

    /* kapt android tests */
    kaptAndroidTest("com.google.dagger:dagger-compiler:2.42")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
    testImplementation("org.mockito:mockito-inline:3.12.4")
    testImplementation("org.mockito:mockito-core:3.12.4")

    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
    androidTestImplementation( "androidx.test.espresso:espresso-contrib:3.5.0")
}
