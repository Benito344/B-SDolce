package com.boulangerie.pro.utils

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val zone: ZoneId = ZoneId.systemDefault()

    fun startOfDay(epochMillis: Long): Long {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun dayBounds(epochDay: Long): Pair<Long, Long> {
        val date = LocalDate.ofEpochDay(epochDay)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun millisToEpochDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun formatDate(epochDay: Long, pattern: String = "dd MMMM yyyy"): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofPattern(pattern, Locale.FRENCH))
    }

    fun formatDateTime(millis: Long): String {
        val dt = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH))
    }

    fun daysAgo(days: Long): Long {
        return LocalDate.now().minusDays(days).toEpochDay()
    }

    fun dayOfWeekFr(epochDay: Long): String {
        return when (LocalDate.ofEpochDay(epochDay).dayOfWeek) {
            DayOfWeek.MONDAY -> "Lundi"
            DayOfWeek.TUESDAY -> "Mardi"
            DayOfWeek.WEDNESDAY -> "Mercredi"
            DayOfWeek.THURSDAY -> "Jeudi"
            DayOfWeek.FRIDAY -> "Vendredi"
            DayOfWeek.SATURDAY -> "Samedi"
            DayOfWeek.SUNDAY -> "Dimanche"
        }
    }
}

object FormatUtils {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.FRANCE)
    private val numberFormat = NumberFormat.getNumberInstance(Locale.FRENCH).apply {
        maximumFractionDigits = 2
    }

    fun currency(amount: Double): String = currencyFormat.format(amount)
    fun number(value: Double): String = numberFormat.format(value)
}
