plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
    kotlin("kapt")
}

android {
    namespace = "com.example.smartspend"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartspend"
        minSdk = 24
        targetSdk = 34
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

    // Update Java compatibility to Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    configurations.all {
        resolutionStrategy.force("androidx.core:core:1.13.1")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Add dependencies for third-party libraries, excluding potential android.support conflicts
    implementation("com.github.AnyChart:AnyChart-Android:1.1.2") {
        exclude(group = "com.android.support")
    }
    implementation("com.github.QuadFlask:colorpicker:0.0.15") {
        exclude(group = "com.android.support")
    }
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") {
        exclude(group = "com.android.support")
    }

    // Networking library
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("org.robolectric:robolectric:4.6.1")
    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.android.gms:play-services-auth:20.3.0")

    //offlien sync
    implementation("androidx.room:room-runtime:2.5.2")
    kapt("androidx.room:room-compiler:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")


    implementation("com.google.firebase:firebase-auth:22.1.1")
    implementation("androidx.biometric:biometric:1.2.0-alpha03")
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))


}
