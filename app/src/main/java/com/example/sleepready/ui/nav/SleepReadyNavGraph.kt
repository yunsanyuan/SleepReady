package com.example.sleepready.ui.nav

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.sleepready.di.AppContainer
import com.example.sleepready.di.appContainer
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.domain.model.TimerPreset
import com.example.sleepready.permission.PermissionManager
import com.example.sleepready.permission.PermissionState
import com.example.sleepready.ui.history.HistoryScreen
import com.example.sleepready.ui.history.HistoryViewModel
import com.example.sleepready.ui.inbox.KnowledgeInboxScreen
import com.example.sleepready.ui.inbox.KnowledgeInboxViewModel
import com.example.sleepready.ui.moment.MomentScreen
import com.example.sleepready.ui.moment.MomentViewModel
import com.example.sleepready.ui.schedule.ScheduleScreen
import com.example.sleepready.ui.schedule.ScheduleViewModel
import com.example.sleepready.ui.search.SearchScreen
import com.example.sleepready.ui.search.SearchViewModel
import com.example.sleepready.ui.settings.SettingsScreen
import com.example.sleepready.ui.settings.SettingsViewModel
import com.example.sleepready.ui.statistics.StatisticsScreen
import com.example.sleepready.ui.statistics.StatisticsViewModel
import com.example.sleepready.ui.timer.CountdownScreen
import com.example.sleepready.ui.timer.CountdownViewModel
import com.example.sleepready.ui.timer.TimerPresetEditorScreen
import com.example.sleepready.ui.timer.TimerPresetListScreen
import com.example.sleepready.ui.timer.TimerPresetListViewModel
import com.example.sleepready.ui.verse.VerseDetailScreen
import com.example.sleepready.ui.verse.VerseEditorScreen
import com.example.sleepready.ui.verse.VerseScreen
import com.example.sleepready.ui.verse.VerseViewModel

@Composable
fun SleepReadyNavGraph(
    permissionManager: PermissionManager,
    navController: NavHostController,
    startDestination: String = Routes.SCHEDULE
) {
    val context = LocalContext.current
    val container: AppContainer = context.appContainer
    val application = context.applicationContext as Application

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.SCHEDULE) {
            val vm: ScheduleViewModel = viewModel(
                factory = SleepReadyViewModelFactory(container, application)
            )
            ScheduleScreen(
                viewModel = vm,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.EXPLORE) {
            val vm: KnowledgeInboxViewModel = viewModel(
                factory = SleepReadyViewModelFactory(container, application)
            )
            KnowledgeInboxScreen(
                viewModel = vm,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.SLEEP_TIMER) {
            val vm: TimerPresetListViewModel = viewModel(
                factory = SleepReadyViewModelFactory(container, application, permissionManager)
            )
            TimerPresetListScreen(
                viewModel = vm,
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToCountdown = {
                    navController.navigate(Routes.TIMER_COUNTDOWN) {
                        popUpTo(Routes.SLEEP_TIMER) { inclusive = false }
                    }
                },
                onCreate = { navController.navigate(Routes.TIMER_PRESET_NEW) },
                onEdit = { navController.navigate(Routes.timerPresetEdit(it.id)) }
            )
        }

        composable(Routes.MOMENT) {
            val vm: MomentViewModel = viewModel(
                factory = SleepReadyViewModelFactory(container, application)
            )
            MomentScreen(
                viewModel = vm,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.VERSE) {
            val vm: VerseViewModel = viewModel(
                factory = SleepReadyViewModelFactory(container, application)
            )
            VerseScreen(
                viewModel = vm,
                onOpenDetail = { id -> navController.navigate(Routes.verseDetail(id)) },
                onCreate = { navController.navigate(Routes.VERSE_NEW) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.VERSE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: VerseViewModel = viewModel(
                viewModelStoreOwner = navController.getBackStackEntry(Routes.VERSE),
                factory = SleepReadyViewModelFactory(container, application)
            )
            VerseDetailScreen(
                id = id,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.verseEdit(it)) }
            )
        }

        composable(Routes.VERSE_NEW) {
            val vm: VerseViewModel = viewModel(
                viewModelStoreOwner = navController.getBackStackEntry(Routes.VERSE),
                factory = SleepReadyViewModelFactory(container, application)
            )
            VerseEditorScreen(
                initialId = null,
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.VERSE_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: VerseViewModel = viewModel(
                viewModelStoreOwner = navController.getBackStackEntry(Routes.VERSE),
                factory = SleepReadyViewModelFactory(container, application)
            )
            VerseEditorScreen(
                initialId = id,
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TIMER_PRESET_NEW) {
            val listVm: TimerPresetListViewModel = viewModel(
                viewModelStoreOwner = navController.getBackStackEntry(Routes.SLEEP_TIMER),
                factory = SleepReadyViewModelFactory(container, application, permissionManager)
            )
            val states by rememberPermissionStates(permissionManager)
            TimerPresetEditorScreen(
                initial = null,
                permissionStates = states,
                onBack = { navController.popBackStack() },
                onSave = { preset ->
                    listVm.add(preset)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.TIMER_PRESET_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val listVm: TimerPresetListViewModel = viewModel(
                viewModelStoreOwner = navController.getBackStackEntry(Routes.SLEEP_TIMER),
                factory = SleepReadyViewModelFactory(container, application, permissionManager)
            )
            val initial: TimerPreset? by produceState<TimerPreset?>(initialValue = null, id) {
                value = listVm.getById(id)
            }
            val states by rememberPermissionStates(permissionManager)
            if (initial != null) {
                TimerPresetEditorScreen(
                    initial = initial,
                    permissionStates = states,
                    onBack = { navController.popBackStack() },
                    onSave = { preset ->
                        listVm.update(preset)
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Routes.TIMER_COUNTDOWN) {
            val vm: CountdownViewModel = viewModel(
                factory = SleepReadyViewModelFactory(container, application)
            )
            CountdownScreen(
                viewModel = vm,
                onFinished = { _ ->
                    navController.navigate(Routes.HISTORY) {
                        popUpTo(Routes.TIMER_COUNTDOWN) { inclusive = true }
                    }
                },
                onCancelled = {
                    navController.navigate(Routes.SLEEP_TIMER) {
                        popUpTo(Routes.SLEEP_TIMER) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HISTORY) {
            val vm: HistoryViewModel = viewModel(
                factory = SleepReadyViewModelFactory(container, application)
            )
            HistoryScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = SleepReadyViewModelFactory(container, application)
            )
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenStatistics = { navController.navigate(Routes.STATISTICS) }
            )
        }

        composable(Routes.SEARCH) {
            val vm: SearchViewModel = viewModel(
                factory = SleepReadyViewModelFactory(container, application)
            )
            SearchScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onJumpToSchedule = {
                    navController.navigate(Routes.SCHEDULE) {
                        popUpTo(Routes.SCHEDULE) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onJumpToExplore = {
                    navController.navigate(Routes.EXPLORE) {
                        popUpTo(Routes.SCHEDULE) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onJumpToMoment = {
                    navController.navigate(Routes.MOMENT) {
                        popUpTo(Routes.SCHEDULE) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onJumpToVerseDetail = { id ->
                    navController.navigate(Routes.verseDetail(id))
                }
            )
        }

        composable(Routes.STATISTICS) {
            val vm: StatisticsViewModel = viewModel(
                factory = SleepReadyViewModelFactory(container, application)
            )
            StatisticsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun rememberPermissionStates(pm: PermissionManager): androidx.compose.runtime.State<Map<SwitchItem, PermissionState>> {
    return pm.states.collectAsStateWithLifecycle()
}

class SleepReadyViewModelFactory(
    private val container: AppContainer,
    private val application: Application,
    private val permissionManager: PermissionManager? = null
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = create(modelClass)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            ScheduleViewModel::class.java ->
                ScheduleViewModel(application, container.scheduleRepo) as T
            KnowledgeInboxViewModel::class.java ->
                KnowledgeInboxViewModel(container.knowledgeRepo) as T
            HistoryViewModel::class.java ->
                HistoryViewModel(container.historyRepo) as T
            SettingsViewModel::class.java ->
                SettingsViewModel(application, container.prefs, container.momentRepo) as T
            CountdownViewModel::class.java ->
                CountdownViewModel(application) as T
            TimerPresetListViewModel::class.java ->
                TimerPresetListViewModel(
                    application = application,
                    repo = container.timerPresetRepo,
                    timerConfigRepo = container.timerConfigRepo,
                    prefs = container.prefs
                ) as T
            MomentViewModel::class.java ->
                MomentViewModel(application, container.momentRepo) as T
            VerseViewModel::class.java ->
                VerseViewModel(container.verseRepo) as T
            SearchViewModel::class.java ->
                SearchViewModel(container.searchEngine) as T
            StatisticsViewModel::class.java ->
                StatisticsViewModel(container.statisticsRepo) as T
            else -> error("Unknown ViewModel class ${modelClass.name}")
        }
    }
}
