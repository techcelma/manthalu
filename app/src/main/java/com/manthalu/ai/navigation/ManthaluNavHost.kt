package com.manthalu.ai.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.manthalu.ai.features.chat.ChatScreen
import com.manthalu.ai.features.developer.DeveloperScreen
import com.manthalu.ai.features.history.HistoryScreen
import com.manthalu.ai.features.home.HomeScreen
import com.manthalu.ai.features.knowledge.KnowledgeScreen
import com.manthalu.ai.features.memory.MemoryScreen
import com.manthalu.ai.features.reflection.ReflectionScreen
import com.manthalu.ai.features.settings.SettingsScreen
import com.manthalu.ai.features.study.StudyScreen
import com.manthalu.ai.features.tasks.TasksScreen

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(NavRoutes.Chat.route, "Chat", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    BottomNavItem(NavRoutes.Study.route, "Study", Icons.Filled.Book, Icons.Outlined.Book),
    BottomNavItem(NavRoutes.Developer.route, "Dev", Icons.Filled.Code, Icons.Outlined.Code),
    BottomNavItem(NavRoutes.Reflection.route, "Reflect", Icons.Filled.SelfImprovement, Icons.Outlined.SelfImprovement)
)

@Composable
fun ManthaluNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    } || currentDestination?.route?.startsWith("chat/") == true

    Scaffold(
        bottomBar = {
            if (showBottomBar || currentDestination?.route == NavRoutes.Chat.route) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
                ChatScreen()
            }
            composable(
                route = NavRoutes.ChatWithId.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { backStack ->
                val id = backStack.arguments?.getString("conversationId")
                ChatScreen(conversationIdArg = id)
            }
            composable(NavRoutes.Study.route) { StudyScreen(onStartChat = { navController.navigate(NavRoutes.Chat.route) }) }
            composable(NavRoutes.Developer.route) { DeveloperScreen(onStartChat = { navController.navigate(NavRoutes.Chat.route) }) }
            composable(NavRoutes.Reflection.route) { ReflectionScreen(onStartChat = { navController.navigate(NavRoutes.Chat.route) }) }
            composable(NavRoutes.Knowledge.route) { KnowledgeScreen() }
            composable(NavRoutes.Tasks.route) { TasksScreen() }
            composable(NavRoutes.Memory.route) { MemoryScreen() }
            composable(NavRoutes.History.route) {
                HistoryScreen(
                    onOpenConversation = { id ->
                        navController.navigate(NavRoutes.ChatWithId.create(id))
                    }
                )
            }
            composable(NavRoutes.Settings.route) { SettingsScreen() }
        }
    }
}
