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

// Play Console > Para kazanma kurulumu > Lisanslama'daki Base64 kodlu RSA genel anahtarı. Satın almaların
// Google Play tarafından imzalandığını cihazda doğrulamak için kullanılır. Genel anahtardır, gizli değildir.
val playLicenseKey: String = (System.getenv("PLAY_LICENSE_KEY")?.takeIf { it.isNotBlank() } ?: (findProperty("garantiledim.playLicenseKey") as String?))
    .orEmpty().replace(Regex("\\s"), "")

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
        buildConfigField("String", "PLAY_LICENSE_KEY", "\"$playLicenseKey\"")
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

// Play'e yüklenecek AAB lisans anahtarı olmadan üretilmez: anahtarsız sürümde satın almalar doğrulanamaz
// ve Premium hiç açılmaz.
tasks.matching { it.name == "bundleRelease" }.configureEach {
    doFirst {
        // İlk kapalı test sürümü anahtarsız yüklenebilir (Play lisans anahtarı ancak uygulama oluşturulunca
        // görünür); bu sürümde Premium hiçbir şekilde açılmaz. Premium ürünü anahtarlı sürümden önce etkinleştirilmez.
        val allowMissing = System.getenv("ALLOW_MISSING_PLAY_LICENSE_KEY") == "true"
        if (playLicenseKey.isBlank() && !allowMissing) {
            throw GradleException(
                "Play lisans anahtarı eksik: gradle.properties içindeki garantiledim.playLicenseKey " +
                    "ya da PLAY_LICENSE_KEY ortam değişkeni (Play Console > Para kazanma kurulumu > Lisanslama)."
            )
        }
    }
}
