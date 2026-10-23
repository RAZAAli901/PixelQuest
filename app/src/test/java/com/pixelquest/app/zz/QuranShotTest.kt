package com.pixelquest.app.zz

import com.pixelquest.app.quran.domain.Revelation
import com.pixelquest.app.quran.domain.Surah
import com.pixelquest.app.quran.ui.QuranHomeScreen
import com.pixelquest.app.quran.ui.list.ContinueReading
import com.pixelquest.app.quran.ui.list.SurahListUiState
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class QuranShotTest : ScratchShot() {
    private val surahs = listOf(
        Surah(1, "ـ", "Al-Faatiha", "The Opening", Revelation.MECCAN, 7, 5, null),
        Surah(2, "ـ", "Al-Baqara", "The Cow", Revelation.MEDINAN, 286, 87, null),
        Surah(3, "ـ", "Aal-i-Imraan", "The Family of Imraan", Revelation.MEDINAN, 200, 89, null),
        Surah(4, "ـ", "An-Nisaa", "The Women", Revelation.MEDINAN, 176, 92, null),
    )
    private val state = SurahListUiState(surahs = surahs, loaded = true, continueReading = ContinueReading(surahs[1], 142))
    @Test fun comic() = shot("quran", PqSkin.Comic) { QuranHomeScreen(onNavigateBack = {}, list = state) }
    @Test fun pixel() = shot("quran", PqSkin.Pixel) { QuranHomeScreen(onNavigateBack = {}, list = state) }
}
