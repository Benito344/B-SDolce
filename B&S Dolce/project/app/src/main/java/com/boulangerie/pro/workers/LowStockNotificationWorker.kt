package com.boulangerie.pro.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.boulangerie.pro.BoulangerieApp
import com.boulangerie.pro.R
import com.boulangerie.pro.data.repository.ArticleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class LowStockNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val articleRepository: ArticleRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val lowStockArticles = articleRepository.observeLowStock().first()
        if (lowStockArticles.isEmpty()) return Result.success()

        val names = lowStockArticles.take(3).joinToString(", ") { it.name }
        val more = if (lowStockArticles.size > 3) " et ${lowStockArticles.size - 3} autres" else ""

        val notification = NotificationCompat.Builder(applicationContext, BoulangerieApp.CHANNEL_STOCK)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Stock bas !")
            .setContentText("$names$more")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)

        return Result.success()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "low_stock_check"

        fun buildRequest(): PeriodicWorkRequest =
            PeriodicWorkRequest.Builder(LowStockNotificationWorker::class.java, 6, TimeUnit.HOURS)
                .build()
    }
}
