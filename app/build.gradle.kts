import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing material lives outside the project so a copy of the source tree never
// carries the key that signs future releases. FOLIO_KEYSTORE_PROPERTIES can point
// somewhere else; a project-local file is still honoured as a last resort.
val signingPropsFile: File? = sequenceOf(
    System.getenv("FOLIO_KEYSTORE_PROPERTIES")?.let(::File),
    File(System.getProperty("user.home"), ".folio-signing/keystore.properties"),
    rootProject.file("keystore.properties"),
).filterNotNull().firstOrNull { it.isFile }

val keystoreProps = Properties().apply {
    signingPropsFile?.inputStream()?.use { load(it) }
}

val releaseKeystore: File? = keystoreProps.getProperty("storeFile")?.let { path ->
    val configured = File(path)
    if (configured.isAbsolute) configured
    else File(signingPropsFile?.parentFile ?: rootProject.projectDir, path)
}
val hasReleaseKeystore = releaseKeystore?.isFile == true

// Signing is required for a release: an unsigned APK must never be produced by accident.
tasks.configureEach {
    if (name == "packageRelease" || name == "bundleRelease") {
        doFirst {
            check(hasReleaseKeystore) {
                "Release signing is not configured. Expected keystore.properties at " +
                    "${signingPropsFile?.path ?: "~/.folio-signing/keystore.properties"}."
            }
        }
    }
}

android {
    namespace = "com.folio.notes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.folio.notes"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn")
        }
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

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.09.01"))

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.16.1")
}
