package com.pixelquest.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.R

/*
 * PixelQuest design tokens (UI v2, docs/ui-v2.md): two skins, one set of screens.
 *
 *   PqSkinState.skin = PqSkin.Pixel   // switch at runtime (e.g. from Settings > "Comic / Pixel")
 *
 * Every token below is a getter backed by snapshot state, so changing the skin recomposes
 * and redraws the whole app. Persist the choice (DataStore) and set it before setContent{}.
 *
 * Rules (both skins): ink border on everything; the hard "step" shadow only on things you
 * can tap; Red/primary ONLY for the single primary action per screen; colour fills = category/state.
 */

enum class PqSkin { Comic, Pixel }

object PqSkinState {
    var skin: PqSkin by mutableStateOf(PqSkin.Comic)
    val spec: PqSkinSpec get() = if (skin == PqSkin.Pixel) PixelSpec else ComicSpec
}

@Immutable
data class PqPalette(
    val ink: Color, val ink2: Color, val muted: Color,
    val paper: Color, val ground: Color, val sunken: Color, val divider: Color,
    val primary: Color, val onPrimary: Color, val primaryText: Color, val flame: Color,
    val sky: Color, val gold: Color, val amber: Color, val lilac: Color, val rose: Color,
    val progress: Color, val title: Color, val trophy: Color, val onTrophy: Color,
    val statusPerfect: Color, val statusPartial: Color, val statusMissed: Color,
    val avatarBg: Color,
)

@Immutable
data class PqSkinSpec(
    val palette: PqPalette,
    val radiusScale: Float,       // 1 = comic rounded, 0 = pixel square
    val border: Dp,
    val borderThin: Dp,
    val shadowX: Float,           // 1 = diagonal comic shadow, 0 = straight-down pixel step
    val shadowOnlyTappable: Boolean,
    val display: FontFamily,      // titles, numbers
    val label: FontFamily,        // buttons, small caps labels
    val body: FontFamily,
    val displayScale: Float,      // Press Start 2P is ~2x wider than Bangers
    val displayLine: Float,
    val labelScale: Float,
    val bodyScale: Float,
    val labelWeight: FontWeight,
)

// res/font: bangers_regular, rubik_medium, rubik_semibold, rubik_bold, rubik_extrabold,
//           press_start_2p_regular, pixelify_sans_medium, pixelify_sans_semibold, pixelify_sans_bold, amiri_regular
val Bangers = FontFamily(Font(R.font.bangers_regular))
val Rubik = FontFamily(
    Font(R.font.rubik_medium, FontWeight.Medium),
    Font(R.font.rubik_semibold, FontWeight.SemiBold),
    Font(R.font.rubik_bold, FontWeight.Bold),
    Font(R.font.rubik_extrabold, FontWeight.ExtraBold),
)
val PressStart = FontFamily(Font(R.font.press_start_2p))
val Pixelify = FontFamily(
    Font(R.font.pixelify_sans_medium, FontWeight.Medium),
    Font(R.font.pixelify_sans_semibold, FontWeight.SemiBold),
    Font(R.font.pixelify_sans_bold, FontWeight.Bold),
    Font(R.font.pixelify_sans_bold, FontWeight.ExtraBold),
)
val Amiri = FontFamily(Font(R.font.amiri_quran))

val ComicSpec = PqSkinSpec(
    palette = PqPalette(
        ink = Color(0xFF161412), ink2 = Color(0xFF4A4540), muted = Color(0xFF6B645C),
        paper = Color(0xFFFFFDF8), ground = Color(0xFFF6F3EC), sunken = Color(0xFFEFE9DD), divider = Color(0xFFEBE4D7),
        primary = Color(0xFFFF5A4E), onPrimary = Color(0xFF161412), primaryText = Color(0xFFC23A2F), flame = Color(0xFFD63A2F),
        sky = Color(0xFF8ECAE6), gold = Color(0xFFFFC21A), amber = Color(0xFFF0A35E), lilac = Color(0xFFB39DDB), rose = Color(0xFFFF5A4E),
        progress = Color(0xFFFF5A4E), title = Color(0xFF161412), trophy = Color(0xFFFFC21A), onTrophy = Color(0xFF161412),
        statusPerfect = Color(0xFF8ECAE6), statusPartial = Color(0xFFF0A35E), statusMissed = Color(0xFFFF5A4E),
        avatarBg = Color(0xFF2A2840),
    ),
    radiusScale = 1f, border = 2.5.dp, borderThin = 2.dp, shadowX = 1f, shadowOnlyTappable = false,
    display = Bangers, label = Rubik, body = Rubik,
    displayScale = 1f, displayLine = 1f, labelScale = 1f, bodyScale = 1f, labelWeight = FontWeight.ExtraBold,
)

val PixelSpec = PqSkinSpec(
    palette = PqPalette(
        ink = Color(0xFF2B2622), ink2 = Color(0xFF4A4540), muted = Color(0xFF6E655A),
        paper = Color(0xFFFFFAF0), ground = Color(0xFFF5EFE2), sunken = Color(0xFFECE4D2), divider = Color(0xFFECE4D2),
        primary = Color(0xFFC25E00), onPrimary = Color(0xFFFFFAF0), primaryText = Color(0xFFA34E00), flame = Color(0xFFC25E00),
        sky = Color(0xFFDBEEFA), gold = Color(0xFFFDE7A0), amber = Color(0xFFFBD5AE), lilac = Color(0xFFE4DAF6), rose = Color(0xFFF7C6BD),
        progress = Color(0xFF2F9E4A), title = Color(0xFFC25E00), trophy = Color(0xFFC25E00), onTrophy = Color(0xFFFFFAF0),
        statusPerfect = Color(0xFF1D6FA5), statusPartial = Color(0xFFE8A33D), statusMissed = Color(0xFFD1342A),
        avatarBg = Color(0xFF2A2840),
    ),
    radiusScale = 0f, border = 2.dp, borderThin = 2.dp, shadowX = 0f, shadowOnlyTappable = true,
    display = PressStart, label = PressStart, body = Pixelify,
    displayScale = 0.5f, displayLine = 1.35f, labelScale = 0.7f, bodyScale = 1.12f, labelWeight = FontWeight.Normal,
)

private val s get() = PqSkinState.spec
private val p get() = PqSkinState.spec.palette

object PqColors {
    val Ink get() = p.ink
    val Ink2 get() = p.ink2
    val Muted get() = p.muted
    val Paper get() = p.paper             // card surface
    val Ground get() = p.ground           // screen background
    val Sunken get() = p.sunken           // done rows, empty tracks
    val Divider get() = p.divider
    val Red get() = p.primary             // primary action (comic red / pixel orange)
    val OnRed get() = p.onPrimary
    val RedText get() = p.primaryText     // primary-coloured text on light (passes contrast)
    val Flame get() = p.flame
    val Sky get() = p.sky                 // prayer / info
    val Gold get() = p.gold               // level, celebration, active nav
    val Amber get() = p.amber             // fitness, AI
    val Lilac get() = p.lilac             // learning, Quran
    val Rose get() = p.rose               // health, Maghrib
    val Progress get() = p.progress
    val Title get() = p.title
    val Trophy get() = p.trophy
    val OnTrophy get() = p.onTrophy
    val StatusPerfect get() = p.statusPerfect
    val StatusPartial get() = p.statusPartial
    val StatusMissed get() = p.statusMissed
    val AvatarBg get() = p.avatarBg
    /** The coach's Live tag; also "done" and "saved" on the screens from before UI v2. */
    val Live get() = Color(0xFF1F7A3A)
    /** Error text and outlines: red ink that passes contrast on paper in both skins. */
    val Error get() = if (isPixelSkin) p.statusMissed else p.primaryText
}

object PqDimens {
    val Border get() = s.border
    val BorderThin get() = s.borderThin
    val Shadow = 3.dp
    val ShadowLg = 4.dp
    val Radius get() = 14.dp * s.radiusScale
    val RadiusLg get() = 16.dp * s.radiusScale
    val RadiusSm get() = 10.dp * s.radiusScale
    val ScreenPad = 16.dp
    val Gap = 12.dp
    /** The smallest tap target: 48dp, Android's and the app's accessibility rule (the handoff had 44). */
    val TouchMin = 48.dp
}

/** Skin-aware shapes: rounded in Comic, square in Pixel. */
fun pqShape(radius: Dp): Shape = if (s.radiusScale == 0f) RectangleShape else RoundedCornerShape(radius * s.radiusScale)
fun pqShapeTop(radius: Dp): Shape = if (s.radiusScale == 0f) RectangleShape else RoundedCornerShape(topStart = radius, topEnd = radius)
val PqCircle: Shape get() = if (s.radiusScale == 0f) RectangleShape else CircleShape
val isPixelSkin: Boolean get() = PqSkinState.skin == PqSkin.Pixel

/** Size helpers so one number works in both skins. */
val Number.bsp: TextUnit get() = (toFloat() * s.bodyScale).sp      // body text
val Number.lsp: TextUnit get() = (toFloat() * s.labelScale).sp     // buttons / labels

/**
 * Pixelify Sans joins f and i (and f and l) into one pixel glyph that reads as an A at text sizes
 * ("all five" read "all Ave"), so the Pixel skin's text keeps its letters apart.
 */
private val features: String? get() = if (isPixelSkin) "liga 0, clig 0" else null

object PqType {
    fun display(size: Int, line: Int? = null) = TextStyle(
        fontFamily = s.display, fontSize = (size * s.displayScale).sp,
        lineHeight = ((line ?: size) * s.displayScale * s.displayLine).sp,
        letterSpacing = if (isPixelSkin) 0.sp else 1.sp, color = PqColors.Ink, fontFeatureSettings = features,
    )
    fun number(size: Int) = display(size).copy(letterSpacing = 0.sp)

    val Display get() = display(36)
    val Title get() = display(30)
    val CardTitle get() = display(24)
    val Hero get() = display(48, 46)
    val Number get() = number(24)

    val Body get() = TextStyle(fontFamily = s.body, fontWeight = FontWeight.Medium, fontSize = 14.bsp, lineHeight = 20.bsp, color = PqColors.Ink, fontFeatureSettings = features)
    val BodyStrong get() = Body.copy(fontWeight = if (isPixelSkin) FontWeight.SemiBold else FontWeight.ExtraBold, fontSize = 15.bsp)
    val Meta get() = Body.copy(fontSize = 12.bsp, lineHeight = 16.bsp, color = PqColors.Muted)
    val MetaStrong get() = Meta.copy(fontWeight = FontWeight.Bold, color = PqColors.Ink)
    val Label get() = TextStyle(fontFamily = s.label, fontWeight = s.labelWeight, fontSize = 11.lsp, lineHeight = 15.lsp, letterSpacing = if (isPixelSkin) 0.sp else 1.3.sp, color = PqColors.Ink, fontFeatureSettings = features)
    val Button get() = Label.copy(fontSize = 13.lsp, lineHeight = 18.lsp, letterSpacing = if (isPixelSkin) 0.sp else 0.5.sp)
    val NavLabel get() = Label.copy(fontSize = 11.lsp, letterSpacing = 0.sp, fontWeight = if (isPixelSkin) FontWeight.Normal else FontWeight.Bold)
    val Arabic get() = TextStyle(fontFamily = Amiri, fontSize = 24.sp, lineHeight = 30.sp, color = PqColors.Ink)
}

/**
 * The Material colours and type the redesign's screens see, from the active skin. The app's theme
 * ([PixelQuestTheme], Theme.kt) applies it, so there is one theme, as the handoff asks.
 */
fun pqMaterialColors() = lightColorScheme(
    primary = PqColors.Red, onPrimary = PqColors.OnRed,
    primaryContainer = PqColors.Amber, onPrimaryContainer = PqColors.Ink,
    secondary = PqColors.Sky, onSecondary = PqColors.Ink,
    secondaryContainer = PqColors.Sky, onSecondaryContainer = PqColors.Ink,
    tertiary = PqColors.Lilac, onTertiary = PqColors.Ink,
    tertiaryContainer = PqColors.Lilac, onTertiaryContainer = PqColors.Ink,
    background = PqColors.Ground, onBackground = PqColors.Ink,
    surface = PqColors.Paper, onSurface = PqColors.Ink,
    surfaceVariant = PqColors.Sunken, onSurfaceVariant = PqColors.Muted,
    error = PqColors.Error, onError = PqColors.Paper,
    outline = PqColors.Ink, outlineVariant = PqColors.Divider,
)

private val typeBySkin = HashMap<PqSkin, Typography>()

/**
 * Material's type scale in the active skin's fonts, for the screens from before UI v2 that ask for
 * `MaterialTheme.typography`. Titles are the display face; body and labels are the body and label
 * faces. No colour, so text keeps its container's.
 */
fun pqTypography(): Typography = typeBySkin.getOrPut(PqSkinState.skin) {
    fun TextStyle.plain() = copy(color = Color.Unspecified)
    Typography(
        displayLarge = PqType.display(36).plain(),
        displayMedium = PqType.display(30).plain(),
        displaySmall = PqType.display(26).plain(),
        headlineLarge = PqType.display(28).plain(),
        headlineMedium = PqType.display(24).plain(),
        headlineSmall = PqType.display(22).plain(),
        titleLarge = PqType.display(22).plain(),
        titleMedium = PqType.BodyStrong.copy(fontSize = 16.bsp, lineHeight = 22.bsp).plain(),
        titleSmall = PqType.BodyStrong.copy(fontSize = 14.bsp, lineHeight = 20.bsp).plain(),
        bodyLarge = PqType.Body.copy(fontSize = 16.bsp, lineHeight = 22.bsp).plain(),
        bodyMedium = PqType.Body.plain(),
        bodySmall = PqType.Body.copy(fontSize = 12.bsp, lineHeight = 16.bsp).plain(),
        labelLarge = PqType.Button.plain(),
        labelMedium = PqType.Label.copy(fontSize = 12.lsp, lineHeight = 16.lsp).plain(),
        labelSmall = PqType.Label.plain(),
    )
}
