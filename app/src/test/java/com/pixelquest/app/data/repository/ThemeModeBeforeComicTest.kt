package com.pixelquest.app.data.repository

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Home screen's Comic toggle switches back to whatever theme was active before Comic.
 * SettingsRepositoryImpl records that theme every time Comic is chosen, from any screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ThemeModeBeforeComicTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        context.getSharedPreferences("pixelquest_settings", Context.MODE_PRIVATE).edit().clear().commit()
        repository = SettingsRepositoryImpl(context)
    }

    @Test
    fun freshInstall_defaultsToPixel() = runBlocking {
        assertEquals(ThemeMode.Pixel, repository.getThemeModeBeforeComic())
    }

    @Test
    fun choosingComic_recordsThePreviousTheme() = runBlocking {
        repository.setThemeMode(ThemeMode.Light)
        repository.setThemeMode(ThemeMode.Comic)

        assertEquals(ThemeMode.Comic, repository.themeMode.first())
        assertEquals(ThemeMode.Light, repository.getThemeModeBeforeComic())
    }

    @Test
    fun choosingComicFromFollowSystem_recordsFollowSystem() = runBlocking {
        repository.setThemeMode(ThemeMode.System)
        repository.setThemeMode(ThemeMode.Comic)

        assertEquals(ThemeMode.System, repository.getThemeModeBeforeComic())
    }

    @Test
    fun choosingComicTwice_keepsTheThemeFromBeforeComic() = runBlocking {
        repository.setThemeMode(ThemeMode.Light)
        repository.setThemeMode(ThemeMode.Comic)
        repository.setThemeMode(ThemeMode.Comic)

        assertEquals(ThemeMode.Light, repository.getThemeModeBeforeComic())
    }

    @Test
    fun choosingComicOnFreshInstall_recordsTheDefaultPixel() = runBlocking {
        repository.setThemeMode(ThemeMode.Comic)

        assertEquals(ThemeMode.Pixel, repository.getThemeModeBeforeComic())
    }
}
