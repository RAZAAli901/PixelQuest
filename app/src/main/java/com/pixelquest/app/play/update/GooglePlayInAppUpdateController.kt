package com.pixelquest.app.play.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Play Store bridge. Missing Play Store/services is a no-op, never an app error. */
@Singleton
class GooglePlayInAppUpdateController @Inject constructor(
    @ApplicationContext context: Context,
    private val prompts: UpdatePromptStateStore,
) : InAppUpdateController {
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    private var onDownloaded: (() -> Unit)? = null
    private var listening = false
    private var startedType: Int? = null
    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) onDownloaded?.invoke()
    }

    override fun onStart(onDownloaded: () -> Unit) {
        this.onDownloaded = onDownloaded
        if (!listening) {
            manager.registerListener(listener)
            listening = true
        }
    }

    override fun onStop() {
        if (listening) manager.unregisterListener(listener)
        listening = false
        onDownloaded = null
    }

    override fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            when (info.installStatus()) {
                InstallStatus.DOWNLOADED -> onDownloaded?.invoke()
                else -> if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    when (
                        InAppUpdatePolicy.chooseType(
                            priority = info.updatePriority(),
                            stalenessDays = info.clientVersionStalenessDays(),
                            flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
                            immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
                        )
                    ) {
                        InAppUpdatePolicy.Type.FLEXIBLE -> start(info, AppUpdateType.FLEXIBLE, launcher)
                        InAppUpdatePolicy.Type.IMMEDIATE -> start(info, AppUpdateType.IMMEDIATE, launcher)
                        InAppUpdatePolicy.Type.NONE -> Unit
                    }
                }
            }
        }
    }

    override fun resume(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> onDownloaded?.invoke()
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                    start(info, AppUpdateType.IMMEDIATE, launcher)
            }
        }
    }

    override fun onFlowResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_CANCELED && startedType == AppUpdateType.FLEXIBLE) {
            prompts.recordFlexibleDecline(System.currentTimeMillis())
        }
        startedType = null
    }

    override fun completeUpdate() {
        manager.completeUpdate()
    }

    private fun start(
        info: com.google.android.play.core.appupdate.AppUpdateInfo,
        type: Int,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ) {
        if (type == AppUpdateType.FLEXIBLE && !prompts.canOfferFlexibleUpdate(System.currentTimeMillis())) return
        startedType = type
        manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
    }
}
