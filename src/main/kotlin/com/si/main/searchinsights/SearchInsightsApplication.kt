package com.si.main.searchinsights

import com.si.main.searchinsights.service.DatePersistenceService
import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import jdk.internal.misc.VM.shutdown
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

@SpringBootApplication
class SearchInsightsApplication(
    private val searchConsoleService: SearchConsoleService,
    private val mailService: MailService,
    private val datePersistenceService: DatePersistenceService
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val today = LocalDate.now()
        val lastRunDate = datePersistenceService.readLastRunDate()

        if (lastRunDate != today) {
            val excelFile = searchConsoleService.createExcelFile(searchConsoleService.fetchSearchAnalyticsData())
            mailService.sendMail(excelFile, "search_insights.xlsx")
            datePersistenceService.writeLastRunDate(today)
            println("Complete tasks today!")
        } else {
            println("Today task already completed!")
        }

        // 2시간 후 종료
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.schedule({
            shutdown()
            scheduler.shutdown()
            exitProcess(0)
        }, 2, TimeUnit.HOURS)
    }
}

    fun main(args: Array<String>) {
        runApplication<SearchInsightsApplication>(*args)
    }

