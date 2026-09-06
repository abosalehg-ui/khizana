plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * The version a release carries comes from the tag that triggered it.
 *
 * A hardcoded `versionCode = 1` meant every signed release built from every
 * `v*` tag claimed to be the same version, and Android refuses to install a
 * build whose versionCode does not exceed the installed one — so the second
 * release could never be an upgrade of the first. `v1.2.3` becomes name
 * "1.2.3" and code 10203; anything else (a local build, a branch build) stays
 * at the development 0.1.0 / 1.
 */
val releaseTag: String? = (System.getenv("GITHUB_REF_NAME") ?: "")
    .takeIf { Regex("^v\\d+\\.\\d+\\.\\d+$").matches(it) }

fun khizanaVersionName(): String = releaseTag?.removePrefix("v") ?: "0.1.0"

fun khizanaVersionCode(): Int {
    val tag = releaseTag ?: return 1
    val (major, minor, patch) = tag.removePrefix("v").split(".").map(String::toInt)
    // Room for 100 minors and 100 patches per major, monotonic by construction.
    return major * 10000 + minor * 100 + patch
}

android {
    namespace = "com.abosalehg.khizana"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abosalehg.khizana"
        minSdk = 29
        targetSdk = 35
        versionCode = khizanaVersionCode()
        versionName = khizanaVersionName()
    }

    signingConfigs {
        create("release") {
            // Populated from environment (GitHub Secrets in CI). Local release
            // builds stay unsigned unless these variables are exported.
            val keystorePath = System.getenv("KHIZANA_KEYSTORE_FILE")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KHIZANA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KHIZANA_KEY_ALIAS")
                keyPassword = System.getenv("KHIZANA_KEY_PASSWORD")
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
            if (System.getenv("KHIZANA_KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    // MigrationTestHelper can only look the schema JSON up as an asset, and a
    // Robolectric unit test sees the *merged* assets of the variant under test
    // — the test source set's own assets never get there. Registering the
    // exported schema directory on the debug variant is what lets the migration
    // test run on the JVM in CI instead of needing an emulator; release builds
    // never see these files, and there is only ever one copy of them on disk.
    sourceSets {
        getByName("debug") {
            assets.srcDirs("$projectDir/schemas")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Android framework stubs (e.g. android.util.Log) return defaults
            // instead of throwing, so production logging never breaks a test.
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Room schema history: required to write and verify migrations. The generated
// JSON under app/schemas must be committed alongside every version bump.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.compose)
    // PDF rendering uses android.graphics.pdf.PdfRenderer from the platform —
    // no third-party native PDF parser, so it is patched by the OS.

    testImplementation(libs.junit)
    // Real org.json for JVM unit tests (the android.jar stubs throw).
    testImplementation(libs.org.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
