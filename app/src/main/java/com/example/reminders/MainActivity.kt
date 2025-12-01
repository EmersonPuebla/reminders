package com.example.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.reminders.ui.AppViewModelProvider
import com.example.reminders.ui.screens.*
import com.example.reminders.ui.theme.RemindersTheme

import androidx.compose.animation.*
import androidx.compose.animation.core.tween

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val theme by settingsViewModel.theme.collectAsState()

            val darkTheme = when (theme) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                Theme.SYSTEM -> isSystemInDarkTheme()
            }

            RemindersTheme(darkTheme = darkTheme) {
                RemindersApp()
            }
        }
    }
}

@Composable
fun RemindersApp() {
    val navController = rememberNavController()
    val animationDuration = 150
    val animationSpec = tween<Float>(animationDuration)

    NavHost(navController = navController, startDestination = "reminders") {
        composable("reminders",
            enterTransition = {
                EnterTransition.None
            },
            popExitTransition = {
                ExitTransition.None
            }
        ) {
            ListReminderScreen(
                navController = navController,
                onSettingsClick = { navController.navigate("settings") }
            )

        }
        dialog(
            "create_reminder",
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            EditReminderScreen(onBack = { navController.popBackStack() })
        }
        dialog(
            route = "edit_reminder/{reminderId}",
            arguments = listOf(navArgument("reminderId") { type = NavType.IntType }),
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            EditReminderScreen(onBack = { navController.popBackStack() })
        }
        composable("settings",
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(animationDuration),
                    initialOffsetX = { fullWidth -> fullWidth }
                )
            },

            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(animationDuration),
                    targetOffsetX = { fullWidth -> fullWidth }
                )
            }
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
