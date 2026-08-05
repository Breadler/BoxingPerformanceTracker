package com.breadler.boxingperformancetracker.navigation

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.breadler.boxingperformancetracker.data.SessionSummary
import com.breadler.boxingperformancetracker.ui.components.ProcessingStatusBar
import com.breadler.boxingperformancetracker.ui.screens.HomeScreen
import com.breadler.boxingperformancetracker.ui.screens.NewSessionScreen
import com.breadler.boxingperformancetracker.ui.screens.PreviousSessionsScreen
import com.breadler.boxingperformancetracker.ui.screens.ProcessingScreen
import com.breadler.boxingperformancetracker.ui.screens.SessionPlaybackScreen
import com.breadler.boxingperformancetracker.ui.viewmodel.StrykoViewModel

private object Routes {
    const val Home = "home"
    const val NewSession = "newSession"
    const val PreviousSessions = "previousSessions"
    const val Processing = "processing"
    const val SessionPlayback = "sessionPlayback"
    const val SessionIdArg = "sessionId"

    fun sessionPlayback(sessionId: String): String = "$SessionPlayback/$sessionId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as Application
    val viewModel: StrykoViewModel = viewModel(factory = StrykoViewModel.factory(application))
    val sessions by viewModel.sessions.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val queuedSessionNames by viewModel.queuedSessionNames.collectAsState()
    val prepareSeconds by viewModel.prepareSeconds.collectAsState()
    val workSeconds by viewModel.workSeconds.collectAsState()
    val useFrontCamera by viewModel.useFrontCamera.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    fun openCompletedSession() {
        importState.completedSessionId?.let { sessionId ->
            navController.navigate(Routes.sessionPlayback(sessionId)) {
                launchSingleTop = true
            }
        }
        viewModel.acknowledgeCompletion()
    }

    fun exitToHome() {
        navController.popBackStack(Routes.Home, inclusive = false)
    }

    // If the processing screen is the current destination but there's nothing left to show
    // there (no active import, nothing queued), skip past it automatically - e.g. after
    // opening a finished session and then exiting session playback, which lands back on what
    // would otherwise be a dead loading screen. Gated on currentRoute (not left as a check
    // inside ProcessingScreen itself) so it can't fire while ProcessingScreen is still
    // transiently composed during the transition away from it - that raced with "open session"
    // navigation and popped it right back off.
    LaunchedEffect(currentRoute, importState.isActive, queuedSessionNames.isEmpty()) {
        if (currentRoute == Routes.Processing && !importState.isActive && queuedSessionNames.isEmpty()) {
            navController.popBackStack()
        }
    }

    Scaffold(
        bottomBar = {
            if (importState.isActive && currentRoute != Routes.Processing) {
                ProcessingStatusBar(
                    state = importState,
                    onTapProgress = { navController.navigate(Routes.Processing) },
                    onOpen = ::openCompletedSession,
                    onDismiss = viewModel::acknowledgeCompletion,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    onStartNewSession = {
                        navController.navigate(Routes.NewSession)
                    },
                    onViewPreviousSessions = {
                        navController.navigate(Routes.PreviousSessions)
                    },
                )
            }
            composable(Routes.NewSession) {
                NewSessionScreen(
                    onImportVideo = { uri ->
                        viewModel.importVideo(uri)
                        navController.navigate(Routes.Processing)
                    },
                    onExit = ::exitToHome,
                    initialPrepareSeconds = prepareSeconds,
                    initialWorkSeconds = workSeconds,
                    initialUseFrontCamera = useFrontCamera,
                    onPrepareSecondsChanged = viewModel::setPrepareSeconds,
                    onWorkSecondsChanged = viewModel::setWorkSeconds,
                    onUseFrontCameraChanged = viewModel::setUseFrontCamera,
                )
            }
            composable(Routes.Processing) {
                ProcessingScreen(
                    importState = importState,
                    queuedSessionNames = queuedSessionNames,
                    onExit = {
                        navController.popBackStack()
                    },
                    onNewRound = {
                        navController.navigate(Routes.NewSession)
                    },
                    onOpenSession = ::openCompletedSession,
                )
            }
            composable(Routes.PreviousSessions) {
                PreviousSessionsScreen(
                    sessions = sessions,
                    onExit = ::exitToHome,
                    onOpenSession = { sessionId ->
                        navController.navigate(Routes.sessionPlayback(sessionId))
                    },
                    onDeleteSession = viewModel::deleteSession,
                )
            }
            composable(
                route = "${Routes.SessionPlayback}/{${Routes.SessionIdArg}}",
                arguments = listOf(navArgument(Routes.SessionIdArg) { type = NavType.StringType }),
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString(Routes.SessionIdArg)
                val sessionState by produceState<SessionSummary?>(
                    initialValue = sessionId?.let { id -> sessions.firstOrNull { it.id == id } },
                    key1 = sessionId,
                    key2 = sessions,
                ) {
                    if (value == null && sessionId != null) {
                        value = viewModel.getSession(sessionId)
                    }
                }
                val session = sessionState
                if (session == null) {
                    PreviousSessionsScreen(
                        sessions = sessions,
                        onExit = ::exitToHome,
                        onOpenSession = { selectedSessionId ->
                            navController.navigate(Routes.sessionPlayback(selectedSessionId))
                        },
                        onDeleteSession = viewModel::deleteSession,
                    )
                } else {
                    SessionPlaybackScreen(
                        session = session,
                        onExit = {
                            navController.popBackStack()
                        },
                        onStartNewRound = {
                            navController.navigate(Routes.NewSession)
                        },
                    )
                }
            }
        }
    }
}
