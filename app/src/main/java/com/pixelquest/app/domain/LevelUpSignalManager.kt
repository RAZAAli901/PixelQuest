package com.pixelquest.app.domain

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LevelUpSignalManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pixelquest_level_up", Context.MODE_PRIVATE)

    private val _pendingLevelUp = MutableStateFlow<Int?>(getPendingLevelUpInternal())
    val pendingLevelUp: StateFlow<Int?> = _pendingLevelUp.asStateFlow()

    private fun getPendingLevelUpInternal(): Int? {
        val level = prefs.getInt(KEY_PENDING_LEVEL, -1)
        return if (level > 0) level else null
    }

    fun setPendingLevelUp(newLevel: Int) {
        prefs.edit().putInt(KEY_PENDING_LEVEL, newLevel).apply()
        _pendingLevelUp.value = newLevel
    }

    fun clearPendingLevelUp() {
        prefs.edit().remove(KEY_PENDING_LEVEL).apply()
        _pendingLevelUp.value = null
    }

    companion object {
        private const val KEY_PENDING_LEVEL = "key_pending_level_up"
    }
}
