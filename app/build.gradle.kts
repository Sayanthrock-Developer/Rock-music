plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

fun String.asBuildConfigValue(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun org.gradle.api.provider.ProviderFactory.stringBuildConfig(name: String): String =
    gradleProperty(name).orNull.orEmpty().asBuildConfigValue()

android {
    namespace = "com.rockmusic.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rockmusic.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "ROCK_SPOTIFY_CLIENT_ID", providers.stringBuildConfig("ROCK_SPOTIFY_CLIENT_ID"))
        buildConfigField("String", "ROCK_SPOTIFY_REDIRECT_URI", providers.stringBuildConfig("ROCK_SPOTIFY_REDIRECT_URI"))
        buildConfigField("String", "ROCK_ECHO_FIND_BASE_URL", providers.stringBuildConfig("ROCK_ECHO_FIND_BASE_URL"))
        buildConfigField("String", "ROCK_ECHO_FIND_API_KEY", providers.stringBuildConfig("ROCK_ECHO_FIND_API_KEY"))
        buildConfigField("String", "ROCK_LISTEN_TOGETHER_REST_URL", providers.stringBuildConfig("ROCK_LISTEN_TOGETHER_REST_URL"))
        buildConfigField("String", "ROCK_LISTEN_TOGETHER_WS_URL", providers.stringBuildConfig("ROCK_LISTEN_TOGETHER_WS_URL"))
        buildConfigField("String", "ROCK_DISCORD_CLIENT_ID", providers.stringBuildConfig("ROCK_DISCORD_CLIENT_ID"))
        buildConfigField("String", "ROCK_DISCORD_REDIRECT_URI", providers.stringBuildConfig("ROCK_DISCORD_REDIRECT_URI"))
        buildConfigField("String", "ROCK_DISCORD_ACTIVITY_BACKEND_URL", providers.stringBuildConfig("ROCK_DISCORD_ACTIVITY_BACKEND_URL"))
        buildConfigField("String", "ROCK_CATALOGUE_BASE_URL", providers.stringBuildConfig("ROCK_CATALOGUE_BASE_URL"))
        buildConfigField("String", "ROCK_CATALOGUE_API_KEY", providers.stringBuildConfig("ROCK_CATALOGUE_API_KEY"))
        buildConfigField("String", "ROCK_LYRICS_BASE_URL", providers.stringBuildConfig("ROCK_LYRICS_BASE_URL"))
        buildConfigField("String", "ROCK_LYRICS_API_KEY", providers.stringBuildConfig("ROCK_LYRICS_API_KEY"))
        buildConfigField("String", "ROCK_PODCAST_SEARCH_BASE_URL", providers.stringBuildConfig("ROCK_PODCAST_SEARCH_BASE_URL"))
        buildConfigField("String", "ROCK_PODCAST_SEARCH_API_KEY", providers.stringBuildConfig("ROCK_PODCAST_SEARCH_API_KEY"))
        buildConfigField("String", "ROCK_DOWNLOADS_BASE_URL", providers.stringBuildConfig("ROCK_DOWNLOADS_BASE_URL"))
        buildConfigField("String", "ROCK_DOWNLOADS_API_KEY", providers.stringBuildConfig("ROCK_DOWNLOADS_API_KEY"))
        buildConfigField("String", "ROCK_CLOUD_CLIENT_ID", providers.stringBuildConfig("ROCK_CLOUD_CLIENT_ID"))
        buildConfigField("String", "ROCK_CLOUD_REDIRECT_URI", providers.stringBuildConfig("ROCK_CLOUD_REDIRECT_URI"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
        )
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp("androidx.room:room-compiler:2.7.2")

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    implementation(libs.coil.compose)
    implementation(libs.retrofit.core)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
