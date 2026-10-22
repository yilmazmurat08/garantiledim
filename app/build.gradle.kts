import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Derleme numarası (CI'da çalışma numarası, yerelde BUILD_NUMBER); telefona yeni sürüm
// eskisinin üzerine kurulabilsin diye artar.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: System.getenv("BUILD_NUMBER"))?.toIntOrNull() ?: 1

// Play yükleme anahtarının yolu; verilmezse release test anahtarıyla imzalanır.
val uploadKeystore: String? = System.getenv("UPLOAD_KEYSTORE")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.garantiledim.app"
    compileSdk = 36

    defaultConfig {
        // Play Store'a ilk yüklemeden sonra değiştirilemez.
        applicationId = "com.garantiledim.app"
        minSdk = 26
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Yalnızca test APK'ları için; herkese açık.
        create("test") {
            storeFile = file("signing/test.keystore")
            storePassword = "garantiledim-test"
            keyAlias = "test"
            keyPassword = "garantiledim-test"
        }
        // Play Console'a yüklenecek AAB için gizli yükleme anahtarı. Depoya konmaz; yolu ve
        // şifreleri ortam değişkenleriyle verilir (README > Play Store sürümü).
        if (uploadKeystore != null) {
            create("upload") {
                storeFile = file(uploadKeystore)
                storePassword = System.getenv("UPLOAD_STORE_PASSWORD")
                keyAlias = System.getenv("UPLOAD_KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("UPLOAD_KEY_PASSWORD") ?: System.getenv("UPLOAD_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName(if (uploadKeystore != null) "upload" else "test")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        animationsDisabled = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.generateKotlin", "true")
    // Veritabanı şeması her sürümde kaydedilir; sonraki sürümlerde veri kaybı olmadan geçiş yazılabilsin diye.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.coil.compose)
    implementation(libs.play.billing)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.work.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
