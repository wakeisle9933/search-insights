package com.si.main.searchinsights

import com.si.main.searchinsights.enum.ReportFrequency
import com.si.main.searchinsights.extension.logger
import com.si.main.searchinsights.service.DatePersistenceService
import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import com.si.main.searchinsights.util.DateUtils
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootApplication
class SearchInsightsApplication(
    private val searchConsoleService: SearchConsoleService,
    private val mailService: MailService,
    private val datePersistenceService: DatePersistenceService,
    private val context: ApplicationContext
) : CommandLineRunner {

    private val logger = logger()

    override fun run(vararg args: String?) {
        val today = LocalDate.now()
        val lastRunDate = datePersistenceService.readLastRunDate()

        if (lastRunDate != today) {
            val excelFile = searchConsoleService.createExcelFile(searchConsoleService.fetchSearchAnalyticsData())
            mailService.sendMail(excelFile, "daily_search_insights.xlsx", ReportFrequency.DAILY)

            // Weekly Report
            if(today.dayOfWeek == DayOfWeek.WEDNESDAY) {
                val excelFile = searchConsoleService.createExcelFile(searchConsoleService.fetchSearchAnalyticsData(
                    DateUtils.getFormattedDateBeforeDays(10), DateUtils.getFormattedDateBeforeDays(7) ))
                mailService.sendMail(excelFile, "weekly_search_insights.xlsx", ReportFrequency.WEEKLY)
            }

            // Monthly Report
            if(today.dayOfMonth == 3) {
                val excelFile = searchConsoleService.createExcelFile(searchConsoleService.fetchSearchAnalyticsData(
                    DateUtils.getFirstDayOfCurrentMonth(), DateUtils.getLastDayOfCurrentMonth()))
                mailService.sendMail(excelFile, "monthly_search_insights.xlsx", ReportFrequency.MONTHLY)
            }

            datePersistenceService.writeLastRunDate(today)
            logger.info("Complete tasks today!")
        } else {
            logger.info("Today task already completed!")
        }

        // 2시간 후 종료
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.schedule({
            logger.info("Search Insights Shut down!")
            SpringApplication.exit(context, { 0 })
            scheduler.shutdown()
        }, 2, TimeUnit.HOURS)
    }
}

    fun main(args: Array<String>) {
        runApplication<SearchInsightsApplication>(*args)
    }

