package com.si.main.searchinsights.controller

import com.si.main.searchinsights.enum.ReportFrequency
import com.si.main.searchinsights.service.GoogleTrendsService
import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchConsoleController(
    private val mailService: MailService,
    private val searchConsoleService: SearchConsoleService,
    private val googleTrendsService: GoogleTrendsService,
    ) {

    @GetMapping("/email-search-insights-report")
    fun sendDailyMailing(
        @RequestParam(required = false) fromDate: String? = null,
        @RequestParam(required = false) toDate: String? = null
    ) {
        val excelFile = if (fromDate != null && toDate != null) {
            searchConsoleService.createExcelFile(
                searchConsoleService.fetchSearchAnalyticsData(fromDate, toDate), searchConsoleService.fetchAnalyticsData(fromDate, toDate), ReportFrequency.CUSTOM
            )
        } else {
            searchConsoleService.createExcelFile(
                searchConsoleService.fetchSearchAnalyticsData(), searchConsoleService.fetchAnalyticsData(fromDate, toDate), ReportFrequency.CUSTOM
            )
        }
        mailService.sendMail(excelFile, "search_insights.xlsx", ReportFrequency.CUSTOM, fromDate, toDate)
    }

    @GetMapping("/related-queries")
    fun getRelatedQueries(@RequestParam keyword: String): ResponseEntity<Map<String, Any>> {
        val queries = googleTrendsService.fetchRelatedQueries(keyword)
        return ResponseEntity.ok(queries)
    }

    @GetMapping("/related-topics")
    fun getRelatedTopics(@RequestParam keyword: String): ResponseEntity<Map<String, Any>> {
        val topics = googleTrendsService.fetchRelatedTopics(keyword)
        return ResponseEntity.ok(topics)
    }

    @GetMapping("/suggestions")
    fun getSuggestions(@RequestParam keyword: String): ResponseEntity<List<Map<String, String>>> {
        val suggestions = googleTrendsService.fetchSuggestions(keyword)
        return ResponseEntity.ok(suggestions)
    }

}

