package com.example.sleepready

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sleepready.di.appContainer
import com.example.sleepready.permission.LocalPermissionManager
import com.example.sleepready.permission.PermissionManager
import com.example.sleepready.ui.nav.Routes
import com.example.sleepready.ui.nav.SleepReadyNavGraph
import com.example.sleepready.ui.theme.SleepReadyTheme
import com.example.sleepready.widget.WidgetReceiver

class MainActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>
    private val pendingNavRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(this)

        mediaProjectionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result -> onMediaProjectionResult(result) }

        pendingNavRoute.value = extractNavRoute(intent)

        setContent {
            val themeMode by appContainer.prefs.observeThemeMode()
                .collectAsStateWithLifecycle(initialValue = appContainer.prefs.themeMode)
            val colorScheme by appContainer.prefs.observeColorScheme()
                .collectAsStateWithLifecycle(initialValue = appContainer.prefs.colorScheme)
            SleepReadyTheme(themeMode = themeMode, colorScheme = colorScheme) {
                CompositionLocalProvider(LocalPermissionManager provides permissionManager) {
                    val navController = rememberNavController()

                    val pending = pendingNavRoute.value
                    LaunchedEffect(pending) {
                        if (pending != null) {
                            navController.navigate(pending) {
                                launchSingleTop = true
                                restoreState = true
                            }
                            pendingNavRoute.value = null
                        }
                    }

                    AppShell(navController = navController, permissionManager = permissionManager)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavRoute.value = extractNavRoute(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::permissionManager.isInitialized) permissionManager.refresh()
    }

    fun requestScreenRecordingProjection() {
        val mgr = getSystemService(MediaProjectionManager::class.java) ?: return
        mediaProjectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    private fun onMediaProjectionResult(result: ActivityResult) {
        val data = result.data ?: return
        val mgr = getSystemService(MediaProjectionManager::class.java) ?: return
        val projection = runCatching { mgr.getMediaProjection(result.resultCode, data) }.getOrNull() ?: return
        appContainer.screenRecordingController.attach(projection)
    }

    private fun extractNavRoute(intent: Intent?): String? {
        val raw = intent?.getStringExtra(WidgetReceiver.EXTRA_NAV) ?: return null
        return when (raw) {
            Routes.SLEEP_TIMER, Routes.TIMER_COUNTDOWN, Routes.HISTORY, Routes.SETTINGS,
            Routes.EXPLORE, Routes.SCHEDULE, Routes.MOMENT, Routes.VERSE -> raw
            else -> null
        }
    }
}

// ========== App Shell ==========

@Composable
private fun AppShell(navController: NavHostController, permissionManager: PermissionManager) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute == null || currentRoute !in nonTopLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) SleepReadyBottomBar(currentRoute, navController)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            SleepReadyNavGraph(
                permissionManager = permissionManager,
                navController = navController
            )
        }
    }
}

@Composable
private fun SleepReadyBottomBar(currentRoute: String?, navController: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            val iconColor by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                animationSpec = tween(300), label = "navColor"
            )
            val scale by animateFloatAsState(
                if (selected) 1.1f else 1f,
                animationSpec = tween(300), label = "navScale"
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.size(26.dp).scale(scale)
                    )
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = iconColor
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )
        }
    }
}

private data class BottomNavItem(
    val route: String, val label: String, val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.SCHEDULE, "印记", Icons.Outlined.CalendarMonth),
    BottomNavItem(Routes.EXPLORE, "心动", Icons.Outlined.Explore),
    BottomNavItem(Routes.SLEEP_TIMER, "入梦", Icons.Outlined.Bedtime),
    BottomNavItem(Routes.MOMENT, "时光", Icons.Outlined.CollectionsBookmark),
    BottomNavItem(Routes.VERSE, "拾句", Icons.Outlined.AutoAwesome)
)

private val nonTopLevelRoutes = setOf(
    Routes.TIMER_PRESET_NEW, Routes.TIMER_PRESET_EDIT,
    Routes.TIMER_COUNTDOWN, Routes.HISTORY, Routes.SETTINGS,
    Routes.SEARCH, Routes.STATISTICS,
    Routes.VERSE_DETAIL, Routes.VERSE_NEW, Routes.VERSE_EDIT
)
