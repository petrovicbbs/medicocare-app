plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.medicocare.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.medicocare.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// play-services-ads (preko svojih tranzitivnih zavisnosti) povlači Guava-in prazan
// "conflict avoidance" artefakt com.google.guava:listenablefuture:1.0, koji zna da se
// sudari sa pravim com.google.common.util.concurrent.ListenableFuture koji koristi CameraX
// (ProcessCameraProvider.getInstance(...).addListener(...) u ScanBarcodeScreen.kt) — rezultat
// je "Cannot access class ListenableFuture" u kspDebugKotlin/compileDebugKotlin. Standardna
// popravka: isključiti prazan artefakt svuda i naterati Gradle da uvek koristi pravu Guava-u.
configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.appcompat:appcompat:1.7.0") // per-app jezik (AppCompatDelegate.setApplicationLocales)
    implementation("com.google.android.gms:play-services-ads:23.6.0") // banner reklama (Premium+ uklanja)
    implementation("com.google.guava:guava:33.6.0-android") // prava ListenableFuture (vidi napomenu iznad)

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager (reschedule after boot)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // CameraX (za skeniranje barkoda)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ML Kit — prepoznavanje barkoda (radi na uređaju, bez interneta)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
