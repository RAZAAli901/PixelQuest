package com.pixelquest.tools.quran

import java.io.File
import java.security.MessageDigest

/**
 * The SHA-256 of each source file as Tanzil published it (licenses/README.md). The build checks
 * them first, so a single changed byte in the text, the translation or the metadata fails it.
 * Updating a file to a new Tanzil release means updating its checksum here too (docs/quran-data.md).
 */
object Checksums {
    val recorded: Map<String, String> = mapOf(
        "quran-uthmani.xml" to "8c5aeae20363a98f6963720d29fce040ca8b56a8e75f8b564c257fce7f6d0417",
        "en.sahih.txt" to "a1778a1a56695d9b59ae910809ec46d9f4a55f05961de51cd56e6ebcf9040883",
        "quran-data.xml" to "8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a"
    )

    fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    /** Fails, naming the file, unless each of [files] has its recorded checksum. */
    fun check(files: List<File>) {
        files.forEach { file ->
            val expected = recorded[file.name] ?: throw QuranBuildException("no checksum is recorded for ${file.name}")
            val actual = sha256(file)
            if (actual != expected) {
                throw QuranBuildException("${file.name} isn't Tanzil's file as published: its SHA-256 is $actual, not $expected")
            }
        }
    }
}
