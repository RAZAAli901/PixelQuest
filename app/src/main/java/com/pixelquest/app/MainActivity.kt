package com.pixelquest.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.ui.notifications.NotificationsOffBanner
import com.pixelquest.app.ui.navigation.PixelNavHost
import com.pixelquest.app.ui.navigation.PqAppFrame
import com.pixelquest.app.ui.navigation.Routes
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.navigation.navigateInIslamic
import com.pixelquest.app.ui.theme.PixelQuestTheme
import kotlinx.coroutines.flow.first
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.compose.runtime.CompositionLocalProvider
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.audio.SoundManager

import androidx.compose.runtime.collectAsState
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.ui.components.PixelCrtOverlay

import androidx.activity.viewModels

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var soundManager: SoundManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var userProfileRepository: com.pixelquest.app.domain.repository.UserProfileRepository

    @Inject
    lateinit var islamicRegistry: com.pixelquest.app.islamic.IslamicRegistry

    @Inject
    lateinit var tips: com.pixelquest.app.ui.tips.TipsStore


    private val lostProgressViewModel: com.pixelquest.app.ui.screens.settings.LostProgressViewModel by viewModels()

    var isNotificationPermissionGranted by mutableStateOf(true)
        private set

    /** A screen a notification asked to open (the Namaz reminder), until the app can show it. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeFrom(intent)?.let { pendingRoute = it }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            isNotificationPermissionGranted = isGranted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingRoute = routeFrom(intent)
        checkNotificationPermission()
        // Settings > Look, read before the first frame so it opens in the player's skin (docs/ui-v2.md):
        // one preference read, not a flow run to its first value.
        val startLook = com.pixelquest.app.data.repository.SettingsRepositoryImpl.storedThemeMode(this)
        com.pixelquest.app.ui.theme.PqSkinState.skin = startLook.skin
        setContent {
            // ...and as it changes, every screen re-skins at once.
            androidx.compose.runtime.LaunchedEffect(Unit) {
                settingsRepository.themeMode.collect { com.pixelquest.app.ui.theme.PqSkinState.skin = it.skin }
            }
            val isReduceMotionEnabled by settingsRepository.isReduceMotionEnabled.collectAsState(initial = false)
            PixelQuestTheme(
                isReduceMotion = com.pixelquest.app.ui.theme.rememberReduceMotion(isReduceMotionEnabled)
            ) {
                CompositionLocalProvider(LocalSoundManager provides soundManager, com.pixelquest.app.ui.tips.LocalTipsStore provides tips) {
                    val isCrtEnabled by settingsRepository.isCrtEnabled.collectAsState(initial = false)
                    val isSimpleModeEnabled by settingsRepository.simpleModeEnabled.collectAsState(initial = false)
                    // A player who has played counts as onboarded even without the flag (OnboardingGate).
                    val onboardingFlow = androidx.compose.runtime.remember {
                        kotlinx.coroutines.flow.combine(
                            settingsRepository.onboardingComplete,
                            userProfileRepository.getProfile()
                        ) { flag, profile -> com.pixelquest.app.domain.OnboardingGate.isDone(flag, profile) }
                    }
                    val onboardingComplete by onboardingFlow.collectAsState(initial = true)
                    // CRT overlay is restricted to the Pixel skin, and forced OFF in Simple Mode.
                    val effectiveTheme =
                        if (com.pixelquest.app.ui.theme.isPixelSkin) com.pixelquest.app.ui.theme.ThemeMode.Pixel
                        else com.pixelquest.app.ui.theme.ThemeMode.Comic
                    val shouldApplyCrt = com.pixelquest.app.domain.model.CrtFilterPolicy.shouldApplyCrt(
                        isCrtSettingEnabled = isCrtEnabled,
                        effectiveThemeMode = effectiveTheme,
                        isSimpleModeEnabled = isSimpleModeEnabled
                    )
                    PixelCrtOverlay(enabled = shouldApplyCrt) {
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val rootViewModel: com.pixelquest.app.ui.navigation.PqRootViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                        val root by rootViewModel.state.collectAsState()
                        // A screen asked for by a notification opens once the app is past the
                        // splash and onboarding, over Home, so Back returns there.
                        androidx.compose.runtime.LaunchedEffect(pendingRoute, currentRoute, onboardingComplete) {
                            val route = pendingRoute ?: return@LaunchedEffect
                            if (!onboardingComplete || currentRoute == null || currentRoute == Screen.Splash.route || currentRoute == Screen.Onboarding.route) {
                                return@LaunchedEffect
                            }
                            pendingRoute = null
                            when {
                                currentRoute == route -> Unit
                                // The Namaz Tracker: over the Islamic hub, so Back goes there, then Home.
                                Routes.isIslamic(route) ->
                                    navController.navigateInIslamic(route)
                                else -> navController.navigate(route) { launchSingleTop = true }
                            }
                        }
                        // A signed-in account without a Darood Party username is asked for one,
                        // once, the same way: past the splash and onboarding.
                        val usernamePrompt: com.pixelquest.app.darood.ui.username.UsernamePromptViewModel =
                            androidx.hilt.navigation.compose.hiltViewModel()
                        val askForUsername by usernamePrompt.prompt.collectAsState()
                        androidx.compose.runtime.LaunchedEffect(askForUsername, currentRoute, onboardingComplete) {
                            if (!askForUsername || !onboardingComplete || currentRoute == null ||
                                currentRoute == Screen.Splash.route || currentRoute == Screen.Onboarding.route
                            ) {
                                return@LaunchedEffect
                            }
                            usernamePrompt.promptShown()
                            navController.navigate(Screen.UsernameSetup.route) { launchSingleTop = true }
                        }

                        // The bottom bar (docs/ui-v2.md): a screen opened from a tab keeps that tab lit,
                        // and the trophy opens the Leaderboard.
                        PqAppFrame(navController, root) { innerPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (!isNotificationPermissionGranted && currentRoute != Screen.Splash.route && currentRoute != Screen.Onboarding.route) {
                                    NotificationsOffBanner(
                                        onAllow = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", packageName, null)
                                                }
                                                startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                                PixelNavHost(
                                    navController = navController,
                                    modifier = Modifier.weight(1f),
                                    onboardingComplete = onboardingComplete,
                                    islamicItems = islamicRegistry.providers
                                )
                                // An empty game while the phone kept an automatic backup: a lost database.
                                if (onboardingComplete && currentRoute != null && currentRoute != Screen.Splash.route && currentRoute != Screen.Onboarding.route) {
                                    com.pixelquest.app.ui.screens.settings.LostProgressPrompt(lostProgressViewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** The screen [intent] asks to open, if it's one a notification may open (MainActivity is exported). */
    private fun routeFrom(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA_OPEN_ROUTE)?.takeIf(::isOpenable)

    companion object {
        /** A screen to open, from a notification's tap. */
        const val EXTRA_OPEN_ROUTE = "com.pixelquest.app.extra.OPEN_ROUTE"

        // The Namaz Tracker's, and its route before the Islamic hub, which 1.4's reminders carry.
        private val OPENABLE_ROUTES = setOf(Screen.Namaz.route, com.pixelquest.app.ui.navigation.Routes.LEGACY_NAMAZ)

        /** A study party's screen, for its reminders: only a party id (a UUID), nothing else. */
        private val STUDY_PARTY_ROUTE = Regex("^" + Regex.escape(Screen.PartiesParty.createRoute("")) + "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")

        /** A Darood Party's room, or its weekly ranking, for their notifications: only a party id (a UUID). */
        private val DAROOD_WEEK_ROUTE = Regex("^" + Regex.escape(Screen.DaroodWeek.createRoute("@")).replace("@", "\\E[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}\\Q") + "$")
        private val DAROOD_ROOM_ROUTE = Regex("^" + Regex.escape(Screen.DaroodRoom.createRoute("@")).replace("@", "\\E[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}\\Q") + "$")

        /** Whether a notification may open [route]: a fixed screen, or a party by its id. */
        fun isOpenable(route: String): Boolean = route in OPENABLE_ROUTES || STUDY_PARTY_ROUTE.matches(route) || DAROOD_WEEK_ROUTE.matches(route) || DAROOD_ROOM_ROUTE.matches(route)
    }

    /**
     * Notifications aren't asked for at launch, but when the player first turns a reminder on
     * (rememberAskForNotifications). The banner offering them shows only once they've said no.
     */
    private fun checkNotificationPermission() {
        val ask = com.pixelquest.app.ui.notifications.NotificationAsk
        isNotificationPermissionGranted = !ask.needed(this) || !ask.asked(this)
    }

    override fun onResume() {
        super.onResume()
        // Back from Android's permission prompt or its settings: the banner follows the answer.
        checkNotificationPermission()
    }
}
