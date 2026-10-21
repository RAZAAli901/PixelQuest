package com.pixelquest.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.data.backup.BackupSummary
import com.pixelquest.app.data.backup.LostProgress
import com.pixelquest.app.ui.components.PqConfirmDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Offers the automatic backup when the game is found empty (LostProgress): after Android deletes a
 * corrupt database the app opens the dashboard, not the welcome screen, so this is where it's asked.
 */
@HiltViewModel
class LostProgressViewModel @Inject constructor(
    private val lostProgress: LostProgress
) : ViewModel() {

    /** "11 Oct" and what the backup holds (BackupSummary). */
    data class Prompt(val day: String, val summary: String)

    private val _prompt = MutableStateFlow<Prompt?>(null)
    val prompt: StateFlow<Prompt?> = _prompt.asStateFlow()

    private var offer: LostProgress.Offer? = null
    private var checked = false

    /** Looks once per launch, when the game's screens are first shown (after onboarding, if any). */
    fun check() {
        if (checked) return
        checked = true
        viewModelScope.launch {
            val found = try {
                lostProgress.check()
            } catch (e: Exception) {
                null
            } ?: return@launch
            offer = found
            _prompt.value = Prompt(found.day.format(DAY), BackupSummary.describe(found.payload))
        }
    }

    fun restore() {
        val chosen = offer ?: return
        offer = null
        _prompt.value = null
        viewModelScope.launch {
            // If it fails, nothing changed: the backup is still listed in Settings > Data & Backup.
            try {
                lostProgress.restore(chosen)
            } catch (e: Exception) {
            }
        }
    }

    fun notNow() {
        val chosen = offer ?: return
        offer = null
        _prompt.value = null
        viewModelScope.launch(Dispatchers.IO) { lostProgress.decline(chosen) }
    }

    private companion object {
        val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    }
}

@Composable
fun LostProgressPrompt(viewModel: LostProgressViewModel) {
    LaunchedEffect(Unit) { viewModel.check() }
    val prompt by viewModel.prompt.collectAsState()
    prompt?.let { LostProgressDialog(it, onRestore = viewModel::restore, onNotNow = viewModel::notNow) }
}

@Composable
fun LostProgressDialog(prompt: LostProgressViewModel.Prompt, onRestore: () -> Unit, onNotNow: () -> Unit) {
    PqConfirmDialog(
        title = "RESTORE YOUR GAME?",
        message = "Your game is empty, but the phone kept an automatic backup from ${prompt.day}: ${prompt.summary}" +
            "\n\nThis can happen when the phone had to start the game's data over. " +
            "Not now keeps it in Settings > Data & Backup.",
        confirm = "RESTORE ${prompt.day.uppercase()}",
        dismiss = "NOT NOW",
        onConfirm = onRestore,
        onDismiss = onNotNow
    )
}
