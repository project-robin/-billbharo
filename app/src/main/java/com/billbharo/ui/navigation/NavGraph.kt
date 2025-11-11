package com.billbharo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.billbharo.ui.screens.home.HomeScreen
import com.billbharo.ui.screens.inventory.InventoryScreen
import com.billbharo.ui.screens.khata.KhataScreen
import com.billbharo.ui.screens.newinvoice.NewInvoiceScreen
import com.billbharo.ui.screens.reports.ReportsScreen
import com.billbharo.ui.screens.settings.SettingsScreen

/**
 * Defines the sealed class for screen routes in the application.
 *
 * Each object represents a screen and contains its unique route string. This provides a
 * type-safe way to handle navigation.
 *
 * @param route The navigation route string for the screen.
 */
sealed class Screen(val route: String) {
    /** Represents the Home screen. */
    object Home : Screen("home")

    /** Represents the New Invoice screen. */
    object NewInvoice : Screen("new_invoice")

    /** Represents the Khata (customer credit) screen. */
    object Khata : Screen("khata")

    /**
     * Represents the Khata Detail screen, which requires a customer ID.
     * @param createRoute A function to generate the route with a specific customer ID.
     */
    object KhataDetail : Screen("khata_detail/{customerId}") {
        fun createRoute(customerId: Long) = "khata_detail/$customerId"
    }

    /** Represents the Inventory screen. */
    object Inventory : Screen("inventory")

    /** Represents the Reports screen. */
    object Reports : Screen("reports")

    /** Represents the Settings screen. */
    object Settings : Screen("settings")
}

/**
 * Sets up the navigation graph for the application.
 *
 * This composable function defines the navigation flow between different screens using
 * Jetpack Navigation Compose.
 *
 * @param navController The [NavHostController] that manages navigation.
 * @param startDestination The route of the screen to be displayed first.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.NewInvoice.route) {
            NewInvoiceScreen(navController = navController)
        }

        composable(Screen.Khata.route) {
            KhataScreen(navController = navController)
        }

        composable(Screen.Inventory.route) {
            InventoryScreen(navController = navController)
        }

        composable(Screen.Reports.route) {
            ReportsScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}
