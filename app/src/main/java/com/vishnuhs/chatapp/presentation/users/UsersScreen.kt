package com.vishnuhs.chatapp.presentation.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class SimpleFirebaseUser(
    val id: String,
    val name: String,
    val email: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    onNavigateBack: () -> Unit,
    onUserClick: (String, String) -> Unit
) {
    var users by remember { mutableStateOf<List<SimpleFirebaseUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Load users function
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        error = null

        try {
            println("DEBUG: Starting to load users...")

            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            if (currentUser == null) {
                error = "Not logged in"
                isLoading = false
                return@LaunchedEffect
            }

            println("DEBUG: Current user: ${currentUser.email}")

            val firestore = FirebaseFirestore.getInstance()
            val result = firestore.collection("users").get().await()

            println("DEBUG: Got ${result.documents.size} documents from Firestore")

            val fetchedUsers = mutableListOf<SimpleFirebaseUser>()

            for (doc in result.documents) {
                try {
                    val userId = doc.id
                    println("DEBUG: Processing user: $userId")

                    // Skip current user
                    if (userId == currentUser.uid) {
                        println("DEBUG: Skipping current user")
                        continue
                    }

                    val name = doc.getString("displayName")
                        ?: doc.getString("name")
                        ?: doc.getString("email")?.substringBefore("@")
                        ?: "User"

                    val email = doc.getString("email") ?: "No email"

                    fetchedUsers.add(
                        SimpleFirebaseUser(
                            id = userId,
                            name = name,
                            email = email
                        )
                    )

                    println("DEBUG: Added user: $name")

                } catch (e: Exception) {
                    println("DEBUG: Error processing user ${doc.id}: ${e.message}")
                }
            }

            users = fetchedUsers
            println("DEBUG: Final users list size: ${users.size}")

        } catch (e: Exception) {
            println("DEBUG: Error loading users: ${e.message}")
            e.printStackTrace()
            error = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    val refreshUsers: () -> Unit = { refreshTrigger++ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Start New Chat",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        try {
                            println("DEBUG: Navigating back from users")
                            onNavigateBack()
                        } catch (e: Exception) {
                            println("DEBUG: Error in onNavigateBack: ${e.message}")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = refreshUsers) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF6366F1)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading users...",
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = "⚠️ Error",
                                    fontSize = 24.sp,
                                    color = Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = error!!,
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = refreshUsers,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    )
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }

                    users.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = "👥 No Users Found",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Create a second account to test real users, or ask friends to sign up!",
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = refreshUsers,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    )
                                ) {
                                    Text("Refresh")
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(users) { user ->
                                DirectUserItem(
                                    user = user,
                                    onClick = {
                                        try {
                                            println("DEBUG: User clicked: ${user.name}")
                                            // Direct navigation without dialog
                                            onUserClick(user.id, user.name)
                                        } catch (e: Exception) {
                                            println("DEBUG: Error in user click: ${e.message}")
                                            e.printStackTrace()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectUserItem(
    user: SimpleFirebaseUser,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    onClick()
                } catch (e: Exception) {
                    println("DEBUG: Error in DirectUserItem click: ${e.message}")
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture with user's initial
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.email,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Online Status (always green for now)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
            )
        }
    }
}