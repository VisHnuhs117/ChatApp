package com.vishnuhs.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vishnuhs.chatapp.presentation.auth.LoginScreen
import com.vishnuhs.chatapp.presentation.auth.SignUpScreen
import com.vishnuhs.chatapp.presentation.home.HomeScreen
import com.vishnuhs.chatapp.presentation.users.UsersScreen
import com.vishnuhs.chatapp.presentation.chat.ChatScreen
import com.vishnuhs.chatapp.ui.theme.ChatAppTheme
import com.vishnuhs.chatapp.domain.model.User
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatAppTheme {
                ChatAppNavigation()
            }
        }
    }
}

@Composable
fun ChatAppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate("signup")
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToUsers = {
                    navController.navigate("users")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("users") {
            UsersScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onUserClick = { userId, userName ->
                    navController.navigate("chat/$userId/$userName")
                }
            )
        }

        composable("chat/{userId}/{userName}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val userName = backStackEntry.arguments?.getString("userName") ?: "User"

            ChatScreen(
                chatUserId = userId,
                chatUserName = userName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}