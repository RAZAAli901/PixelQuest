package com.pixelquest.app.study.chat.domain

/**
 * Why a picked file can't be shared, said on the phone before anything's sent
 * (docs/study-chat.md, Files). The server checks again, by the file's own bytes; these only save
 * the player a wait for an answer that's sure to be no.
 */
enum class UploadProblem {
    /** Not one of the allowed types. */
    BAD_TYPE,

    /** A document with macros (docm, xlsm, pptm): never shared. */
    MACROS,

    /** An archive (zip, rar, 7z...): never shared. */
    ARCHIVE,

    /** Nothing in it. */
    EMPTY,

    /** Over 20 MB. */
    TOO_LARGE,

    /** Nothing's left of its name once cleaned. */
    BAD_NAME
}

/** A file ready to ask the server for: its cleaned name, and the type it's sent as. */
data class UploadCandidate(val name: String, val mime: String)

object UploadChecks {
    private val MACRO_EXTENSIONS = setOf("docm", "dotm", "xlsm", "xltm", "xlam", "pptm", "potm", "ppam", "ppsm", "sldm")
    private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "jar", "apk", "aab", "cab", "iso")

    /** What [mime] and the name's [extension] say, together: the allowed type, if they agree on one. */
    private fun typeFor(mime: String, extension: String): String? {
        val byExtension = StudyChatConfig.ALLOWED_TYPES.entries.firstOrNull { extension in it.value }?.key ?: return null
        // A picker that doesn't know (or says only "some bytes") goes by the name; otherwise they must agree.
        return if (mime.isBlank() || mime == "application/octet-stream" || mime == byExtension) byExtension else null
    }

    fun extensionOf(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot > 0 && dot < name.length - 1) name.substring(dot + 1).lowercase() else ""
    }

    /**
     * The name the server would keep: its last part only, without path, reserved or control
     * characters, spaces tidied, and cut to 120 characters keeping its extension. Null if nothing's left.
     */
    fun cleanName(raw: String): String? {
        var name = raw.split('/', '\\').last()
            .filterNot { it.isISOControl() || it in "\u200B\u200C\u200D\u200E\u200F\u202A\u202B\u202C\u202D\u202E\u2066\u2067\u2068\u2069\uFEFF" }
            .filterNot { it in ":*?\"<>|" }
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimStart('.')
            .trim()
        if (name.length > StudyChatConfig.FILE_NAME_MAX_CHARS) {
            val ext = extensionOf(name)
            val keep = if (ext.isNotEmpty() && ext.length < 10) ".$ext" else ""
            name = name.take(StudyChatConfig.FILE_NAME_MAX_CHARS - keep.length).trimEnd() + keep
        }
        return name.takeUnless { it.isEmpty() || it == "." || it == ".." }
    }

    /** The first reason [name], [mime] and [sizeBytes] can't be shared, or what to ask the server for. */
    fun check(name: String, mime: String, sizeBytes: Long): Result<UploadCandidate> {
        val clean = cleanName(name) ?: return failure(UploadProblem.BAD_NAME)
        val extension = extensionOf(clean)
        if (extension in MACRO_EXTENSIONS || mime.contains("macroEnabled", ignoreCase = true)) return failure(UploadProblem.MACROS)
        if (extension in ARCHIVE_EXTENSIONS || mime in setOf("application/zip", "application/x-zip-compressed", "application/x-rar-compressed", "application/x-7z-compressed")) {
            return failure(UploadProblem.ARCHIVE)
        }
        val type = typeFor(mime, extension) ?: return failure(UploadProblem.BAD_TYPE)
        if (sizeBytes == 0L) return failure(UploadProblem.EMPTY)
        if (sizeBytes > StudyChatConfig.FILE_MAX_BYTES) return failure(UploadProblem.TOO_LARGE)
        return Result.success(UploadCandidate(clean, type))
    }

    private fun failure(problem: UploadProblem) = Result.failure<UploadCandidate>(UploadRefused(problem))
}

/** A check's refusal, carried in a [Result]. */
class UploadRefused(val problem: UploadProblem) : Exception(problem.name)
