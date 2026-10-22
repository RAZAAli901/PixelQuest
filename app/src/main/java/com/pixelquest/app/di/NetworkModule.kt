package com.pixelquest.app.di

import com.pixelquest.app.data.remote.SupabaseClientProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(@dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context): SupabaseClient {
        // The session sealed with a Keystore key, before the client exists (S034).
        if (SupabaseClientProvider.sessionManager == null) {
            SupabaseClientProvider.sessionManager = com.pixelquest.app.auth.KeystoreSessionManager(context)
        }
        return SupabaseClientProvider.client
    }

    @Provides
    @Singleton
    fun providePostgrest(client: SupabaseClient): Postgrest {
        return SupabaseClientProvider.postgrest
    }

    @Provides
    @Singleton
    fun provideAuth(client: SupabaseClient): Auth {
        return SupabaseClientProvider.auth
    }
}
