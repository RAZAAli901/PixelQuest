package com.pixelquest.tools.quran

import java.io.File
import kotlin.system.exitProcess

/**
 * Reads the Quran's source files ([SourceFiles]), and writes the app's data from them
 * (docs/quran-data.md). Arguments: the source folder, then the output folder.
 */
fun main(args: Array<String>) {
    val source = File(args.getOrElse(0) { "data/quran-source" })
    val output = File(args.getOrElse(1) { "app/src/main/assets/quran" })
    try {
        val written = QuranBuild(source, output).run()
        written.forEach { println("Wrote ${it.path} (${it.length()} bytes)") }
    } catch (e: QuranBuildException) {
        System.err.println("Quran build failed: ${e.message}")
        exitProcess(1)
    }
}

/** Why the build stopped: a source file is missing, or isn't what it must be. Nothing is written. */
class QuranBuildException(message: String) : Exception(message)

/** The three files Tanzil publishes that the app is built from, in [folder]. */
class SourceFiles(val folder: File) {
    val arabic = File(folder, "quran-uthmani.xml")
    val translation = File(folder, "en.sahih.txt")
    val metadata = File(folder, "quran-data.xml")

    val all: List<File> get() = listOf(arabic, translation, metadata)

    /** Fails unless every file is there. */
    fun requireAll() {
        val missing = all.filterNot { it.isFile }
        if (missing.isNotEmpty()) throw QuranBuildException("missing: ${missing.joinToString { it.path }}")
    }
}

/** The build: read and check the sources, then write the app's data to [output] ([Output]). */
class QuranBuild(source: File, private val output: File) {
    private val files = SourceFiles(source)

    /** Builds the data, and returns the files it wrote. */
    fun run(): List<File> {
        files.requireAll()
        Checksums.check(files.all)
        // Each file is read whole before anything is written: the Arabic and the metadata as XML,
        // the translation as text, one ayah a line.
        val arabic = ArabicSource.read(files.arabic)
        val translation = TranslationSource.read(files.translation)
        val metadata = MetadataSource.read(files.metadata)
        Validation.check(arabic, translation, metadata)
        Bismillah.check(arabic)
        val joined = Join.of(arabic, translation)
        val version = Output.version(files)
        return Output.write(
            output,
            Output.quran(version, arabic, metadata, joined),
            Output.translation(version, translation, joined),
            joined
        )
    }
}
