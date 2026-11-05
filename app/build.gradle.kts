import java.util.Properties

plugins {
    alias(libs.plugins.google.services)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val keystoreProps = Properties().apply {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) load(propsFile.inputStream().reader(Charsets.UTF_8))
}

val firebaseConfigured = !providers.fileContents(layout.projectDirectory.file("google-services.json"))
    .asText.get().contains("moneyhelper-placeholder")

android {
    namespace = "ru.denpom.finance.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ru.denpom.finance.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        resValue("bool", "firebase_messaging_enabled", firebaseConfigured.toString())
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    val hasReleaseSigning = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        .all { !keystoreProps.getProperty(it).isNullOrBlank() }

    signingConfigs {
        if (hasReleaseSigning) create("release") {
            storeFile = file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps["storePassword"] as String
            keyAlias = keystoreProps["keyAlias"] as String
            keyPassword = keystoreProps["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        resValues = true
    }
    androidResources {
        noCompress += "traineddata"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.appsflyer)
    implementation(libs.installreferrer)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    implementation(libs.onesignal.notifications)
    implementation(libs.onesignal.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.tesseract4android)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
