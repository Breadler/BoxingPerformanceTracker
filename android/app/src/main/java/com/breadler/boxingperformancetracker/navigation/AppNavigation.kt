package com.breadler.boxingperformancetracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.breadler.boxingperformancetracker.model.SampleBoxingSessions
import com.breadler.boxingperformancetracker.ui.screens.HomeScreen
import com.breadler.boxingperformancetracker.ui.screens.NewSessionScreen
import com.breadler.boxingperformancetracker.ui.screens.PreviousSessionsScreen
import com.breadler.boxingperformancetracker.ui.screens.SessionPlaybackScreen

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
                onExit = {
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.PreviousSessions) {
            PreviousSessionsScreen(
                sessions = SampleBoxingSessions.all(),
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
            val session = sessionId?.let(SampleBoxingSessions::findById)
            if (session == null) {
                PreviousSessionsScreen(
                    sessions = SampleBoxingSessions.all(),
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