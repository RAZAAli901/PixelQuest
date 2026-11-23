package com.pixelquest.app.tune.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pixelquest.app.BuildConfig
import com.pixelquest.app.tune.model.TuneCatalogResponse
import com.pixelquest.app.tune.model.TuneDownloadResponse
import com.pixelquest.app.tune.model.TuneItem
import com.pixelquest.app.tune.model.TuneTarget
import com.pixelquest.app.util.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tuneDataStore by preferencesDataStore(name = "tune_preferences")

interface TuneRepository {
    val questTuneIdFlow: Flow<String?>
    val namazTuneIdFlow: Flow<String?>
    suspend fun selectDefault(target: TuneTarget)
    suspend fun select(target: TuneTarget, tune: TuneItem): Boolean
    suspend fun fetchCatalog(): Result<List<TuneItem>>
    suspend fun fetchTuneDownloadUrl(tuneId: String): Result<String>
    suspend fun downloadTune(tune: TuneItem): Boolean
    fun isTuneCached(tuneId: String): Boolean
    fun removeCachedTune(tuneId: String): Boolean
    fun getTotalCacheSizeBytes(): Long
}

@Singleton
class TuneRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val httpClient: HttpClient,
    private val cacheManager: TuneCacheManager
) : TuneRepository {

    companion object {
        private const val TAG = "TuneRepository"
        val KEY_QUEST_TUNE_ID = stringPreferencesKey("quest_tune_id")
        val KEY_NAMAZ_TUNE_ID = stringPreferencesKey("namaz_tune_id")
    }

    override val questTuneIdFlow: Flow<String?> = context.tuneDataStore.data.map { preferences ->
        preferences[KEY_QUEST_TUNE_ID]
    }

    override val namazTuneIdFlow: Flow<String?> = context.tuneDataStore.data.map { preferences ->
        preferences[KEY_NAMAZ_TUNE_ID]
    }

    private suspend fun setQuestTuneId(tuneId: String?) {
        context.tuneDataStore.edit { preferences ->
            if (tuneId == null) {
                preferences.remove(KEY_QUEST_TUNE_ID)
            } else {
                preferences[KEY_QUEST_TUNE_ID] = tuneId
            }
        }
    }

    private suspend fun setNamazTuneId(tuneId: String?) {
        context.tuneDataStore.edit { preferences ->
            if (tuneId == null) {
                preferences.remove(KEY_NAMAZ_TUNE_ID)
            } else {
                preferences[KEY_NAMAZ_TUNE_ID] = tuneId
            }
        }
    }

    private fun getAccessToken(): String? {
        val session = supabaseClient.auth.currentSessionOrNull()
        return session?.accessToken
    }

    override suspend fun fetchCatalog(): Result<List<TuneItem>> {
        val token = getAccessToken() ?: return Result.failure(IllegalStateException("User is not signed in"))
        val endpoint = "${BuildConfig.SUPABASE_URL}/functions/v1/tune-catalog"

        return try {
            val response = httpClient.get(endpoint) {
                header("Authorization", "Bearer $token")
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            }
            if (response.status.value in 200..299) {
                val catalog: TuneCatalogResponse = response.body()
                Result.success(catalog.tunes)
            } else {
                Result.failure(RuntimeException("Failed to fetch catalog: ${response.status}"))
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Error fetching tune catalog", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchTuneDownloadUrl(tuneId: String): Result<String> {
        val token = getAccessToken() ?: return Result.failure(IllegalStateException("User is not signed in"))
        val endpoint = "${BuildConfig.SUPABASE_URL}/functions/v1/tune-catalog?tuneId=$tuneId"

        return try {
            val response = httpClient.get(endpoint) {
                header("Authorization", "Bearer $token")
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            }
            if (response.status.value in 200..299) {
                val data: TuneDownloadResponse = response.body()
                val url = data.tune.downloadUrl
                if (!url.isNullOrBlank()) {
                    Result.success(url)
                } else {
                    Result.failure(IllegalStateException("Missing downloadUrl in tune response"))
                }
            } else {
                Result.failure(RuntimeException("Failed to fetch download url: ${response.status}"))
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Error fetching tune download url for $tuneId", e)
            Result.failure(e)
        }
    }

    override suspend fun downloadTune(tune: TuneItem): Boolean {
        val urlResult = fetchTuneDownloadUrl(tune.id)
        val url = urlResult.getOrNull() ?: return false
        return cacheManager.downloadAndCache(tune, url)
    }

    override suspend fun selectDefault(target: TuneTarget) {
        when (target) {
            TuneTarget.QUEST -> setQuestTuneId(null)
            TuneTarget.NAMAZ -> setNamazTuneId(null)
        }
    }

    override suspend fun select(target: TuneTarget, tune: TuneItem): Boolean {
        if (!tune.categories.contains(target.category)) return false
        if (!cacheManager.isCached(tune.id) && !downloadTune(tune)) return false
        if (!cacheManager.isCached(tune.id)) return false
        when (target) {
            TuneTarget.QUEST -> setQuestTuneId(tune.id)
            TuneTarget.NAMAZ -> setNamazTuneId(tune.id)
        }
        return true
    }

    override fun isTuneCached(tuneId: String): Boolean {
        return cacheManager.isCached(tuneId)
    }

    override fun removeCachedTune(tuneId: String): Boolean {
        return cacheManager.removeTune(tuneId)
    }

    override fun getTotalCacheSizeBytes(): Long {
        return cacheManager.getTotalCacheSizeBytes()
    }
}
