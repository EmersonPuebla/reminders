package com.example.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.reminders.ui.screens.* 
import com.example.reminders.ui.theme.RemindersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeState = remember { mutableStateOf(Theme.SYSTEM) }
            val darkTheme = when (themeState.value) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                Theme.SYSTEM -> isSystemInDarkTheme()
            }

            RemindersTheme(darkTheme = darkTheme) {
                RemindersApp(themeState = themeState)
            }
        }
    }
}

@Composable
fun RemindersApp(themeState: MutableState<Theme>) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "reminders") {
        composable("reminders") {
            ReminderListScreen(
                onAddReminder = { navController.navigate("create_reminder") },
                onItemClick = { reminderTitle ->
                    navController.navigate("view_reminder/$reminderTitle")
                },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("create_reminder") {
            ReminderDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "view_reminder/{reminderTitle}",
            arguments = listOf(navArgument("reminderTitle") { type = NavType.StringType })
        ) { backStackEntry ->
            val reminderTitle = backStackEntry.arguments?.getString("reminderTitle") ?: ""
            ViewReminderScreen(
                reminderTitle = reminderTitle,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onThemeChange = { themeState.value = it }
            )
        }
    }
}