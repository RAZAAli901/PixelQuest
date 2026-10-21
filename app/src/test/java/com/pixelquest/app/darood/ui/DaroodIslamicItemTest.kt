package com.pixelquest.app.darood.ui

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pixelquest.app.R
import com.pixelquest.app.data.local.AppDatabase
import com.pixelquest.app.darood.data.DaroodPartyRepositoryImpl
import com.pixelquest.app.party.data.remote.PartyMemberDto
import com.pixelquest.app.testing.FakeDaroodServer
import com.pixelquest.app.testing.FakeSettingsRepository
import com.pixelquest.app.ui.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Darood Party's card on the Islamic hub: there while its flag is on, with its invitations. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DaroodIslamicItemTest {

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val server = FakeDaroodServer(me = "me")
    private val repository = DaroodPartyRepositoryImpl(server, db.daroodDao(), { server.me }, now = { 1_000L }, zone = { java.time.ZoneId.of("UTC") })
    private val settings = FakeSettingsRepository()
    private val item = DaroodIslamicItem(settings, repository)

    @After
    fun tearDown() {
        db.close()
    }

    private fun invite(party: String) {
        server.members += PartyMemberDto(party, "me", "member", "invited", null, null)
    }

    @Test
    fun theCard_opensTheParties_withTheBeadIcons() = runBlocking {
        val card = item.item().first()

        assertEquals("darood", card.id)
        assertEquals(R.string.darood_title, card.titleRes)
        assertEquals("Deen's tile opens the parties of every kind", Screen.Parties.route, card.route)
        assertEquals(R.drawable.ic_darood_pixel, card.icon.pixel)
        assertEquals(R.drawable.ic_darood_comic, card.icon.comic)
        assertEquals(0, card.badgeCount)
        assertTrue(card.enabled)
    }

    @Test
    fun theBadge_countsInvitations_notPartiesAlreadyJoined() = runBlocking {
        server.seedParty("Friday circle", "me")
        invite(server.seedParty("Family", "u2"))
        invite(server.seedParty("Masjid", "u3"))
        repository.refreshParties()

        val card = item.item().first()
        assertEquals(2, card.badgeCount)
        assertEquals(R.plurals.islamic_darood_badge, card.badgeLabel)
    }

    @Test
    fun withItsFlagOff_theCardIsHidden() = runBlocking {
        settings.daroodAvailable.value = false

        assertFalse(item.item().first().enabled)
    }
}
