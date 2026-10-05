package com.pixelquest.app.data.remote

import com.pixelquest.app.di.NetworkModule
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.auth
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs under Robolectric: Supabase Auth saves sessions through Android settings, which need an
 * Android context (on the plain JVM the client fails to build).
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34], application = android.app.Application::class)
class SupabaseClientSmokeTest {

    @org.junit.Before
    fun initSettings() {
        // In the app, androidx.startup runs this initializer from the manifest; Robolectric doesn't.
        androidx.startup.AppInitializer.getInstance(androidx.test.core.app.ApplicationProvider.getApplicationContext())
            .initializeComponent(com.russhwolf.settings.SettingsInitializer::class.java)
    }

    @Test
    fun supabaseClient_isConfiguredWithPlugins() {
        val client = SupabaseClientProvider.client
        assertNotNull("SupabaseClient should not be null", client)

        val postgrest = SupabaseClientProvider.postgrest
        assertNotNull("Postgrest plugin should be installed and accessible", postgrest)

        val auth = SupabaseClientProvider.auth
        assertNotNull("Auth plugin should be installed and accessible", auth)
    }

    @Test
    fun networkModule_providesDependencies() {
        val client = NetworkModule.provideSupabaseClient()
        assertNotNull("NetworkModule should provide SupabaseClient", client)

        val postgrest = NetworkModule.providePostgrest(client)
        assertNotNull("NetworkModule should provide Postgrest", postgrest)

        val auth = NetworkModule.provideAuth(client)
        assertNotNull("NetworkModule should provide Auth", auth)
    }

    @Test
    fun postgrest_canTargetProfilesTable() {
        val client = SupabaseClientProvider.client
        val profilesTable = client.postgrest["profiles"]
        assertNotNull("Postgrest table reference for 'profiles' should be valid", profilesTable)
        assertTrue("Table name should match profiles", profilesTable.table == "profiles")
    }
}
