package com.pixelquest.app.notification

import android.app.Application
import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** S047: on a lock screen that hides sensitive content, a notification shows a generic title only. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LockScreenPrivacyTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun everyNotification_isPrivate_withAGenericPublicVersion() {
        val notification = PqNotifications.builder(context, NotificationChannels.Spec.MISSED.id)
            .setContentTitle("Therapy session at 5")
            .setContentText("Your quest is due")
            .build()
        assertEquals(NotificationCompat.VISIBILITY_PRIVATE, notification.visibility)
        val public = notification.publicVersion
        assertNotNull(public)
        assertEquals(context.getString(R.string.notif_hidden_title), public!!.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertNull("no quest text on the lock screen", public.extras.getCharSequence(Notification.EXTRA_TEXT))
    }

    @Test
    fun noNotification_isBuiltAroundIt() {
        // Built anywhere else, a notification would skip the private default: each must set its own.
        val main = listOf(File("src/main/java"), File("app/src/main/java")).first { it.exists() }
        val unguarded = main.walkTopDown().filter { it.isFile && it.extension == "kt" && it.name != "PqNotifications.kt" }
            .filter { "NotificationCompat.Builder(" in it.readText() || "Notification.Builder(" in it.readText() }
            .filterNot { "setPublicVersion" in it.readText() }
            .map { it.name }.toList()
        assertEquals(emptyList<String>(), unguarded)
        val public = main.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .filter { "VISIBILITY_PUBLIC" in it.readText() }.map { it.name }.toList()
        assertEquals(emptyList<String>(), public)
    }
}
