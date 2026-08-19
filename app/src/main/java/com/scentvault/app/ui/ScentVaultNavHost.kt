package com.scentvault.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scentvault.app.ui.detail.FragranceDetailScreen
import com.scentvault.app.ui.list.FragranceListScreen
import com.scentvault.app.ui.settings.SettingsScreen

private const val ROUTE_LIST = "list"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_DETAIL = "detail?id={id}"
private const val NEW_FRAGRANCE_ID = -1L

@Composable
fun ScentVaultNavHost() {
    val navController = rememberNavController()
    val viewModel: FragranceViewModel = viewModel()

    NavHost(navController = navController, startDestination = ROUTE_LIST) {
        composable(ROUTE_LIST) {
            FragranceListScreen(
                viewModel = viewModel,
                onAddNew = { navController.navigate("detail?id=$NEW_FRAGRANCE_ID") },
                onOpen = { id -> navController.navigate("detail?id=$id") },
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) }
            )
        }
        composable(
            route = ROUTE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = NEW_FRAGRANCE_ID })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: NEW_FRAGRANCE_ID
            FragranceDetailScreen(
                fragranceId = id.takeIf { it != NEW_FRAGRANCE_ID },
                viewModel = viewModel,
                onDone = { navController.popBackStack() }
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
