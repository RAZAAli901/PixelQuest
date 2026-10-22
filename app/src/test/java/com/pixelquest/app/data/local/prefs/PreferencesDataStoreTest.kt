package com.pixelquest.app.data.local.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferencesDataStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val rawDataStore by lazy {
        PreferenceDataStoreFactory.create(scope = scope) {
            temp.newFile("test_preferences.preferences_pb")
        }
    }
    private val preferencesStore: PreferencesDataStore by lazy {
        DefaultPreferencesDataStore(rawDataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun defaultValues_areReturned_whenKeyNotSet() = runBlocking {
        val boolKey = booleanPreferencesKey("unknown_bool")
        assertFalse(preferencesStore.get(boolKey, false).first())

        val intKey = intPreferencesKey("unknown_int")
        assertEquals(42, preferencesStore.get(intKey, 42).first())

        val stringVal = preferencesStore.getString("missing_str", "default_val").first()
        assertEquals("default_val", stringVal)
    }

    @Test
    fun setAndGet_roundTrip_allPrimitives() = runBlocking {
        preferencesStore.setBoolean("flag", true)
        assertTrue(preferencesStore.getBoolean("flag", false).first())

        preferencesStore.setInt("count", 100)
        assertEquals(100, preferencesStore.getInt("count", 0).first())

        preferencesStore.setLong("timestamp", 123456789L)
        assertEquals(123456789L, preferencesStore.getLong("timestamp", 0L).first())

        preferencesStore.setFloat("ratio", 3.14f)
        assertEquals(3.14f, preferencesStore.getFloat("ratio", 0f).first(), 0.001f)

        preferencesStore.setString("name", "PixelQuest")
        assertEquals("PixelQuest", preferencesStore.getString("name", "").first())

        preferencesStore.setStringSet("tags", setOf("tag1", "tag2"))
        assertEquals(setOf("tag1", "tag2"), preferencesStore.getStringSet("tags").first())
    }

    @Test
    fun getSnapshot_retrievesCurrentValueSynchronouslyInCoroutines() = runBlocking {
        preferencesStore.setString("theme", "retro")
        val theme = preferencesStore.getSnapshot(stringPreferencesKey("theme"), "default")
        assertEquals("retro", theme)
    }

    @Test
    fun removeAndClear_removesValues() = runBlocking {
        preferencesStore.setString("k1", "v1")
        preferencesStore.setString("k2", "v2")

        preferencesStore.remove(stringPreferencesKey("k1"))
        assertEquals("default", preferencesStore.getString("k1", "default").first())
        assertEquals("v2", preferencesStore.getString("k2", "default").first())

        preferencesStore.clear()
        assertEquals("default", preferencesStore.getString("k2", "default").first())
    }
}
