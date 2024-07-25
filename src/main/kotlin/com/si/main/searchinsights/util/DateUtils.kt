package com.si.main.searchinsights.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getFormattedDateBeforeDays(days: Long): String {
        val date = LocalDate.now().minusDays(days)
        return date.format(formatter)
    }

    fun getFirstDayOfCurrentMonth(): String {
        return LocalDate.now().withDayOfMonth(1).format(formatter)
    }

    fun getLastDayOfCurrentMonth(): String {
        return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).format(formatter)
    }

}