package com.manthalu.ai.navigation

sealed class NavRoutes(val route: String) {
    data object Home : NavRoutes("home")
    data object Chat : NavRoutes("chat")
    data object ChatWithId : NavRoutes("chat/{conversationId}") {
        fun create(conversationId: String) = "chat/$conversationId"
    }
    data object Study : NavRoutes("study")
    data object Developer : NavRoutes("developer")
    data object Reflection : NavRoutes("reflection")
    data object Knowledge : NavRoutes("knowledge")
    data object Tasks : NavRoutes("tasks")
    data object Memory : NavRoutes("memory")
    data object History : NavRoutes("history")
    data object Settings : NavRoutes("settings")
}
