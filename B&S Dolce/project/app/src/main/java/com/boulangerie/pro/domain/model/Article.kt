package com.boulangerie.pro.domain.model

import com.boulangerie.pro.data.local.entity.ArticleEntity

val CATEGORIES = listOf("Pain", "Viennoiserie", "Pâtisserie", "Sandwich", "Boisson", "Autre")
val UNITS = listOf("pièce", "kg", "g", "L", "tranche", "sachet")

fun ArticleEntity.isLowStock(): Boolean = quantityInStock <= lowStockThreshold
fun ArticleEntity.isOutOfStock(): Boolean = quantityInStock <= 0.0
