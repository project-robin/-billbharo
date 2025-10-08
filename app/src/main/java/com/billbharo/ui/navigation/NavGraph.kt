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

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NewInvoice : Screen("new_invoice")
    object Khata : Screen("khata")
    object KhataDetail : Screen("khata_detail/{customerId}") {
        fun createRoute(customerId: Long) = "khata_detail/$customerId"
    }
    object Inventory : Screen("inventory")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
}

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
