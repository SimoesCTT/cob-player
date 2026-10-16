plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cobplayer.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cobplayer.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
}

dependencies {
    val media3Version = "1.8.0"

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Media3 / ExoPlayer
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("io.github.anilbeesetti:nextlib-media3ext:1.8.0-0.9.0")
    implementation("androidx.media3:media3-cast:$media3Version")
    implementation("androidx.mediarouter:mediarouter:1.7.0")
    implementation("com.google.android.gms:play-services-cast-framework:21.5.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.hierynomus:smbj:0.13.0")
    implementation("commons-net:commons-net:3.10.0")
    implementation("androidx.media3:media3-cast:$media3Version")
    implementation("androidx.mediarouter:mediarouter:1.7.0")
    implementation("com.google.android.gms:play-services-cast-framework:21.5.0")

    // SMB2/SMB3 client for network shares

    // FTP / FTPS

    // JSch for SFTP
    implementation("com.github.mwiede:jsch:0.2.18")

    // FFmpeg extension — decodes AC3, DTS, ALAC, and more.
    // Requires native libs to be built via NDK. If your build fails
    // with "missing .so", comment this line out — the app still works
    // with device codecs.
    // implementation("androidx.media3:media3-exoplayer-ffmpeg:$media3Version")
}
