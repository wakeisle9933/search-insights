package com.si.main.searchinsights.util

import com.si.main.searchinsights.extension.logger
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {

    private val shortDateFormatter = DateTimeFormatter.ofPattern("MM-dd")
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val logger = logger()

    fun convertToLocalDateString(dateString: String?): String {
        return try {
            val date = LocalDate.parse(dateString)
            date.toString()
        } catch (e: Exception) {
            logger.error("Check Input Data! - Input data : $dateString", e)
            throw IllegalArgumentException("Invalid date formats(Example:2024-08-13) Parameter: $dateString")
        }
    }

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