// The Quran's data builder (docs/quran-data.md): reads Tanzil's files in data/quran-source, checks
// them, and writes the app's data files to app/src/main/assets/quran. Run it with
// ./gradlew :tools:quran-build:run. It's a plain JVM program, not part of the app.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("com.pixelquest.tools.quran.QuranBuildKt")
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.named<JavaExec>("run") {
    // From the repository's root: the source files in, the app's data files out.
    workingDir = rootProject.projectDir
    args("data/quran-source", "app/src/main/assets/quran")
}

tasks.test {
    // The tests read the real source files, relative to the repository's root.
    workingDir = rootProject.projectDir
}
