package com.pixelquest.app.mascot

// GENERATED from design-reference/PQMascot.dc.html (DATA block). Same grid = same Pix as the design.
// 24×24 cell grid. Layers are drawn in order; later cells win.

data class MascotCell(val x: Int, val y: Int, val color: Int)
data class MascotLayer(val id: String, val anim: String?, val cells: List<MascotCell>)
data class MascotFrame(val layers: List<MascotLayer>, val motion: String)

enum class PixMood(val key: String) {
    Happy("happy"),
    Celebrate("celebrate"),
    LevelUp("levelup"),
    OnFire("onfire"),
    Worried("worried"),
    Sad("sad"),
    Lost("lost"),
    Sleepy("sleepy"),
    Praying("praying"),
    Focus("focus"),
    Challenge("challenge"),
    Wow("wow"),
    Wise("wise"),
}

internal class PixShape(val outline: Boolean, val cells: List<Pair<String, List<IntArray>>>, val anim: String? = null)
internal class PixMoodDef(val eyes: String, val mouth: String, val acc: List<String>, val motion: String, val blink: Boolean = false)

private fun pts(vararg v: Int): List<IntArray> = v.toList().chunked(2).map { intArrayOf(it[0], it[1]) }
private fun shape(outline: Boolean, vararg c: Pair<String, List<IntArray>>) = PixShape(outline, c.toList())
private fun acc(outline: Boolean, anim: String?, vararg c: Pair<String, List<IntArray>>) = PixShape(outline, c.toList(), anim)

internal object PixData {
    val colors = mapOf(
        "k" to 0xFF161412.toInt(), "w" to 0xFFFFFDF8.toInt(), "g" to 0xFFFFC21A.toInt(), "g2" to 0xFFCFC6B8.toInt(), "t" to 0xFFFFB3A8.toInt(), "c" to 0xFFFF9D92.toInt(), "s" to 0xFF8ECAE6.toInt(), "o" to 0xFFF0A35E.toInt(), "l" to 0xFFB39DDB.toInt(), "d" to 0xFF7A1D17.toInt(), "body" to 0xFFFF5A4E.toInt(),
    )
    val pixel = mapOf(
        "k" to 0xFF2B2622.toInt(),
        "w" to 0xFFFFFAF0.toInt(),
        "c" to 0xFFE8A33D.toInt(),
        "d" to 0xFF5A2A00.toInt(),
        "body" to 0xFFC25E00.toInt(),
    )
    val eyes = mapOf(
        "open" to shape(
            false,
            "k" to pts(0, 0, 1, 0, 0, 1, 1, 1, 0, 2, 1, 2),
            "w" to pts(0, 0),
        ),
        "happy" to shape(
            false,
            "k" to pts(-1, 2, 0, 1, 1, 1, 2, 2),
        ),
        "closed" to shape(
            false,
            "k" to pts(-1, 1, 0, 2, 1, 2, 2, 1),
        ),
        "sad" to shape(
            false,
            "k" to pts(0, 1, 1, 1, 0, 2, 1, 2, -1, 0, 0, -1),
            "w" to pts(0, 1),
        ),
        "determined" to shape(
            false,
            "k" to pts(0, 1, 1, 1, 0, 2, 1, 2, -1, -1, 0, 0, 1, 0),
            "w" to pts(0, 1),
        ),
        "squeeze" to shape(
            false,
            "k" to pts(-1, 0, 0, 1, 1, 1, -1, 2),
        ),
        "x" to shape(
            false,
            "k" to pts(-1, 0, 1, 0, 0, 1, -1, 2, 1, 2),
        ),
        "star" to shape(
            true,
            "g" to pts(0, 0, -1, 1, 0, 1, 1, 1, 0, 2),
        ),
        "wide" to shape(
            true,
            "w" to pts(0, 0, 1, 0, 0, 1, 1, 1, 0, 2, 1, 2),
            "k" to pts(1, 1, 1, 2),
        ),
    )
    val mouths = mapOf(
        "smile" to shape(
            false,
            "k" to pts(8, 15, 9, 16, 10, 16, 11, 16, 12, 16, 13, 15),
        ),
        "grin" to shape(
            false,
            "k" to pts(8, 15, 13, 15, 9, 16, 12, 16, 10, 17, 11, 17),
            "w" to pts(9, 15, 10, 15, 11, 15, 12, 15),
            "t" to pts(10, 16, 11, 16),
        ),
        "frown" to shape(
            false,
            "k" to pts(9, 16, 10, 16, 11, 16, 12, 16, 8, 17, 13, 17),
        ),
        "flat" to shape(
            false,
            "k" to pts(9, 16, 10, 16, 11, 16, 12, 16),
        ),
        "small" to shape(
            false,
            "k" to pts(9, 15, 10, 16, 11, 16, 12, 15),
        ),
        "wobble" to shape(
            false,
            "k" to pts(8, 16, 9, 15, 10, 16, 11, 15, 12, 16, 13, 15),
        ),
        "o" to shape(
            false,
            "k" to pts(10, 15, 11, 15, 9, 16, 12, 16, 9, 17, 12, 17, 10, 18, 11, 18),
            "d" to pts(10, 16, 11, 16, 10, 17, 11, 17),
        ),
    )
    val acc = mapOf(
        "cheeks" to acc(
            false,
            null,
            "c" to pts(5, 14, 16, 14),
        ),
        "tear" to acc(
            false,
            "drip",
            "s" to pts(6, 13, 6, 14),
        ),
        "tears" to acc(
            false,
            "drip",
            "s" to pts(5, 13, 5, 14, 5, 15, 4, 16, 16, 13, 16, 14, 16, 15, 17, 16),
        ),
        "sweat" to acc(
            true,
            "drip",
            "s" to pts(20, 7, 20, 8, 21, 8, 20, 9, 21, 9),
        ),
        "zzz" to acc(
            false,
            "float",
            "k" to pts(1, 1, 2, 1, 3, 1, 4, 1, 3, 2, 2, 3, 1, 4, 2, 4, 3, 4, 4, 4, 6, 0, 7, 0, 8, 0, 7, 1, 6, 2, 7, 2, 8, 2),
        ),
        "flame" to acc(
            true,
            "flicker",
            "o" to pts(11, 1, 10, 2, 11, 2, 9, 3, 10, 3, 11, 3, 12, 3, 9, 4, 10, 4, 11, 4, 12, 4, 9, 5, 10, 5, 11, 5, 12, 5),
            "g" to pts(11, 3, 10, 4, 11, 4, 10, 5, 11, 5),
        ),
        "crown" to acc(
            true,
            "shine",
            "g" to pts(7, 3, 10, 3, 11, 3, 14, 3, 7, 4, 8, 4, 10, 4, 11, 4, 13, 4, 14, 4, 7, 5, 8, 5, 9, 5, 12, 5, 13, 5, 14, 5),
            "s" to pts(10, 5, 11, 5),
        ),
        "confetti" to acc(
            false,
            "twinkle",
            "g" to pts(1, 3, 21, 11, 13, 1),
            "s" to pts(2, 9, 22, 16, 6, 2),
            "l" to pts(0, 15, 20, 21, 9, 1),
        ),
        "moon" to acc(
            true,
            "glow",
            "g" to pts(2, 1, 3, 1, 1, 2, 2, 2, 1, 3, 2, 3, 2, 4, 3, 4),
        ),
        "book" to acc(
            true,
            null,
            "w" to pts(7, 18, 8, 18, 9, 18, 12, 18, 13, 18, 14, 18, 7, 19, 8, 19, 9, 19, 12, 19, 13, 19, 14, 19),
            "k" to pts(10, 18, 11, 18, 10, 19, 11, 19),
            "l" to pts(7, 20, 8, 20, 9, 20, 10, 20, 11, 20, 12, 20, 13, 20, 14, 20),
        ),
        "pawn" to acc(
            true,
            null,
            "w" to pts(20, 12, 19, 13, 20, 13, 21, 13, 20, 14, 20, 15, 19, 16, 20, 16, 21, 16, 18, 17, 19, 17, 20, 17, 21, 17, 22, 17),
        ),
        "glasses" to acc(
            false,
            null,
            "k" to pts(5, 9, 6, 9, 7, 9, 8, 9, 13, 9, 14, 9, 15, 9, 16, 9, 5, 13, 6, 13, 7, 13, 8, 13, 13, 13, 14, 13, 15, 13, 16, 13, 5, 10, 5, 11, 5, 12, 8, 10, 8, 11, 8, 12, 13, 10, 13, 11, 13, 12, 16, 10, 16, 11, 16, 12, 9, 10, 10, 10, 11, 10, 12, 10),
        ),
        "sparkle" to acc(
            false,
            "twinkle",
            "g" to pts(20, 9, 19, 10, 20, 10, 21, 10, 20, 11),
        ),
    )
    val moods = mapOf(
        "happy" to PixMoodDef("open", "smile", listOf("cheeks"), "bob", blink = true),
        "celebrate" to PixMoodDef("star", "grin", listOf("cheeks", "confetti"), "jump"),
        "levelup" to PixMoodDef("happy", "grin", listOf("cheeks", "crown"), "bounce"),
        "onfire" to PixMoodDef("determined", "grin", listOf("flame"), "bob"),
        "worried" to PixMoodDef("sad", "wobble", listOf("sweat"), "shake"),
        "sad" to PixMoodDef("sad", "frown", listOf("tear"), "droop"),
        "lost" to PixMoodDef("squeeze", "o", listOf("tears"), "sob"),
        "sleepy" to PixMoodDef("closed", "flat", listOf("zzz"), "breathe"),
        "praying" to PixMoodDef("closed", "small", listOf("cheeks", "moon"), "breathe"),
        "focus" to PixMoodDef("determined", "flat", listOf("book"), "lean", blink = true),
        "challenge" to PixMoodDef("determined", "smile", listOf("pawn"), "wiggle"),
        "wow" to PixMoodDef("wide", "o", listOf("confetti"), "jump"),
        "wise" to PixMoodDef("open", "smile", listOf("glasses", "sparkle"), "bob", blink = true),
    )
}

/** Port of Component.layers() in PQMascot.dc.html. Cache the result per (mood, pixel). */
object PixFrames {
    private val cache = HashMap<String, MascotFrame>()

    fun frame(mood: PixMood, pixel: Boolean): MascotFrame = cache.getOrPut(mood.key + pixel) { build(mood.key, pixel) }

    private fun build(mood: String, px: Boolean): MascotFrame {
        val m = PixData.moods[mood] ?: PixData.moods.getValue("happy")
        val c = HashMap(PixData.colors).apply { if (px) putAll(PixData.pixel) }
        val ink = c.getValue("k")
        fun dedupe(cells: List<MascotCell>): List<MascotCell> {
            val mp = LinkedHashMap<Long, MascotCell>()
            cells.forEach {
                mp.remove(key(it.x, it.y))
                mp[key(it.x, it.y)] = it
            }
            return mp.values.toList()
        }
        fun outlineOf(cells: List<MascotCell>): List<MascotCell> {
            val own = cells.map { key(it.x, it.y) }.toHashSet()
            val seen = HashSet<Long>()
            val res = ArrayList<MascotCell>()
            for (cell in cells) {
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        val k = key(cell.x + dx, cell.y + dy)
                        if (k !in own && seen.add(k)) res += MascotCell(cell.x + dx, cell.y + dy, ink)
                    }
                }
            }
            return res
        }
        fun fromShape(s: PixShape, ox: Int = 0, oy: Int = 0, mirror: Boolean = false) =
            s.cells.flatMap { (col, list) -> list.map { p -> MascotCell(ox + if (mirror) 1 - p[0] else p[0], oy + p[1], c.getValue(col)) } }

        val out = ArrayList<MascotLayer>()
        val body = ArrayList<IntArray>()
        for (y in 6..20) {
            for (x in 3..18) {
                if (!px && (x == 3 || x == 18) && (y == 6 || y == 20)) continue
                body += intArrayOf(x, y)
            }
        }
        val sx = if (px) 0 else 1
        out += MascotLayer("shadow", null, body.map { MascotCell(it[0] + sx, it[1] + 1, ink) })

        out += MascotLayer(
            "body",
            null,
            body.map { (x, y) ->
                val edge = x == 3 || x == 18 || y == 6 || y == 20
                val corner = !px && (x == 4 || x == 17) && (y == 7 || y == 19)
                MascotCell(x, y, if (edge || corner) ink else c.getValue("body"))
            },
        )
        val e = PixData.eyes.getValue(m.eyes)
        var eyes = dedupe(fromShape(e, 6, 10) + fromShape(e, 14, 10, mirror = true))
        if (e.outline) eyes = outlineOf(eyes) + eyes
        out += MascotLayer("eyes", if (m.blink) "blink" else null, eyes)
        out += MascotLayer("mouth", null, dedupe(fromShape(PixData.mouths.getValue(m.mouth))))
        for (a in m.acc) {
            val s = PixData.acc.getValue(a)
            var cells = dedupe(fromShape(s))
            if (s.outline) cells = outlineOf(cells) + cells
            out += MascotLayer(a, s.anim, cells)
        }
        return MascotFrame(out, m.motion)
    }

    private fun key(x: Int, y: Int) = (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)
}
