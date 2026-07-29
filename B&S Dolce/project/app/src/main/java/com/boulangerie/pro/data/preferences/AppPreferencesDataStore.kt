package com.boulangerie.pro.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("boulangerie_prefs")

data class AppPreferences(
    val city: String = "Paris",
    val cityLat: Float = 48.8566f,
    val cityLon: Float = 2.3522f,
    val lowStockAlertEnabled: Boolean = true,
    val productionReminderHour: Int = 18,
    val darkThemeForced: Boolean = false,
    val exportEmail: String = "",
    val defaultUnit: String = "pièce",
    val salesHistoryDays: Int = 30,
)

@Singleton
class AppPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore

    companion object {
        val CITY = stringPreferencesKey("city")
        val CITY_LAT = floatPreferencesKey("city_lat")
        val CITY_LON = floatPreferencesKey("city_lon")
        val LOW_STOCK_ALERT = booleanPreferencesKey("low_stock_alert")
        val EXPORT_EMAIL = stringPreferencesKey("export_email")
        val DEFAULT_UNIT = stringPreferencesKey("default_unit")
        val HISTORY_DAYS = stringPreferencesKey("history_days")
    }

    val preferencesFlow: Flow<AppPreferences> = store.data.map { prefs ->
        AppPreferences(
            city = prefs[CITY] ?: "Paris",
            cityLat = prefs[CITY_LAT] ?: 48.8566f,
            cityLon = prefs[CITY_LON] ?: 2.3522f,
            lowStockAlertEnabled = prefs[LOW_STOCK_ALERT] ?: true,
            exportEmail = prefs[EXPORT_EMAIL] ?: "",
            defaultUnit = prefs[DEFAULT_UNIT] ?: "pièce",
            salesHistoryDays = prefs[HISTORY_DAYS]?.toIntOrNull() ?: 30,
        )
    }

    suspend fun setCity(city: String, lat: Float, lon: Float) = store.edit {
        it[CITY] = city; it[CITY_LAT] = lat; it[CITY_LON] = lon
    }
    suspend fun setLowStockAlert(enabled: Boolean) = store.edit { it[LOW_STOCK_ALERT] = enabled }
    suspend fun setExportEmail(email: String) = store.edit { it[EXPORT_EMAIL] = email }
    suspend fun setDefaultUnit(unit: String) = store.edit { it[DEFAULT_UNIT] = unit }
    suspend fun setHistoryDays(days: Int) = store.edit { it[HISTORY_DAYS] = days.toString() }
}
