package com.vishnuhs.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vishnuhs.chatapp.presentation.auth.AuthViewModel
import com.vishnuhs.chatapp.presentation.auth.LoginScreen
import com.vishnuhs.chatapp.presentation.auth.SignUpScreen
import com.vishnuhs.chatapp.presentation.home.HomeScreen
import com.vishnuhs.chatapp.presentation.users.UsersScreen
import com.vishnuhs.chatapp.presentation.chat.ChatScreen
import com.vishnuhs.chatapp.ui.theme.ChatAppTheme
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
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    // Automatically navigate based on auth state
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

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
                },
                viewModel = authViewModel
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
                },
                viewModel = authViewModel
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
                },
                viewModel = authViewModel
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