package com.vishnuhs.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vishnuhs.chatapp.presentation.auth.LoginScreen
import com.vishnuhs.chatapp.presentation.auth.SignUpScreen
import com.vishnuhs.chatapp.presentation.home.HomeScreen
import com.vishnuhs.chatapp.presentation.users.UsersScreen
import com.vishnuhs.chatapp.presentation.chat.ChatScreen
import com.vishnuhs.chatapp.ui.theme.ChatAppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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
                    // Direct logout and navigation
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
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
                    try {
                        println("DEBUG: Navigating to chat with user: $userName (ID: $userId)")

                        // URL encode the parameters to handle special characters like @
                        val encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8.toString())
                        val encodedUserName = URLEncoder.encode(userName, StandardCharsets.UTF_8.toString())

                        navController.navigate("chat/$encodedUserId/$encodedUserName")

                    } catch (e: Exception) {
                        println("DEBUG: Navigation error: ${e.message}")
                        e.printStackTrace()
                    }
                }
            )
        }

        composable("chat/{userId}/{userName}") { backStackEntry ->
            val encodedUserId = backStackEntry.arguments?.getString("userId") ?: ""
            val encodedUserName = backStackEntry.arguments?.getString("userName") ?: "User"

            // URL decode the parameters
            val userId = try {
                URLDecoder.decode(encodedUserId, StandardCharsets.UTF_8.toString())
            } catch (e: Exception) {
                encodedUserId
            }

            val userName = try {
                URLDecoder.decode(encodedUserName, StandardCharsets.UTF_8.toString())
            } catch (e: Exception) {
                encodedUserName
            }

            println("DEBUG: Opening ChatScreen with user: $userName (ID: $userId)")

            ChatScreen(
                chatUserId = userId,
                chatUserName = userName,
                onNavigateBack = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        println("DEBUG: Back navigation error: ${e.message}")
                    }
                }
            )
        }
    }
}