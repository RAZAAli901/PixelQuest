package com.pixelquest.tools.quran

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Reading Tanzil's text files: UTF-8, strictly, so a damaged byte fails instead of becoming a stand-in. */
object Text {
    fun lines(file: File): List<String> {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = try {
            decoder.decode(ByteBuffer.wrap(file.readBytes())).toString()
        } catch (e: CharacterCodingException) {
            throw QuranBuildException("${file.name} isn't valid UTF-8: ${e.message}")
        }
        return text.split('\n')
    }
}
