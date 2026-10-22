import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
}

spotless {
    // Q003: legacy code is adopted incrementally; every changed Kotlin file must be clean.
    ratchetFrom("b6895a2a")
    kotlin {
        target("src/**/*.kt")
        targetExclude("src/test/java/com/pixelquest/app/zz/**")
        ktlint("1.3.1").editorConfigOverride(
            mapOf(
                "ktlint_standard_filename" to "disabled",
                "ktlint_standard_function-naming" to "disabled",
                "ktlint_standard_max-line-length" to "disabled",
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_property-naming" to "disabled",
            ),
        )
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.3.1")
    }
}

// Static checks (J176): detekt's defaults with config/detekt/detekt.yml; what was already there is
// in its baseline, so only new findings fail. Run: ./gradlew :app:detekt
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/baseline.xml")
    source.setFrom("src/main/java", "src/test/java", "src/sharedTest/java", "src/androidTest/java")
    parallel = true
}

val localProps = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProps.load(stream)
    }
}

// A PIXELQUEST_<NAME> environment variable overrides <NAME> from local.properties for one build, e.g.
// to point a debug build at the local Supabase test bench without editing the file (docs/LOCAL_SUPABASE.md).
fun buildSetting(name: String): String? = System.getenv("PIXELQUEST_$name")?.takeIf { it.isNotBlank() } ?: localProps.getProperty(name)

val supabaseUrlProp = buildSetting("SUPABASE_URL") ?: "https://placeholder-project.supabase.co"
val supabaseAnonKeyProp = buildSetting("SUPABASE_ANON_KEY") ?: "placeholder-anon-key"
// S006 (docs/security/ENVIRONMENTS.md): debug builds talk to the development project when one is set
// (SUPABASE_DEV_URL and SUPABASE_DEV_ANON_KEY), so trying things never touches production data.
// Release builds always use SUPABASE_URL and SUPABASE_ANON_KEY.
val supabaseDevUrlProp = buildSetting("SUPABASE_DEV_URL")
val supabaseDevAnonKeyProp = buildSetting("SUPABASE_DEV_ANON_KEY")
if (supabaseDevUrlProp == null || supabaseDevAnonKeyProp == null) {
    logger.info("PixelQuest: no SUPABASE_DEV_URL/SUPABASE_DEV_ANON_KEY set; debug builds use SUPABASE_URL (docs/security/ENVIRONMENTS.md).")
}
val googleWebClientIdProp = buildSetting("GOOGLE_WEB_CLIENT_ID") ?: ""
// Push for chess challenges (docs/chess-design.md): the Firebase project's public app settings.
// Blank (the default) leaves push off; no google-services.json is kept in the repo.
val fcmApiKeyProp = buildSetting("FCM_API_KEY") ?: ""
val fcmAppIdProp = buildSetting("FCM_APP_ID") ?: ""
val fcmProjectIdProp = buildSetting("FCM_PROJECT_ID") ?: ""
val fcmSenderIdProp = buildSetting("FCM_SENDER_ID") ?: ""
val geminiApiKeyProp = buildSetting("GEMINI_API_KEY") ?: "placeholder-gemini-key"
// Debug builds call Gemini directly with the local key unless GEMINI_VIA_PROXY=true (to try the proxy).
val geminiViaProxyProp = buildSetting("GEMINI_VIA_PROXY")?.trim()?.toBoolean() ?: false
// Email-code sign-in needs an email service on the Supabase project (custom SMTP): Supabase's own
// mailer only reaches the project's team. EMAIL_SIGN_IN=false hides it until one is set up.
val emailSignInProp = buildSetting("EMAIL_SIGN_IN")?.trim()?.toBoolean() ?: true

// Room writes each database version's layout to app/schemas (committed), so updates can be tested
// against the real layouts of earlier versions (DatabaseUpgradeTest).
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

android {
    namespace = "com.pixelquest.app"

    // Fakes shared by the unit tests and the instrumented tests (src/sharedTest/.../testing/Fakes.kt).
    sourceSets {
        getByName("test").java.srcDir("src/sharedTest/java")
        getByName("androidTest").java.srcDir("src/sharedTest/java")
    }
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.pixelquest.app"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 110
        versionName = "1.7.2"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrlProp\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKeyProp\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientIdProp\"")
        buildConfigField("String", "FCM_API_KEY", "\"$fcmApiKeyProp\"")
        buildConfigField("String", "FCM_APP_ID", "\"$fcmAppIdProp\"")
        buildConfigField("String", "FCM_PROJECT_ID", "\"$fcmProjectIdProp\"")
        buildConfigField("String", "FCM_SENDER_ID", "\"$fcmSenderIdProp\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKeyProp\"")
        buildConfigField("boolean", "GEMINI_VIA_PROXY", "$geminiViaProxyProp")
        buildConfigField("boolean", "EMAIL_SIGN_IN", "$emailSignInProp")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The app speaks English only (no values-xx folders): its libraries' own strings in other
        // languages would ship unused, and kept the release APK over its 8 MB budget (docs/quran-data.md).
        // A translation added later adds its language here.
        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        create("release") {
            // Passwords come from the environment (CI secrets) or the gitignored local.properties,
            // never from this public file. Without a keystore and both passwords, release builds
            // are signed with the debug key.
            fun secret(name: String): String? = (System.getenv(name) ?: localProps.getProperty(name))?.takeIf { it.isNotBlank() }
            val keystorePath = secret("KEYSTORE_FILE") ?: "pixelquest-release.jks"
            val storeFileObj = file(keystorePath)
            val storePasswordValue = secret("KEYSTORE_PASSWORD")
            val keyPasswordValue = secret("KEY_PASSWORD")
            if (storeFileObj.exists() && storeFileObj.length() > 0L && storePasswordValue != null && keyPasswordValue != null) {
                storeFile = storeFileObj
                storePassword = storePasswordValue
                keyAlias = secret("KEY_ALIAS") ?: "pixelquest"
                keyPassword = keyPasswordValue
            } else {
                initWith(signingConfigs.getByName("debug"))
            }
        }
    }

    buildTypes {
        debug {
            if (supabaseDevUrlProp != null && supabaseDevAnonKeyProp != null) {
                buildConfigField("String", "SUPABASE_URL", "\"$supabaseDevUrlProp\"")
                buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseDevAnonKeyProp\"")
            }
        }
        release {
            // Release APKs are public and the key would be readable in them, so they never contain
            // the Gemini key and always go through the gemini-proxy Edge Function, which holds it.
            buildConfigField("String", "GEMINI_API_KEY", "\"\"")
            buildConfigField("boolean", "GEMINI_VIA_PROXY", "true")
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        // The app uses java.time throughout, which Android only ships from API 26; desugaring
        // bundles it into the APK for API 24-25 (desugar_jdk_libs below).
        isCoreLibraryDesugaringEnabled = true
        val jvmVersion = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
    }
    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Q005: preserve known debt in a checked-in baseline; new warnings fail locally and in CI.
        baseline = file("lint-baseline.xml")
        abortOnError = true
        warningsAsErrors = true
        checkReleaseBuilds = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Q007: Kotlin warnings are build failures on production sources so new code cannot add warning debt.
// KAPT generated stubs and test tasks are excluded.
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.allWarningsAsErrors.set(!name.startsWith("kapt", ignoreCase = true) && !name.contains("Test", ignoreCase = true))
    compilerOptions.freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Installs the baseline profile (src/main/baseline-prof.txt) so ART compiles the UI ahead of time.
    implementation(libs.androidx.profileinstaller)

    // Section B Step 6: Compose BOM & Core Compose UI Dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // UI v2's icons (docs/ui-v2.md); R8 keeps only those the app uses.
    implementation(libs.androidx.material.icons.extended)

    // Section B Step 7: Navigation Compose Dependency
    implementation(libs.androidx.navigation.compose)

    // Section B Step 8: Room Dependency
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Section B Step 9: WorkManager and Hilt Dependencies
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.firebase.messaging)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    // Section B Step 10: Coil and Kotlin Coroutines Dependencies
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Day 13 Section B Step 6: Supabase & Network Dependencies
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    // Darood Party's live updates; Realtime needs a WebSocket, which the Android engine lacks.
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.okhttp)

    // Prayer times, calculated on the phone (Namaz Tracker)
    implementation(libs.adhan2)
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.serialization.json)
    // The Quran reader's settings, in a Preferences DataStore.
    implementation(libs.androidx.datastore.preferences)

    // Day 13 Section C Step 11: Credential Manager & Google Identity Services
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.turbine)
    // The local Supabase bench (LocalSupabase.kt): the JVM's HttpURLConnection, under the Android
    // engine, can't send PATCH; phones can. Tests only.
    testImplementation(libs.ktor.client.cio)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
