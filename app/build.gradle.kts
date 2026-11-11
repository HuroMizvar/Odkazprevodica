plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Firebase sa zapne, iba ak je v priečinku app/ súbor google-services.json.
// Bez neho aplikácia beží v testovacom režime (všetko v telefóne).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "sk.odkazprevodica.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "sk.odkazprevodica.app"
        minSdk = 26
        // Google Play od 31. 8. 2026 vyžaduje Android 16 (API 36).
        targetSdk = 36
        // Číslo verzie = číslo zostavenia na GitHube (tag build-N v Releases).
        // Podľa neho aplikácia v Nastaveniach pozná, že je k dispozícii novšia verzia.
        val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 3
        versionCode = build
        versionName = "1.0.$build"
    }

    // Testovacia verzia (APK z GitHubu): pevný testovací podpis, aby sa nové
    // verzie dali nainštalovať cez staršie. Nie je to podpis pre Google Play.
    // Verzia pre Google Play (AAB): podpis „upload key“ z GitHub Secrets.
    val releaseKeystore = System.getenv("RELEASE_KEYSTORE_FILE")?.let { file(it) }
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseKeystore != null && releaseKeystore.exists()) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            // aktualizácia z GitHubu priamo v aplikácii (iba testovacia verzia)
            buildConfigField("boolean", "SELF_UPDATE", "true")
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            // Google Play nedovoľuje aplikácii aktualizovať sa inak ako cez Play.
            buildConfigField("boolean", "SELF_UPDATE", "false")
            // Zmenšovanie kódu (R8) je zatiaľ vypnuté – menšie riziko pádu
            // v prvých verziách. Zapnúť neskôr po otestovaní.
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.guava)
    implementation(libs.mlkit.text.recognition)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    debugImplementation(libs.androidx.ui.tooling)
}
