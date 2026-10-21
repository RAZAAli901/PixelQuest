package com.pixelquest.app

import android.app.Application
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What a notification's tap may open: MainActivity is exported, so anything else is ignored. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MainActivityRoutesTest {

    @Test
    fun aStudyParty_opensByItsIdAlone() {
        assertTrue(MainActivity.isOpenable("parties/party/5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f"))
        for (route in listOf(
            "parties/party/",
            "parties/party/not-an-id",
            "parties/party/5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f/item/x",
            "parties/party/5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f?x=1",
            "settings"
        )) assertFalse(route, MainActivity.isOpenable(route))
    }

    @Test
    fun theNamazScreen_stillOpens() {
        assertTrue(MainActivity.isOpenable(com.pixelquest.app.ui.navigation.Screen.Namaz.route))
    }
}
