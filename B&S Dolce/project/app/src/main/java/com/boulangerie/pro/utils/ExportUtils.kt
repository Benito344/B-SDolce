package com.boulangerie.pro.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter

object ExportUtils {

    fun exportSalesCsv(
        context: Context,
        rows: List<CsvRow>,
        fileName: String = "ventes_export.csv",
    ): Intent {
        val file = File(context.cacheDir, fileName)
        FileWriter(file).use { writer ->
            writer.appendLine("Date;Article;Catégorie;Quantité;Prix unitaire;Total")
            rows.forEach { row ->
                writer.appendLine(
                    "${row.date};${row.article};${row.category};${row.quantity};${row.unitPrice};${row.total}"
                )
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    data class CsvRow(
        val date: String,
        val article: String,
        val category: String,
        val quantity: String,
        val unitPrice: String,
        val total: String,
    )
}
