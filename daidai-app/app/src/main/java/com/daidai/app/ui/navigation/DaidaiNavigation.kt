package com.daidai.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daidai.app.ui.screen.login.LoginScreen
import com.daidai.app.ui.screen.home.HomeScreen
import com.daidai.app.ui.screen.webhelper.WebHelperScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object WebHelper : Screen("webhelper")
}

@Composable
fun DaidaiNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToWebHelper = {
                    navController.navigate(Screen.WebHelper.route)
                }
            )
        }
        composable(Screen.WebHelper.route) {
            WebHelperScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
