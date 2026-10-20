package com.pixelquest.app.islamic

import com.pixelquest.app.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The hub's registry: its items in order, kept current, and no two alike. */
class IslamicRegistryTest {

    private class Item(override val order: Int, id: String, route: String = "islamic/$id", badge: Int = 0) : IslamicItemProvider {
        val state = MutableStateFlow(
            IslamicItem(id, R.string.menu_settings, R.string.menu_settings, IslamicIcon("•"), route, badgeCount = badge)
        )

        override fun item(): Flow<IslamicItem> = state
    }

    @Test
    fun theItems_comeInOrder_whateverTheSetsOrder() = runTest {
        val registry = IslamicRegistry(linkedSetOf(Item(30, "quran"), Item(10, "namaz"), Item(20, "darood")))

        assertEquals(listOf("namaz", "darood", "quran"), registry.items.first().map { it.id })
    }

    @Test
    fun anItemsChange_reachesTheList() = runTest {
        val namaz = Item(10, "namaz")
        val registry = IslamicRegistry(setOf(namaz, Item(20, "darood")))

        namaz.state.value = namaz.state.value.copy(badgeCount = 2, enabled = false)

        val now = registry.items.first().first { it.id == "namaz" }
        assertEquals(2, now.badgeCount)
        assertEquals("a hidden item is still listed, for the hub to leave out", false, now.enabled)
    }

    @Test
    fun noItems_isAnEmptyList() = runTest {
        assertEquals(emptyList<IslamicItem>(), IslamicRegistry(emptySet()).items.first())
    }

    @Test
    fun twoItemsWithOneId_fail() = runTest {
        val registry = IslamicRegistry(setOf(Item(10, "namaz", route = "islamic/a"), Item(20, "namaz", route = "islamic/b")))

        val failure = runCatching { registry.items.first() }.exceptionOrNull()
        assertTrue(failure.toString(), failure is IllegalStateException)
    }

    @Test
    fun twoItemsWithOneRoute_fail() = runTest {
        val registry = IslamicRegistry(setOf(Item(10, "a", route = "islamic/x"), Item(20, "b", route = "islamic/x")))

        assertTrue(runCatching { registry.items.first() }.exceptionOrNull() is IllegalStateException)
    }
}
