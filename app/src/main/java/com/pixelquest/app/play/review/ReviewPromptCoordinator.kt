package com.pixelquest.app.play.review

import com.pixelquest.app.BuildConfig
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Emits a request only after a successful completion and only when local policy permits it. */
@Singleton
class ReviewPromptCoordinator @Inject constructor(
    private val settings: SettingsRepository,
    private val profiles: UserProfileRepository,
    private val completions: TaskCompletionRepository,
    private val state: ReviewPromptStateStore,
) {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests: SharedFlow<Unit> = _requests

    suspend fun onSuccessfulQuestCompletion(nowMillis: Long = System.currentTimeMillis()) {
        val profile = profiles.getProfile().first() ?: return
        val snapshot = ReviewPromptPolicy.Snapshot(
            onboardingComplete = settings.onboardingComplete.first(),
            profileCreatedAtMillis = profile.createdAt,
            completedQuestCount = completions.getAllLogs().first().count { it.wasCompleted },
            nowMillis = nowMillis,
            currentVersionCode = BuildConfig.VERSION_CODE,
            lastAttemptAtMillis = state.lastAttemptAt(),
            lastAttemptVersionCode = state.lastAttemptVersionCode(),
        )
        if (!ReviewPromptPolicy.isEligible(snapshot)) return
        state.recordAttempt(nowMillis, BuildConfig.VERSION_CODE)
        _requests.tryEmit(Unit)
    }
}
