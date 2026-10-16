package com.pixelquest.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixelquest.app.domain.repository.TaskRepository
import com.pixelquest.app.ui.components.PixelBottomNavBar
import com.pixelquest.app.ui.components.PixelNotificationPermissionBanner
import com.pixelquest.app.ui.navigation.PixelNavHost
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.navigation.navigateToTab
import com.pixelquest.app.ui.theme.PixelQuestTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.compose.runtime.CompositionLocalProvider
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.audio.SoundManager

import androidx.compose.runtime.collectAsState
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.ui.components.PixelCrtOverlay

import androidx.activity.viewModels
import com.pixelquest.app.ui.theme.ThemeViewModel

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

    private val themeViewModel: ThemeViewModel by viewModels()

    private val lostProgressViewModel: com.pixelquest.app.ui.screens.settings.LostProgressViewModel by viewModels()

    var isNotificationPermissionGranted by mutableStateOf(true)
        private set

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            isNotificationPermissionGranted = isGranted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsState()
            val isReduceMotionEnabled by settingsRepository.isReduceMotionEnabled.collectAsState(initial = false)
            PixelQuestTheme(
                themeMode = themeMode,
                isReduceMotion = com.pixelquest.app.ui.theme.rememberReduceMotion(isReduceMotionEnabled)
            ) {
                CompositionLocalProvider(LocalSoundManager provides soundManager) {
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
                    // CRT overlay is architecturally restricted to Pixel theme mode only, and forced OFF in Simple Mode.
                    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                    val effectiveTheme = themeMode.resolveEffective(isSystemDark)
                    val shouldApplyCrt = com.pixelquest.app.domain.model.CrtFilterPolicy.shouldApplyCrt(
                        isCrtSettingEnabled = isCrtEnabled,
                        effectiveThemeMode = effectiveTheme,
                        isSimpleModeEnabled = isSimpleModeEnabled
                    )
                    PixelCrtOverlay(enabled = shouldApplyCrt) {
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val menuDrawer = com.pixelquest.app.ui.menu.rememberMenuDrawerController()
                        val menuViewModel: com.pixelquest.app.ui.menu.MenuViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                        val menuHeader by menuViewModel.header.collectAsState()
                        val menuSelected by menuViewModel.selected.collectAsState()

                        // The side menu slides in over the screens and the bottom bar (docs/ai-coach-compact.md).
                        com.pixelquest.app.ui.menu.MenuDrawerScaffold(
                            controller = menuDrawer,
                            // Swiping in from the left edge opens it on the tabs. A dialog is its own
                            // window and takes the touches while it's up, so it can't open behind one.
                            gesturesEnabled = com.pixelquest.app.ui.navigation.Routes.isTab(currentRoute),
                            drawerContent = {
                                com.pixelquest.app.ui.menu.MenuDrawerHeader(menuHeader)
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    color = com.pixelquest.app.ui.theme.PixelTheme.colors.onSurface.copy(alpha = 0.15f)
                                )
                                com.pixelquest.app.ui.menu.MenuDrawerItems(
                                    items = com.pixelquest.app.ui.menu.MenuItems.all,
                                    context = com.pixelquest.app.ui.menu.MenuContext(isSimpleMode = menuHeader.isSimpleMode),
                                    selected = menuSelected,
                                    onItemClick = { item ->
                                        menuDrawer.close()
                                        menuViewModel.select(item.id)
                                        // The Leaderboard is also a tab: open it the way its tab does.
                                        if (com.pixelquest.app.ui.navigation.Routes.isTab(item.route)) {
                                            navController.navigateToTab(item.route)
                                        } else {
                                            navController.navigate(item.route) { launchSingleTop = true }
                                        }
                                    }
                                )
                            }
                        ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                if (currentRoute != Screen.Splash.route && currentRoute != Screen.Onboarding.route) {
                                    PixelBottomNavBar(
                                        currentRoute = currentRoute,
                                        onNavigate = { route -> navController.navigateToTab(route) }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (!isNotificationPermissionGranted && currentRoute != Screen.Splash.route && currentRoute != Screen.Onboarding.route) {
                                    PixelNotificationPermissionBanner(
                                        onRequestPermission = {
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
                                    onboardingComplete = onboardingComplete
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
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            isNotificationPermissionGranted = granted
            if (!granted) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        } else {
            isNotificationPermissionGranted = true
        }
    }
}
