package com.si.main.searchinsights.controller

import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchConsoleController(
    private val mailService: MailService,
    private val searchConsoleService: SearchConsoleService,

    ) {

    @GetMapping("/test")
    fun sendDailySearchAnalytics() {
        searchConsoleService.fetchSearchAnalyticsData()
    }

    @GetMapping("/email-search-insights-report")
    fun sendDailyMailing(
        @RequestParam(required = false) fromDate: String? = null,
        @RequestParam(required = false) toDate: String? = null
    ) {
        val excelFile = if (fromDate != null && toDate != null) {
            searchConsoleService.createExcelFile(
                searchConsoleService.fetchSearchAnalyticsData(fromDate, toDate)
            )
        } else {
            searchConsoleService.createExcelFile(
                searchConsoleService.fetchSearchAnalyticsData()
            )
        }
        mailService.sendMail(excelFile, "search_insights.xlsx")
    }

}

