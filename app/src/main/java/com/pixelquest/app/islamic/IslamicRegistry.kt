package com.pixelquest.app.islamic

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One item on the Islamic hub. Each is bound into a Hilt set (`@Binds @IntoSet` in IslamicModule),
 * so adding an item touches neither the side menu nor the hub (docs/islamic-hub.md).
 */
interface IslamicItemProvider {
    /** Its place on the hub: lower comes first. */
    val order: Int

    /** Its card, kept current: whether it's shown, and its badge. */
    fun item(): Flow<IslamicItem>

    /**
     * Its own screens, added to the Islamic section's graph (`islamicGraph`), so a new item needs no
     * change to the app's navigation. Empty for items whose screens were there before the hub.
     */
    fun NavGraphBuilder.screens(navController: NavHostController) {}
}

/** Every item bound into the hub's set, in order. */
@Singleton
class IslamicRegistry @Inject constructor(
    providers: Set<@JvmSuppressWildcards IslamicItemProvider>
) {
    /** The providers in the hub's order; ties go by class name, so the order never depends on the set's. */
    val providers: List<IslamicItemProvider> =
        providers.sortedWith(compareBy<IslamicItemProvider>({ it.order }, { it.javaClass.name }))

    /**
     * Every item in order, hidden ones too (enabled false), updated whenever one changes. Two items
     * with the same id or route are a mistake in the bindings, and fail here rather than on the hub.
     */
    val items: Flow<List<IslamicItem>> =
        if (this.providers.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(this.providers.map { it.item() }) { it.toList() }.onEach { items ->
                check(items.distinctBy { it.id }.size == items.size) { "Two Islamic items share an id: ${items.map { it.id }}" }
                check(items.distinctBy { it.route }.size == items.size) { "Two Islamic items share a route: ${items.map { it.route }}" }
            }
        }
}
