package com.boulangerie.pro.domain.model

data class DashboardStats(
    val stockValue: Double,
    val outOfStockCount: Int,
    val lowStockCount: Int,
    val todayRevenue: Double,
)
