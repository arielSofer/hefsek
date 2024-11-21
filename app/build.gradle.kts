plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.hefsek"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.hefsek"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core-ktx:1.12.0")
        force("androidx.appcompat:appcompat:1.6.1")
        exclude(group = "com.android.support")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.11.0")
    
    // Calendar View
    implementation("com.github.kizitonwose:CalendarView:1.0.4")
    
    // Date & Time
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.6")
    
    // ViewModel
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("com.kosherjava:zmanim:2.5.0")
}