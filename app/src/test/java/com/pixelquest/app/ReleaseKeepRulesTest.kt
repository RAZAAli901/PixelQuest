package com.pixelquest.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The release build's R8 rules (app/proguard-rules.pro) keep every class read from or sent to
 * Supabase whole: each serialized class in a remote package must sit under a package kept with
 * `-keep class <package>.** { *; }`. A new DTO in a package nobody kept would build fine and fail
 * only in the signed release. Other serialized classes (the AI coach's) rely on kotlinx.serialization's
 * own rules, which keep their generated serializers.
 */
class ReleaseKeepRulesTest {

    private val appDir = listOf("app", ".").map(::File).first { File(it, "proguard-rules.pro").exists() }
    private val rules = File(appDir, "proguard-rules.pro").readText()
    private val sources = File(appDir, "src/main/java")

    /** The packages kept whole: `-keep class a.b.c.** { *; }` keeps a.b.c and everything under it. */
    private val keptPackages = Regex("""^-keep class ([\w.]+)\.\*\* \{ \*; \}""", RegexOption.MULTILINE)
        .findAll(rules).map { it.groupValues[1] }.toList()

    private fun packageOf(file: File) = Regex("""^package ([\w.]+)""", RegexOption.MULTILINE).find(file.readText())!!.groupValues[1]

    @Test
    fun everySupabaseDto_isInAPackageTheReleaseKeepsWhole() {
        val remote = sources.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "@Serializable" in it.readText() }
            .map { it to packageOf(it) }
            .filter { (_, pkg) -> ".remote" in pkg }
            .toList()
        assertTrue("found the remote DTOs", remote.any { (_, pkg) -> pkg == "com.pixelquest.app.darood.data.remote" })
        for ((file, pkg) in remote) {
            assertTrue(
                "${file.name} (in $pkg) isn't in a package app/proguard-rules.pro keeps whole: $keptPackages",
                keptPackages.any { kept -> pkg == kept || pkg.startsWith("$kept.") }
            )
        }
    }

    @Test
    fun serialNames_andAnnotations_surviveR8() {
        assertTrue("-keepattributes *Annotation*" in rules)
        assertTrue("@kotlinx.serialization.SerialName <fields>;" in rules)
    }
}
