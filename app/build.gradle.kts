import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing is configured outside the repository: either through environment
// variables (CI / local release build) or an untracked keystore.properties file.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(envName: String, propertyName: String): String? =
    System.getenv(envName) ?: keystoreProperties.getProperty(propertyName)

val releaseStorePath = signingValue("GRAFFITI_KEYSTORE_FILE", "storeFile")

android {
    namespace = "com.graffitistudio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.graffitistudio"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        if (releaseStorePath != null) {
            create("release") {
                storeFile = file(releaseStorePath)
                storePassword = signingValue("GRAFFITI_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("GRAFFITI_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("GRAFFITI_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:storage"))
    implementation(project(":core:data"))
    implementation(project(":engine:rendering"))
    implementation(project(":engine:drawing"))
    implementation(project(":feature:home"))
    implementation(project(":feature:workspace"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
