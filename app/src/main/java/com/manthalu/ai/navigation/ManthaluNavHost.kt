package com.manthalu.ai.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.manthalu.ai.features.chat.ChatScreen
import com.manthalu.ai.features.history.HistoryScreen
import com.manthalu.ai.features.home.HomeScreen
import com.manthalu.ai.features.knowledge.KnowledgeScreen
import com.manthalu.ai.features.memory.MemoryScreen
import com.manthalu.ai.features.settings.SettingsScreen
import com.manthalu.ai.features.tasks.TasksScreen

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(NavRoutes.Chat.route, "Chat", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
)

@Composable
fun ManthaluNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val keyboardVisible = WindowInsets.isImeVisible
    val showBottomBar = !keyboardVisible && (
        bottomNavItems.any { item ->
            currentDestination?.hierarchy?.any { it.route == item.route } == true
        } || currentDestination?.route?.startsWith("chat/") == true
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.Home.route) {
                HomeScreen(
                    onNavigateToChat = { navController.navigate(NavRoutes.Chat.route) },
                    onNavigateToMemory = { navController.navigate(NavRoutes.Memory.route) },
                    onNavigateToTasks = { navController.navigate(NavRoutes.Tasks.route) },
                    onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
                    onNavigateToKnowledge = { navController.navigate(NavRoutes.Knowledge.route) },
                    onNavigateToHistory = { navController.navigate(NavRoutes.History.route) }
                )
            }
            composable(NavRoutes.Chat.route) {
                ChatScreen(onOpenConversation = { id -> navController.navigate(NavRoutes.ChatWithId.create(id)) })
            }
            composable(
                route = NavRoutes.ChatWithId.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { backStack ->
                ChatScreen(
                    conversationIdArg = backStack.arguments?.getString("conversationId"),
                    onOpenConversation = { id -> navController.navigate(NavRoutes.ChatWithId.create(id)) }
                )
            }
            composable(NavRoutes.Knowledge.route) { KnowledgeScreen() }
            composable(NavRoutes.Tasks.route) { TasksScreen() }
            composable(NavRoutes.Memory.route) { MemoryScreen() }
            composable(NavRoutes.History.route) {
                HistoryScreen(onOpenConversation = { id ->
                    navController.navigate(NavRoutes.ChatWithId.create(id))
                })
            }
            composable(NavRoutes.Settings.route) { SettingsScreen() }
        }
    }
}
