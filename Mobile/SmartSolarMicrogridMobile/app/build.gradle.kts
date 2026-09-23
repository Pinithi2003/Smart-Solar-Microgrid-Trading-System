plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.smartsolar.stations"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smartsolar.stations"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-member2-demo"

        // Emulator loopback to host API (Backend runs on http://localhost:5205).
        // Mirrors Frontend/SmartSolarStationUI/js/config.js API_BASE_URL.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5205\"")
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
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")

    // API layer (future ASP.NET Core integration). Retrofit only used by StationService now.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // QR generation (demo). Scanning is stubbed — full scanner belongs to Member 4.
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Real OSM tiles for the stations preview (no API key, like web Leaflet).
    // Full interactive maps still belong to Member 4.
    implementation("org.osmdroid:osmdroid-android:6.1.20")
}
