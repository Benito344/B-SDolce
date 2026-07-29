package com.boulangerie.pro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Dashboard : Screen("dashboard", "Accueil", Icons.Default.Home)
    data object Articles : Screen("articles", "Articles", Icons.Default.Inventory2)
    data object QuickSale : Screen("quick_sale", "Ventes", Icons.Default.ShoppingCart)
    data object Prediction : Screen("prediction", "Prédictions", Icons.Default.AutoAwesome)
    data object SalesRecap : Screen("sales_recap", "Historique", Icons.Default.CalendarMonth)
    data object Settings : Screen("settings", "Réglages", Icons.Default.Settings)

    // Sub-screens
    data object ArticleAdd : Screen("article_add", "Nouvel article")
    data object ArticleEdit : Screen("article_edit/{articleId}", "Modifier") {
        fun route(id: Long) = "article_edit/$id"
    }
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Articles,
    Screen.QuickSale,
    Screen.Prediction,
    Screen.SalesRecap,
    Screen.Settings,
)
