package com.pixelquest.app.quran.data

/**
 * Arabic as search matches it, so the player can type plain letters, with or without marks, and
 * find the Uthmani text. The ayah's own text is never changed: this makes a separate key from it
 * (QuranAyahEntity.arabicPlain), and the same key from what's typed.
 *
 * The key drops the marks (harakat, the Quran's small signs and letters, tatweel), and drops alef
 * and hamza in every form, where the Uthmani spelling and everyday spelling differ most (the
 * Uthmani text writes many a long "a" as a small alef above, which everyday spelling writes in
 * full). A letter carrying a hamza becomes the plain letter; alef maksura becomes yeh, teh marbuta
 * heh; and the yeh and keheh of Persian keyboards become Arabic's yeh and kaf. Spaces stay, one at
 * a time.
 */
object ArabicSearch {

    fun key(text: String): String {
        val out = StringBuilder(text.length)
        for (c in text) {
            val code = c.code
            when {
                isMark(code) || code in DROPPED -> Unit
                code in PLAIN -> out.append(PLAIN.getValue(code))
                c.isWhitespace() -> if (out.isNotEmpty() && out.last() != ' ') out.append(' ')
                isLetter(code) -> out.append(c)
            }
        }
        return out.toString().trimEnd()
    }

    /** Whether [text] has Arabic letters in it, so it's searched in the Arabic. */
    fun hasArabic(text: String): Boolean = text.any { isLetter(it.code) }

    private fun isMark(code: Int) =
        code in 0x0610..0x061A || code in 0x064B..0x065F || code == 0x0640 || code == 0x0670 ||
            code in 0x06D6..0x06ED || code in 0x08D3..0x08FF

    private fun isLetter(code: Int) = code in 0x0620..0x064A || code in 0x066E..0x06D3 || code in 0x06FA..0x06FF

    /** Hamza on its own, and alef in every form: plain, with hamza above or below, with madda, wasla. */
    private val DROPPED = setOf(0x0621, 0x0622, 0x0623, 0x0625, 0x0627, 0x0671, 0x0672, 0x0673)

    /** Waw and yeh with hamza, alef maksura, teh marbuta, Persian yeh and keheh: as their plain letters. */
    private val PLAIN = mapOf(
        0x0624 to Char(0x0648),
        0x0626 to Char(0x064A),
        0x0649 to Char(0x064A),
        0x0629 to Char(0x0647),
        0x06CC to Char(0x064A),
        0x06A9 to Char(0x0643)
    )
}
