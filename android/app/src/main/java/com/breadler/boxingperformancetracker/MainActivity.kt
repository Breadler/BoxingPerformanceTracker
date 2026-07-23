package com.breadler.boxingperformancetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.breadler.boxingperformancetracker.data.SampleSessions
import com.breadler.boxingperformancetracker.ui.screens.HomeScreen
import com.breadler.boxingperformancetracker.ui.screens.SessionViewScreen
import com.breadler.boxingperformancetracker.ui.screens.SessionsScreen
import com.breadler.boxingperformancetracker.ui.theme.BoxingPerformanceTrackerTheme

private object Routes {
    const val Home = "home"
    const val Sessions = "sessions"
    const val SessionView = "sessionView"
    const val SessionIdArg = "sessionId"

    fun sessionView(sessionId: String): String = "$SessionView/$sessionId"
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoxingPerformanceTrackerTheme {
                BoxingApp()
            }
        }
    }
}

@Composable
private fun BoxingApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Home,
    ) {
        composable(Routes.Home) {
            HomeScreen(
                onViewPreviousSessions = {
                    navController.navigate(Routes.Sessions)
                },
            )
        }
        composable(Routes.Sessions) {
            SessionsScreen(
                sessions = SampleSessions.all(),
                onBack = {
                    navController.popBackStack()
                },
                onOpenSession = { sessionId ->
                    navController.navigate(Routes.sessionView(sessionId))
                },
            )
        }
        composable(
            route = "${Routes.SessionView}/{${Routes.SessionIdArg}}",
            arguments = listOf(navArgument(Routes.SessionIdArg) { type = NavType.StringType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Routes.SessionIdArg)
            val session = sessionId?.let(SampleSessions::findById)
            if (session == null) {
                SessionsScreen(
                    sessions = SampleSessions.all(),
                    onBack = { navController.popBackStack() },
                    onOpenSession = { selectedSessionId ->
                        navController.navigate(Routes.sessionView(selectedSessionId))
                    },
                )
            } else {
                SessionViewScreen(
                    session = session,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
