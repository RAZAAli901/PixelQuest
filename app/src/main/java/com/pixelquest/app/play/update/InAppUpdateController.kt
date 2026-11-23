package com.pixelquest.app.play.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

interface InAppUpdateController {
    fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>)
    fun resume(launcher: ActivityResultLauncher<IntentSenderRequest>)
    fun onFlowResult(resultCode: Int)
    fun onStart(onDownloaded: () -> Unit)
    fun onStop()
    fun completeUpdate()
}
