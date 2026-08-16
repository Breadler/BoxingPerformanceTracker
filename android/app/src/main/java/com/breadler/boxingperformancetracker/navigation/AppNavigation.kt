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

// Navigation route names
private object Routes {
    const val Home = "home"
    const val NewSession = "newSession"
    const val PreviousSessions = "previousSessions"
    const val Processing = "processing"
    const val SessionPlayback = "sessionPlayback"
    const val SessionIdArg = "sessionId"

    fun sessionPlayback(sessionId: String): String = "$SessionPlayback/$sessionId"
}

// Root nav host wiring all screens and shared view model state
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

    // Navigate to the just-finished session and clear its status
    fun openCompletedSession() {
        importState.completedSessionId?.let { sessionId ->
            navController.navigate(Routes.sessionPlayback(sessionId)) {
                launchSingleTop = true
            }
        }
        viewModel.acknowledgeCompletion()
    }

    // Pop back to the home screen
    fun exitToHome() {
        navController.popBackStack(Routes.Home, inclusive = false)
    }

    // Auto-skip empty processing screen
    LaunchedEffect(currentRoute, importState.isActive, queuedSessionNames.isEmpty()) {
        if (currentRoute == Routes.Processing && !importState.isActive && queuedSessionNames.isEmpty()) {
            navController.popBackStack()
        }
    }

    // Docked processing status bar, shown on every screen but Processing itself
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
            // Session playback, loading the session by id if not already in memory
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
