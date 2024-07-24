package com.si.main.searchinsights.service

import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

@Service
class DatePersistenceService {
    private val lastRunFile = Paths.get("last_run_date.txt")

    fun readLastRunDate(): LocalDate? {
        return if (Files.exists(lastRunFile)) {
            LocalDate.parse(Files.readString(lastRunFile))
        } else {
            null
        }
    }

    fun writeLastRunDate(date: LocalDate) {
        Files.writeString(lastRunFile, date.toString())
    }
}