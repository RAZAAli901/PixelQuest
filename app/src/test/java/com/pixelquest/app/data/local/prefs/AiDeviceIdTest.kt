package com.pixelquest.app.data.local.prefs

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AiDeviceIdTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theId_isARandomUuid_keptAcrossCalls() {
        val deviceId = AiDeviceId(context)

        val first = deviceId.get()

        assertTrue(AiDeviceId.isValid(first))
        assertEquals(first, deviceId.get())
        // A new instance on the same install reads the same id.
        assertEquals(first, AiDeviceId(context).get())
    }

    @Test
    fun aDamagedId_isReplaced() {
        context.getSharedPreferences(AiDeviceId.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("key_ai_device_id", "*").commit()

        val id = AiDeviceId(context).get()

        assertNotEquals("*", id)
        assertTrue(AiDeviceId.isValid(id))
    }

    @Test
    fun separateInstalls_getDifferentIds() {
        val one = AiDeviceId(context.getSharedPreferences("install_one", Context.MODE_PRIVATE)).get()
        val two = AiDeviceId(context.getSharedPreferences("install_two", Context.MODE_PRIVATE)).get()

        assertNotEquals(one, two)
    }
}
