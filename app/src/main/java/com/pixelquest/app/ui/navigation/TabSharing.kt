package com.pixelquest.app.ui.navigation

import kotlinx.coroutines.flow.SharingStarted

/**
 * How a tab's screens share their data: kept running for a minute after the player leaves the tab,
 * so hopping between Home, Quests, Deen and Hero finds each one ready instead of re-reading the
 * database (and re-running its sums) during the slide in. Stopped after that, as before.
 */
val TabSharing: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = 60_000)
