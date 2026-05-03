package com.sleepsmart.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sleepsmart.app.ui.morning.MorningReportScreen
import com.sleepsmart.app.ui.permissions.PermissionsScreen
import com.sleepsmart.app.ui.settings.SettingsScreen
import com.sleepsmart.app.ui.tonight.TonightScreen
import com.sleepsmart.app.ui.tracking.TrackingScreen

object Routes {
    const val PERMISSIONS = "permissions"
    const val TONIGHT = "tonight"
    const val TRACKING = "tracking"
    const val MORNING = "morning/{sessionId}"
    const val SETTINGS = "settings"

    fun morning(sessionId: String) = "morning/$sessionId"
}

@Composable
fun AppNav(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.TONIGHT
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onComplete = {
                    navController.navigate(Routes.TONIGHT) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.TONIGHT) {
            TonightScreen(
                onTrackingStarted = { navController.navigate(Routes.TRACKING) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onMissingPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onSessionFinished = { sid ->
                    navController.navigate(Routes.morning(sid)) {
                        popUpTo(Routes.TONIGHT)
                    }
                }
            )
        }
        composable(Routes.TRACKING) {
            TrackingScreen(
                onSessionEnded = { sid ->
                    navController.navigate(Routes.morning(sid)) {
                        popUpTo(Routes.TONIGHT)
                    }
                }
            )
        }
        composable(
            route = Routes.MORNING,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { entry ->
            val sid = entry.arguments?.getString("sessionId") ?: return@composable
            MorningReportScreen(
                sessionId = sid,
                onDone = {
                    navController.navigate(Routes.TONIGHT) {
                        popUpTo(Routes.TONIGHT) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
