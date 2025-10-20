package com.example.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
                RemindersApp()
            }
        }
    }
}

@Composable
fun RemindersApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "reminders") {
        composable("reminders") {
            ReminderListScreen(
                onAddReminder = { navController.navigate("create_reminder") },
                onItemClick = { reminderId ->
                    navController.navigate("view_reminder/$reminderId")
                },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("create_reminder") {
            ReminderDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "view_reminder/{reminderId}",
            arguments = listOf(navArgument("reminderId") { type = NavType.IntType })
        ) { 
            ViewReminderScreen(
                onBack = { navController.popBackStack() },
                onEditClick = { reminderId ->
                    navController.navigate("edit_reminder/$reminderId")
                }
            )
        }
        composable(
            route = "edit_reminder/{reminderId}",
            arguments = listOf(navArgument("reminderId") { type = NavType.IntType })
        ) {
            ReminderDetailScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
