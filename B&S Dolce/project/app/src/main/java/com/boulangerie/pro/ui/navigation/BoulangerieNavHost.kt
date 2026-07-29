package com.boulangerie.pro.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
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
import com.boulangerie.pro.ui.articles.ArticleFormScreen
import com.boulangerie.pro.ui.articles.ArticleListScreen
import com.boulangerie.pro.ui.dashboard.DashboardScreen
import com.boulangerie.pro.ui.prediction.PredictionScreen
import com.boulangerie.pro.ui.sales.QuickSaleScreen
import com.boulangerie.pro.ui.salesrecap.SalesRecapScreen
import com.boulangerie.pro.ui.settings.SettingsScreen

@Composable
fun BoulangerieNavHost() {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDest = navBackStack?.destination

    // Show bottom nav only on top-level screens
    val showBottomBar = bottomNavItems.any { it.route == currentDest?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToSales = { navController.navigate(Screen.QuickSale.route) },
                    onNavigateToArticles = { navController.navigate(Screen.Articles.route) },
                )
            }

            composable(Screen.Articles.route) {
                ArticleListScreen(
                    onAddArticle = { navController.navigate(Screen.ArticleAdd.route) },
                    onEditArticle = { id -> navController.navigate(Screen.ArticleEdit.route(id)) },
                )
            }

            composable(Screen.ArticleAdd.route) {
                ArticleFormScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.ArticleEdit.route,
                arguments = listOf(navArgument("articleId") { type = NavType.LongType })
            ) {
                ArticleFormScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.QuickSale.route) {
                QuickSaleScreen()
            }

            composable(Screen.Prediction.route) {
                PredictionScreen()
            }

            composable(Screen.SalesRecap.route) {
                SalesRecapScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
