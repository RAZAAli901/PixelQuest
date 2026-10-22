package com.pixelquest.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified preferences wrapper around DataStore<Preferences> (Q041).
 * Consolidates reactive preference observation, typed accessors, and corruption handling.
 */
interface PreferencesDataStore {
    val dataStore: DataStore<Preferences>

    fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T>
    suspend fun <T> getSnapshot(key: Preferences.Key<T>, defaultValue: T): T
    suspend fun <T> set(key: Preferences.Key<T>, value: T)
    suspend fun <T> remove(key: Preferences.Key<T>)
    suspend fun clear()

    // Typed convenience helpers
    fun getBoolean(key: String, defaultValue: Boolean = false): Flow<Boolean>
    suspend fun setBoolean(key: String, value: Boolean)

    fun getInt(key: String, defaultValue: Int = 0): Flow<Int>
    suspend fun setInt(key: String, value: Int)

    fun getLong(key: String, defaultValue: Long = 0L): Flow<Long>
    suspend fun setLong(key: String, value: Long)

    fun getFloat(key: String, defaultValue: Float = 0f): Flow<Float>
    suspend fun setFloat(key: String, value: Float)

    fun getString(key: String, defaultValue: String = ""): Flow<String>
    suspend fun setString(key: String, value: String)

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Flow<Set<String>>
    suspend fun setStringSet(key: String, value: Set<String>)
}

open class DefaultPreferencesDataStore(
    override val dataStore: DataStore<Preferences>,
) : PreferencesDataStore {

    override fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }

    override suspend fun <T> getSnapshot(key: Preferences.Key<T>, defaultValue: T): T =
        get(key, defaultValue).first()

    override suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    override suspend fun <T> remove(key: Preferences.Key<T>) {
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        get(booleanPreferencesKey(key), defaultValue)

    override suspend fun setBoolean(key: String, value: Boolean) =
        set(booleanPreferencesKey(key), value)

    override fun getInt(key: String, defaultValue: Int): Flow<Int> =
        get(intPreferencesKey(key), defaultValue)

    override suspend fun setInt(key: String, value: Int) =
        set(intPreferencesKey(key), value)

    override fun getLong(key: String, defaultValue: Long): Flow<Long> =
        get(longPreferencesKey(key), defaultValue)

    override suspend fun setLong(key: String, value: Long) =
        set(longPreferencesKey(key), value)

    override fun getFloat(key: String, defaultValue: Float): Flow<Float> =
        get(floatPreferencesKey(key), defaultValue)

    override suspend fun setFloat(key: String, value: Float) =
        set(floatPreferencesKey(key), value)

    override fun getString(key: String, defaultValue: String): Flow<String> =
        get(stringPreferencesKey(key), defaultValue)

    override suspend fun setString(key: String, value: String) =
        set(stringPreferencesKey(key), value)

    override fun getStringSet(key: String, defaultValue: Set<String>): Flow<Set<String>> =
        get(stringSetPreferencesKey(key), defaultValue)

    override suspend fun setStringSet(key: String, value: Set<String>) =
        set(stringSetPreferencesKey(key), value)
}

@Singleton
class AppPreferencesDataStore(
    dataStore: DataStore<Preferences>,
) : DefaultPreferencesDataStore(dataStore) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(PREFERENCES_NAME) },
        ),
    )

    companion object {
        const val PREFERENCES_NAME = "pixelquest_app_preferences"
    }
}
