package com.si.main.searchinsights

import com.si.main.searchinsights.enum.ReportFrequency
import com.si.main.searchinsights.extension.logger
import com.si.main.searchinsights.service.DatePersistenceService
import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import com.si.main.searchinsights.util.DateUtils
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.*

@SpringBootApplication
class SearchInsightsApplication(
    private val searchConsoleService: SearchConsoleService,
    private val mailService: MailService,
    private val datePersistenceService: DatePersistenceService,
    private val context: ApplicationContext
) : CommandLineRunner {

    private val logger = logger()

    override fun run(vararg args: String?) {
        val seoulZone = ZoneId.of("Asia/Seoul")
        val today = ZonedDateTime.now(seoulZone).toLocalDate()
        val lastRunDate = datePersistenceService.readLastRunDate()

        if (lastRunDate != today) {
            // 🚀 일간 리포트 - 병렬로 데이터 가져오기!
            runBlocking {
                val (searchData, analyticsData) = coroutineScope {
                    val searchDeferred = async(Dispatchers.IO) { 
                        searchConsoleService.fetchSearchAnalyticsData() 
                    }
                    val analyticsDeferred = async(Dispatchers.IO) { 
                        searchConsoleService.fetchAnalyticsData() 
                    }
                    Pair(searchDeferred.await(), analyticsDeferred.await())
                }
                
                // 🔍 데이터 체크 - 둘 다 비어있으면 이메일 발송 안 함!
                if (searchData.isEmpty() && analyticsData.isEmpty()) {
                    logger.warn("📭 일간 리포트 데이터가 없어서 이메일 발송을 건너뜁니다.")
                } else {
                    val excelFile = searchConsoleService.createExcelFile(searchData, analyticsData, ReportFrequency.DAILY)
                    mailService.sendMail(excelFile, "daily_search_insights.xlsx", ReportFrequency.DAILY)
                    logger.info("✅ 일간 이메일 발송 완료! Search Console: ${searchData.size}개, Analytics: ${analyticsData.size}개")
                }
            }

            // 🚀 주간 리포트 - 병렬 처리!
            if(today.dayOfWeek == DayOfWeek.WEDNESDAY) {
                runBlocking {
                    val (searchData, analyticsData) = coroutineScope {
                        val searchDeferred = async(Dispatchers.IO) {
                            searchConsoleService.fetchSearchAnalyticsData(
                                DateUtils.getFormattedDateBeforeDays(10),
                                DateUtils.getFormattedDateBeforeDays(3)
                            )
                        }
                        val analyticsDeferred = async(Dispatchers.IO) {
                            searchConsoleService.fetchAnalyticsData(
                                DateUtils.getFormattedDateBeforeDays(10),
                                DateUtils.getFormattedDateBeforeDays(3)
                            )
                        }
                        Pair(searchDeferred.await(), analyticsDeferred.await())
                    }
                    
                    // 🔍 데이터 체크
                    if (searchData.isEmpty() && analyticsData.isEmpty()) {
                        logger.warn("💭 주간 리포트 데이터가 없어서 이메일 발송을 건너뜁니다.")
                    } else {
                        val excelFile = searchConsoleService.createExcelFile(searchData, analyticsData, ReportFrequency.WEEKLY)
                        mailService.sendMail(excelFile, "weekly_search_insights.xlsx", ReportFrequency.WEEKLY)
                        logger.info("✅ 주간 이메일 발송 완료! Search Console: ${searchData.size}개, Analytics: ${analyticsData.size}개")
                    }
                }
            }

            // 🚀 월간 리포트 - 병렬 처리!
            if(today.dayOfMonth == 3) {
                runBlocking {
                    val (searchData, analyticsData) = coroutineScope {
                        val searchDeferred = async(Dispatchers.IO) {
                            searchConsoleService.fetchSearchAnalyticsData(
                                DateUtils.getFirstDayOfPreviousMonth(),
                                DateUtils.getLastDayOfPreviousMonth()
                            )
                        }
                        val analyticsDeferred = async(Dispatchers.IO) {
                            searchConsoleService.fetchAnalyticsData(
                                DateUtils.getFirstDayOfPreviousMonth(),
                                DateUtils.getLastDayOfPreviousMonth()
                            )
                        }
                        Pair(searchDeferred.await(), analyticsDeferred.await())
                    }
                    
                    // 🔍 데이터 체크
                    if (searchData.isEmpty() && analyticsData.isEmpty()) {
                        logger.warn("💭 월간 리포트 데이터가 없어서 이메일 발송을 건너뜁니다.")
                    } else {
                        val excelFile = searchConsoleService.createExcelFile(searchData, analyticsData, ReportFrequency.MONTHLY)
                        mailService.sendMail(excelFile, "monthly_search_insights.xlsx", ReportFrequency.MONTHLY)
                        logger.info("✅ 월간 이메일 발송 을 완료! Search Console: ${searchData.size}개, Analytics: ${analyticsData.size}개")
                    }
                }
            }

            datePersistenceService.writeLastRunDate(today)
            logger.info("Complete tasks today!")
        } else {
            logger.info("Today task already completed!")
        }

        // 2시간 후 종료
//        val scheduler = Executors.newSingleThreadScheduledExecutor()
//        scheduler.schedule({
//            logger.info("Search Insights Shut down!")
//            SpringApplication.exit(context, { 0 })
//            scheduler.shutdown()
//        }, 2, TimeUnit.HOURS)
    }
}

    fun main(args: Array<String>) {
        runApplication<SearchInsightsApplication>(*args)
    }

