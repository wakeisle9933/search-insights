package com.si.main.searchinsights.controller

import com.si.main.searchinsights.service.SearchConsoleService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RealTimeAnalyticsController(
    private val searchConsoleService: SearchConsoleService
) {
    @GetMapping("/api/realtime-pageviews")
    fun getRealTimePageViews(): Map<String, Any> {
        return searchConsoleService.fetchRealTimeAnalyticsWithActiveUsers()
    }

    @GetMapping("/api/last30min-pageviews")
    fun getLast30MinPageViews(): Map<String, Any> {
        return searchConsoleService.fetchLast30MinAnalyticsWithActiveUsers()
    }

    @GetMapping("/api/custom-date-pageviews")
    fun getCustomDatePageViews(
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): Map<String, Any> {
        return searchConsoleService.fetchCustomDateAnalyticsWithActiveUsers(startDate, endDate)
    }
}
