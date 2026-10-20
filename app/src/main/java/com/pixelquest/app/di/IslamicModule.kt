package com.pixelquest.app.di

import com.pixelquest.app.darood.ui.DaroodIslamicItem
import com.pixelquest.app.islamic.IslamicItemProvider
import com.pixelquest.app.ui.screens.namaz.NamazIslamicItem
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds

/**
 * The Islamic hub's items (docs/islamic-hub.md). Each item is one `@Binds @IntoSet` line here; the
 * hub and the side menu read the set through IslamicRegistry.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class IslamicModule {

    /** The set itself, so it can be injected before any item is bound into it. */
    @Multibinds
    abstract fun islamicItems(): Set<IslamicItemProvider>

    @Binds
    @IntoSet
    abstract fun namaz(item: NamazIslamicItem): IslamicItemProvider

    @Binds
    @IntoSet
    abstract fun darood(item: DaroodIslamicItem): IslamicItemProvider
}
