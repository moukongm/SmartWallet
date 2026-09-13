plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.example.smartwallet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.smartwallet"
        minSdk = 26
        targetSdk = 36
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

kapt {
    arguments {
        arg("AROUTER_MODULE_NAME", "app")
    }
}

dependencies {
    implementation(project(":business:home:impl"))
    implementation(project(":business:bill:impl"))
    implementation(project(":business:statistics:impl"))
    implementation(project(":business:budget:impl"))
    implementation(project(":business:profile:api"))
    implementation(project(":business:profile:impl"))

    implementation(project(":foundation:common"))

    kapt(libs.arouter.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(project(":business:bill:api"))
    androidTestImplementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.mmkv)
}
