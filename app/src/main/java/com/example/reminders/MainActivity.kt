package com.example.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RemindersApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RemindersApp() {
    val navController = rememberNavController()
    val animationDuration = 300
    val animationSpec = tween<Float>(animationDuration)

    NavHost(navController = navController, startDestination = "reminders") {

        composable(
            "reminders",
            exitTransition = {
                scaleOut(animationSpec = tween(animationDuration), targetScale = 0.9f) +
                    fadeOut(animationSpec = tween(animationDuration))
            },
            popEnterTransition = {
                scaleIn(animationSpec = tween(animationDuration), initialScale = 0.9f) +
                    fadeIn(animationSpec = tween(animationDuration))
            }
        ) {
            ReminderListScreen(
                onAddReminder = { navController.navigate("create_reminder") },
                onItemClick = { reminderId ->
                    navController.navigate("view_reminder/$reminderId")
                },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        dialog(
            "create_reminder",
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            ReminderDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "view_reminder/{reminderId}",
            arguments = listOf(navArgument("reminderId") { type = NavType.IntType }),
            enterTransition = {
                scaleIn(animationSpec = tween(animationDuration), initialScale = 0.9f) +
                    fadeIn(animationSpec = tween(animationDuration))
            },
            popExitTransition = {
                scaleOut(animationSpec = tween(animationDuration), targetScale = 0.9f) +
                    fadeOut(animationSpec = tween(animationDuration))
            }
        ) {
            ViewReminderScreen(
                onBack = { navController.popBackStack() },
                onEditClick = { reminderId ->
                    navController.navigate("edit_reminder/$reminderId")
                }
            )
        }
        dialog(
            route = "edit_reminder/{reminderId}",
            arguments = listOf(navArgument("reminderId") { type = NavType.IntType }),
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            ReminderDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            "settings",
            enterTransition = {
                scaleIn(animationSpec = tween(animationDuration), initialScale = 0.9f) +
                    fadeIn(animationSpec = tween(animationDuration))
            },
            popExitTransition = {
                scaleOut(animationSpec = tween(animationDuration), targetScale = 0.9f) +
                    fadeOut(animationSpec = tween(animationDuration))
            }
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
