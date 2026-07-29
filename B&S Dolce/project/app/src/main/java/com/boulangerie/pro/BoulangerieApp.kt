package com.boulangerie.pro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BoulangerieApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val stockChannel = NotificationChannel(
                CHANNEL_STOCK,
                "Alertes stock",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications de stock bas" }
            manager.createNotificationChannel(stockChannel)

            val prodChannel = NotificationChannel(
                CHANNEL_PRODUCTION,
                "Rappels production",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Rappels de production du lendemain" }
            manager.createNotificationChannel(prodChannel)
        }
    }

    companion object {
        const val CHANNEL_STOCK = "stock_alerts"
        const val CHANNEL_PRODUCTION = "production_reminders"
    }
}
