package com.breadler.boxingperformancetracker.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.breadler.boxingperformancetracker.data.SessionSummary
import com.breadler.boxingperformancetracker.ui.screens.HomeScreen
import com.breadler.boxingperformancetracker.ui.screens.NewSessionScreen
import com.breadler.boxingperformancetracker.ui.screens.PreviousSessionsScreen
import com.breadler.boxingperformancetracker.ui.screens.SessionPlaybackScreen
import com.breadler.boxingperformancetracker.ui.viewmodel.StrykoViewModel

private object Routes {
    const val Home = "home"
    const val NewSession = "newSession"
    const val PreviousSessions = "previousSessions"
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

    NavHost(
        navController = navController,
        startDestination = Routes.Home,
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
                importState = importState,
                onImportVideo = viewModel::importVideo,
                onImportFinished = { sessionId ->
                    navController.navigate(Routes.sessionPlayback(sessionId)) {
                        launchSingleTop = true
                    }
                    viewModel.clearImportState()
                },
                onExit = {
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.PreviousSessions) {
            PreviousSessionsScreen(
                sessions = sessions,
                onExit = {
                    navController.popBackStack()
                },
                onOpenSession = { sessionId ->
                    navController.navigate(Routes.sessionPlayback(sessionId))
                },
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
                    onExit = { navController.popBackStack() },
                    onOpenSession = { selectedSessionId ->
                        navController.navigate(Routes.sessionPlayback(selectedSessionId))
                    },
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