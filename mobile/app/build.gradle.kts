import java.io.File
import java.util.Properties

private fun escapeJavaString(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Windows: deep paths under OneDrive + long Kotlin-generated names can exceed MAX_PATH and break
// dex/clean tasks. Build outside the synced tree (still safe local-only; not committed).
val shortAppBuildDir =
    File(
        System.getenv("LOCALAPPDATA")
            ?: File(System.getProperty("user.home"), "AppData${File.separator}Local").path,
        "MoodMapGradle${File.separator}app",
    )
layout.buildDirectory.set(shortAppBuildDir)

android {
    val localProperties = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) load(f.inputStream())
    }

    namespace = "hacklanta.moodmap"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "hacklanta.moodmap"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val geminiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"${escapeJavaString(geminiKey)}\"")
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${escapeJavaString(googleWebClientId)}\""
        )
        buildConfigField("String", "MAPS_API_KEY", "\"${escapeJavaString(mapsApiKey)}\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    val releaseStorePath = localProperties.getProperty("RELEASE_STORE_FILE")
        ?: System.getenv("RELEASE_STORE_FILE")

    buildTypes {
        debug {
            val url =
                (localProperties.getProperty("MOOD_MAP_API_BASE_URL") ?: "http://10.0.2.2:8000").trimEnd('/') + "/"
            buildConfigField("String", "MOOD_MAP_API_BASE_URL", "\"${escapeJavaString(url)}\"")
            buildConfigField("boolean", "IS_DEBUG_BUILD", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val urlFromEnv = System.getenv("MOOD_MAP_API_BASE_URL").orEmpty()
            val urlFromFile = localProperties.getProperty("MOOD_MAP_API_BASE_URL").orEmpty()
            val url = urlFromFile.ifBlank { urlFromEnv }.trim()
            val normalized =
                if (url.isNotBlank() && url.lowercase().startsWith("https://")) url.trimEnd('/') + "/"
                else ""
            buildConfigField(
                "String",
                "MOOD_MAP_API_BASE_URL",
                "\"${escapeJavaString(normalized.ifBlank { "https://unset.invalid/" })}\""
            )
            buildConfigField("boolean", "IS_DEBUG_BUILD", "false")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    if (!releaseStorePath.isNullOrBlank()) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseStorePath!!)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                    ?: System.getenv("RELEASE_STORE_PASSWORD").orEmpty()
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                    ?: System.getenv("RELEASE_KEY_ALIAS").orEmpty()
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
                    ?: System.getenv("RELEASE_KEY_PASSWORD").orEmpty()
            }
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.register("validateReleaseApiConfiguration") {
    val propsFile = rootProject.file("local.properties")
    doLast {
        val p = Properties()
        if (propsFile.exists()) {
            propsFile.inputStream().use { p.load(it) }
        }
        val url = p.getProperty("MOOD_MAP_API_BASE_URL").orEmpty()
            .ifBlank { System.getenv("MOOD_MAP_API_BASE_URL").orEmpty() }
            .trim()
        require(url.isNotBlank() && url.lowercase().startsWith("https://")) {
            "Release builds require MOOD_MAP_API_BASE_URL with an https:// URL in local.properties or the environment."
        }
    }
}

afterEvaluate {
    listOf(
        "generateReleaseBuildConfig",
        "compileReleaseKotlin",
        "assembleRelease",
        "bundleRelease"
    ).forEach { taskName ->
        tasks.findByName(taskName)?.dependsOn("validateReleaseApiConfiguration")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.security:security-crypto:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    val nav_version = "2.7.7"
    implementation("androidx.navigation:navigation-compose:$nav_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:5.0.3")
    implementation("com.google.code.gson:gson:2.11.0")
}
