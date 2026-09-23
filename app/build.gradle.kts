plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.triplet.play)
}

android {
    namespace = "com.dockie.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dockie.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 6
        versionName = "1.4.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // Release signing is injected via environment / gradle.properties outside VCS:
        // DOCKIE_KEYSTORE_PATH, DOCKIE_KEYSTORE_PASSWORD, DOCKIE_KEY_ALIAS, DOCKIE_KEY_PASSWORD
        // or dockie.keystore.path etc. in ~/.gradle/gradle.properties (never committed).
        // When no keystore is present the config stays empty and release builds unsigned.
        create("release") {
            val ksPath = (System.getenv("DOCKIE_KEYSTORE_PATH")
                ?: (project.findProperty("dockie.keystore.path") as String?)
                ?: (project.findProperty("DOCKIE_KEYSTORE_PATH") as String?))
            val ksPass = (System.getenv("DOCKIE_KEYSTORE_PASSWORD")
                ?: (project.findProperty("dockie.keystore.password") as String?)
                ?: (project.findProperty("DOCKIE_KEYSTORE_PASSWORD") as String?))
            val keyAlias = (System.getenv("DOCKIE_KEY_ALIAS")
                ?: (project.findProperty("dockie.key.alias") as String?)
                ?: (project.findProperty("DOCKIE_KEY_ALIAS") as String?)
                ?: "dockie")
            val keyPass = (System.getenv("DOCKIE_KEY_PASSWORD")
                ?: (project.findProperty("dockie.key.password") as String?)
                ?: (project.findProperty("DOCKIE_KEY_PASSWORD") as String?))
            if (!ksPath.isNullOrBlank() && !ksPass.isNullOrBlank() && !keyPass.isNullOrBlank()
                && file(ksPath).exists()
            ) {
                storeFile = file(ksPath)
                storePassword = ksPass
                this.keyAlias = keyAlias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Attach release signing only when a keystore was actually configured above.
            // (CI provides it via secrets; see .github/workflows/release.yml.)
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
        debug {
            applicationIdSuffix = ".debug"
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    val composeBom = libs.androidx.compose.bom
    implementation(platform(composeBom))
    androidTestImplementation(platform(composeBom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
}

// Google Play publishing (EYC shared infrastructure — see RELEASING.md).
// Auth: ADC when EYC_PLAY_ADC=true / -Peyc.play.adc=true (local EYC key via
// GOOGLE_APPLICATION_CREDENTIALS, OIDC in CI). Otherwise an explicit key file
// via -Peyc.play.keyFile / EYC_PLAY_KEYFILE, else legacy play-service-account.json.
// Tracks: CLI --track internal|alpha|production (default internal).
// Play signing is Google-managed for the Play-distributed version (EYC decision:
// no PEPK upload, no sideload-key migration; old sideload installs uninstall).
play {
    val eycAdc = (project.findProperty("eyc.play.adc") as String?) == "true" ||
        System.getenv("EYC_PLAY_ADC") == "true"
    if (eycAdc) {
        useApplicationDefaultCredentials.set(true)
    } else {
        val keyFile = (project.findProperty("eyc.play.keyFile") as String?)
            ?: System.getenv("EYC_PLAY_KEYFILE")
        serviceAccountCredentials.set(
            if (!keyFile.isNullOrBlank()) file(keyFile)
            else rootProject.file("play-service-account.json"),
        )
    }
    track.set("internal")
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
    defaultToAppBundles.set(true)
}
