package com.pixelquest.app.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.chat.LocalChatSimpleMode
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.ThemeMode
import java.util.Locale

val LocalSimpleMode = staticCompositionLocalOf { false }

object ScreenSizes {
    val PhoneCompact = DpSize(360.dp, 640.dp)
    val PhoneStandard = DpSize(411.dp, 891.dp)
    val TabletLandscape = DpSize(1280.dp, 800.dp)
    val TabletPortrait = DpSize(800.dp, 1280.dp)
}

data class ComposeTestConfiguration(
    val themeMode: ThemeMode = ThemeMode.Comic,
    val isDark: Boolean = false,
    val isSimpleMode: Boolean = false,
    val isReduceMotion: Boolean = false,
    val fontScale: Float = 1.0f,
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    val locale: Locale = Locale.US,
    val screenSize: DpSize? = null,
)

@Composable
fun ThemedTestContent(
    configuration: ComposeTestConfiguration = ComposeTestConfiguration(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val customDensity = Density(density = density.density, fontScale = configuration.fontScale)
    val androidConfig = LocalConfiguration.current
    val updatedAndroidConfig = android.content.res.Configuration(androidConfig).apply {
        setLocale(configuration.locale)
        fontScale = configuration.fontScale
    }

    CompositionLocalProvider(
        LocalDensity provides customDensity,
        LocalLayoutDirection provides configuration.layoutDirection,
        LocalConfiguration provides updatedAndroidConfig,
        LocalChatSimpleMode provides configuration.isSimpleMode,
        LocalSimpleMode provides configuration.isSimpleMode,
    ) {
        PixelQuestTheme(
            themeMode = configuration.themeMode,
            isReduceMotion = configuration.isReduceMotion,
        ) {
            if (configuration.screenSize != null) {
                Box(
                    modifier = Modifier.size(
                        width = configuration.screenSize.width,
                        height = configuration.screenSize.height,
                    ),
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}

fun ComposeContentTestRule.setThemedContent(
    configuration: ComposeTestConfiguration = ComposeTestConfiguration(),
    content: @Composable () -> Unit,
) {
    setContent {
        ThemedTestContent(configuration = configuration, content = content)
    }
}

fun ComposeContentTestRule.setPixelTheme(
    isDark: Boolean = false,
    isSimpleMode: Boolean = false,
    content: @Composable () -> Unit,
) = setThemedContent(
    ComposeTestConfiguration(themeMode = ThemeMode.Pixel, isDark = isDark, isSimpleMode = isSimpleMode),
    content = content,
)

fun ComposeContentTestRule.setComicTheme(
    isDark: Boolean = false,
    isSimpleMode: Boolean = false,
    content: @Composable () -> Unit,
) = setThemedContent(
    ComposeTestConfiguration(themeMode = ThemeMode.Comic, isDark = isDark, isSimpleMode = isSimpleMode),
    content = content,
)

fun ComposeContentTestRule.setSimpleMode(
    themeMode: ThemeMode = ThemeMode.Comic,
    content: @Composable () -> Unit,
) = setThemedContent(
    ComposeTestConfiguration(themeMode = themeMode, isSimpleMode = true),
    content = content,
)

fun ComposeContentTestRule.setFontScale(
    fontScale: Float,
    content: @Composable () -> Unit,
) = setThemedContent(
    ComposeTestConfiguration(fontScale = fontScale),
    content = content,
)

fun ComposeContentTestRule.setRtl(
    locale: Locale = Locale("ar"),
    content: @Composable () -> Unit,
) = setThemedContent(
    ComposeTestConfiguration(
        layoutDirection = LayoutDirection.Rtl,
        locale = locale,
    ),
    content = content,
)

fun ComposeContentTestRule.setScreenSize(
    screenSize: DpSize,
    content: @Composable () -> Unit,
) = setThemedContent(
    ComposeTestConfiguration(screenSize = screenSize),
    content = content,
)
