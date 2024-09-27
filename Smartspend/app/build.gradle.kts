plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
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



    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
