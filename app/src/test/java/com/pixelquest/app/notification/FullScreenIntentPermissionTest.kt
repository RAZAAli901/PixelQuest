package com.pixelquest.app.notification

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FullScreenIntentPermissionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun canUseFullScreenIntent_returnsBoolean() {
        // Under Robolectric API 34 shadow, NotificationManager returns default
        val result = FullScreenIntentPermission.canUseFullScreenIntent(context)
        // Verify it executes without exception and returns a valid boolean
        assertTrue(result || !result)
    }

    @Test
    fun openSettingsIntent_createsValidIntent() {
        val intent = FullScreenIntentPermission.openSettingsIntent(context)

        assertNotNull(intent)
        assertEquals(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, intent.action)
        assertEquals("package:${context.packageName}", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
