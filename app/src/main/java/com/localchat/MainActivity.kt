package com.localchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localchat.data.repository.UserPreferencesRepository
import com.localchat.ui.screens.ChatScreen
import com.localchat.ui.screens.HostSetupScreen
import com.localchat.ui.screens.JoinScreen
import com.localchat.ui.screens.UserSetupScreen
import com.localchat.ui.screens.WelcomeScreen
import com.localchat.ui.theme.LocalChatTheme
import com.localchat.viewmodels.ChatViewModel
import com.localchat.viewmodels.ServerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocalChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatAppNavigation()
                }
            }
        }
    }
}

@Composable
fun ChatAppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Create repositories and view models
    val userPreferencesRepository = remember { UserPreferencesRepository(context) }
    val serverViewModel = remember { ServerViewModel(context) }
    val chatViewModel = remember { ChatViewModel(context) }

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        // Welcome screen
        composable("welcome") {
            WelcomeScreen(
                onHostClick = {
                    navController.navigate("user_setup/host")
                },
                onJoinClick = {
                    navController.navigate("user_setup/join")
                }
            )
        }

        // User setup screen
        composable(
            route = "user_setup/{mode}",
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "join"

            UserSetupScreen(
                mode = mode,
                onContinue = { username, avatarColor ->
                    if (mode == "host") {
                        navController.navigate("host_setup/$username/$avatarColor")
                    } else {
                        navController.navigate("join/$username/$avatarColor")
                    }
                },
                userPreferencesRepository = userPreferencesRepository
            )
        }

        // Host setup screen
        composable(
            route = "host_setup/{username}/{avatarColor}",
            arguments = listOf(
                navArgument("username") { type = NavType.StringType },
                navArgument("avatarColor") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val avatarColor = backStackEntry.arguments?.getString("avatarColor") ?: "#2196F3"

            HostSetupScreen(
                username = username,
                avatarColor = avatarColor,
                serverViewModel = serverViewModel,
                onEnterChat = {
                    navController.navigate("chat/host/$username/$avatarColor/localhost")
                }
            )
        }

        // Join screen
        composable(
            route = "join/{username}/{avatarColor}",
            arguments = listOf(
                navArgument("username") { type = NavType.StringType },
                navArgument("avatarColor") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val avatarColor = backStackEntry.arguments?.getString("avatarColor") ?: "#2196F3"

            JoinScreen(
                username = username,
                avatarColor = avatarColor,
                chatViewModel = chatViewModel,
                userPreferencesRepository = userPreferencesRepository,
                onConnected = {
                    // Get server address from ViewModel or shared state
                    navController.navigate("chat/client/$username/$avatarColor/connected") {
                        popUpTo("welcome") { inclusive = false }
                    }
                }
            )
        }

        // Chat screen
        composable(
            route = "chat/{mode}/{username}/{avatarColor}/{serverAddress}",
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("username") { type = NavType.StringType },
                navArgument("avatarColor") { type = NavType.StringType },
                navArgument("serverAddress") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "client"
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val avatarColor = backStackEntry.arguments?.getString("avatarColor") ?: "#2196F3"
            val serverAddress = backStackEntry.arguments?.getString("serverAddress") ?: "127.0.0.1:8080"

            ChatScreen(
                mode = mode,
                username = username,
                avatarColor = avatarColor,
                serverAddress = serverAddress,
                chatViewModel = chatViewModel,
                onExit = {
                    navController.navigate("welcome") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
    }
}
