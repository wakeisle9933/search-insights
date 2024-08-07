package com.si.main.searchinsights.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {

    private val shortDateFormatter = DateTimeFormatter.ofPattern("MM-dd")
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getFormattedCurrentDate(): String {
        val date = LocalDate.now()
        return date.format(formatter)
    }

    fun getFormattedDateBeforeDays(days: Long): String {
        val date = LocalDate.now().minusDays(days)
        return date.format(formatter)
    }

    fun getFirstDayOfPreviousMonth(): String {
        return LocalDate.now().minusMonths(1).withDayOfMonth(1).format(formatter)
    }

    fun getLastDayOfPreviousMonth(): String {
        return LocalDate.now().minusMonths(1).withDayOfMonth(LocalDate.now().lengthOfMonth()).format(formatter)
    }

}