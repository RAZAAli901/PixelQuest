package com.pixelquest.app.ui.screens.account

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.auth.AuthRepository
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.data.local.entity.UserProfileEntity
import com.pixelquest.app.data.remote.SupabaseResult
import com.pixelquest.app.data.repository.CloudProfileRepository
import com.pixelquest.app.testing.FakeUserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Settings > Account > Export my cloud data (J173): the server's JSON lands in the file picked. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CloudExportTest {

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class Exporting(private val answer: SupabaseResult<String>) : AuthRepository {
        override val currentUser = MutableStateFlow<AuthUser?>(AuthUser("user-1", "aria@example.com", "Aria"))
        override suspend fun exchangeGoogleIdToken(idToken: String, rawNonce: String?) = SupabaseResult.Success(AuthUser("user-1", "aria@example.com", "Aria"))
        override suspend fun signOut() = SupabaseResult.Success(Unit)
        override suspend fun getInitialUser(): AuthUser? = currentUser.value
        override suspend fun exportMyData() = answer
    }

    private class Idle : CloudProfileRepository {
        override suspend fun updateOptInAndSync(optIn: Boolean, displayName: String) = SupabaseResult.Success(Unit)
        override suspend fun syncProfileToCloud() = SupabaseResult.Success(Unit)
        override suspend fun pushProfileToCloud() = SupabaseResult.Success(false)
    }

    private fun account(answer: SupabaseResult<String>) = AccountViewModel(
        FakeUserProfileRepository(UserProfileEntity(username = "Aria", avatarId = "avatar_hero", supabaseUserId = "user-1")),
        Idle(),
        authRepository = Exporting(answer)
    )

    private fun waitFor(vm: AccountViewModel, condition: (CloudExport) -> Boolean) {
        repeat(200) {
            if (condition(vm.uiState.value.cloudExport)) return
            Thread.sleep(10)
        }
        throw AssertionError("still ${vm.uiState.value.cloudExport}")
    }

    private val context = ApplicationProvider.getApplicationContext<Application>()

    /** A file the player picked, as the document picker hands it back. */
    private fun picked(): Pair<Uri, ByteArrayOutputStream> {
        val uri = Uri.parse("content://test.documents/cloud-${System.nanoTime()}.json")
        val written = ByteArrayOutputStream()
        org.robolectric.Shadows.shadowOf(context.contentResolver).registerOutputStream(uri, written)
        return uri to written
    }

    @Test
    fun theExport_isSavedToTheChosenFile() {
        val (uri, written) = picked()
        val vm = account(SupabaseResult.Success("""{"format":1,"chess_games":[]}"""))
        vm.exportCloudData(context, uri)
        waitFor(vm) { it == CloudExport.SAVED }
        assertEquals("""{"format":1,"chess_games":[]}""", written.toString())
    }

    @Test
    fun offline_saysItCouldnt_andWritesNothing() {
        val (uri, written) = picked()
        val vm = account(SupabaseResult.NetworkError(IOException("offline")))
        vm.exportCloudData(context, uri)
        waitFor(vm) { it == CloudExport.FAILED }
        assertEquals("", written.toString())
    }
}
