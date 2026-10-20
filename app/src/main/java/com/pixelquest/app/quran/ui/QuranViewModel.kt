package com.pixelquest.app.quran.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.quran.data.QuranImporter
import com.pixelquest.app.quran.data.QuranLoadState
import com.pixelquest.app.quran.data.local.QuranDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The one-time "Preparing the Quran" message, while the Quran isn't on the phone yet.
 *
 * @param progress from 0 to 1 while it's loading; null while waiting for the load to start.
 * @param failed the bundle couldn't be loaded (it never ships that way: its tests stop it).
 */
data class QuranPreparing(val progress: Float? = null, val failed: Boolean = false)

/** Whether the Quran is ready to read, or still being prepared. */
@HiltViewModel
class QuranViewModel @Inject constructor(importer: QuranImporter, dao: QuranDao) : ViewModel() {

    /** Null once the Quran is on the phone, and no new bundle is being loaded. */
    val preparing: StateFlow<QuranPreparing?> = combine(importer.state, dao.observeTranslations()) { state, translations ->
        when {
            state is QuranLoadState.Loading -> QuranPreparing(state.progress)
            translations.isNotEmpty() -> null
            state is QuranLoadState.Failed -> QuranPreparing(failed = true)
            else -> QuranPreparing()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuranPreparing())
}
