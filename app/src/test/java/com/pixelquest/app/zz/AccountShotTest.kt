package com.pixelquest.app.zz

import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.AuthUser
import com.pixelquest.app.ui.screens.account.AccountContent
import com.pixelquest.app.ui.screens.account.AccountUiState
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class AccountShotTest : ScratchShot() {
    @Test fun signedOut() = shot("account-out", PqSkin.Comic) {
        PixelQuestTheme { AccountContent(authState = AuthUiState.SignedOut, accountState = AccountUiState(), cloudAvailable = true, googleAvailable = true, emailAvailable = true) }
    }

    @Test fun signedIn() = shot("account-in", PqSkin.Pixel) {
        PixelQuestTheme {
            AccountContent(
                authState = AuthUiState.SignedIn(AuthUser("0123456789abcdef", "hero@pixelquest.test", null)),
                accountState = AccountUiState(isOptedIn = true, displayNameInput = "DragonSlayer"),
                cloudAvailable = true
            )
        }
    }
}
