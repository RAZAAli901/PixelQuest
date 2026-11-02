package com.pixelquest.app.testing

import com.pixelquest.app.data.remote.SupabaseResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.SocketTimeoutException

class FakeRepositoriesTest {

    @Test
    fun fakeTaskRepository_failureSwitch_throwsTimeout() = runBlocking {
        val repo = FakeTaskRepository()
        repo.setFailure(FakeFailureType.TIMEOUT)

        try {
            repo.getAllTasks()
            fail("Expected SocketTimeoutException")
        } catch (expected: SocketTimeoutException) {
            assertTrue(expected.message?.contains("timeout") == true)
        }
    }

    @Test
    fun fakePartyRepository_failureSwitch_returnsNetworkError() = runBlocking {
        val repo = FakePartyRepository()
        repo.setFailure(FakeFailureType.NO_NETWORK)

        val result = repo.refreshParties()
        assertTrue(result is SupabaseResult.NetworkError)
    }

    @Test
    fun fakeLeaderboardRepository_failureSwitch_returnsServerError() = runBlocking {
        val repo = FakeLeaderboardRepository()
        repo.setFailure(FakeFailureType.SERVER_ERROR)

        val result = repo.getProfiles()
        assertTrue(result is SupabaseResult.ServerError)
        assertEquals(500, (result as SupabaseResult.ServerError).code)
    }

    @Test
    fun fakeAuthRepository_emptyResponseSwitch_returnsNullUser() = runBlocking {
        val repo = FakeAuthRepository()
        assertNotNull(repo.getInitialUser())

        repo.setFailure(FakeFailureType.EMPTY_RESPONSE)
        assertEquals(null, repo.getInitialUser())
    }

    @Test
    fun fakeStudyRepository_inMemoryState_managesItems() = runBlocking {
        val repo = FakeStudyRepository()
        val item = TestDataBuilders.buildStudyItem(partyId = "party-1", title = "Physics Quiz")
        repo.itemsFlow.value = mapOf("party-1" to listOf(item))

        val items = repo.observeItems("party-1").first()
        assertEquals(1, items.size)
        assertEquals("Physics Quiz", items[0].title)
    }
}
