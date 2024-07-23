package com.si.main.searchinsights.controller

import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchConsoleController (
    private val mailService: MailService,
    private val searchConsoleService: SearchConsoleService,

) {
    @GetMapping("/test")
    fun sendDailySearchAnalytics() {
        searchConsoleService.fetchSearchAnalyticsData()
    }

    @GetMapping("/mail-sending-test")
    fun sendDailyMailing() {
        val excelFile =
            searchConsoleService.createExcelFile(searchConsoleService.fetchSearchAnalyticsData())
        mailService.sendMail(excelFile, "search_insights.xlsx")
    }

}

