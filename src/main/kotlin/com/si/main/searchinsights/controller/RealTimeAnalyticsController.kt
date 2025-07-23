package com.si.main.searchinsights.controller

import com.si.main.searchinsights.service.SearchConsoleService
import com.si.main.searchinsights.service.WordPressCategoryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RealTimeAnalyticsController(
    private val searchConsoleService: SearchConsoleService,
    private val wordPressCategoryService: WordPressCategoryService
) {
    @GetMapping("/api/realtime-pageviews")
    fun getRealTimePageViews(): Map<String, Any> {
        val data = searchConsoleService.fetchRealTimeAnalyticsWithActiveUsers()
        val pageViews = data["pageViews"] as List<com.si.main.searchinsights.data.PageViewInfo>

        // 카테고리별 페이지뷰 가져오기
        val categoryViews = wordPressCategoryService.getCategoryPageViews(pageViews)

        // 결과에 categoryViews 추가
        return data + mapOf("categoryViews" to categoryViews)
    }

    @GetMapping("/api/last30min-pageviews")
    fun getLast30MinPageViews(): Map<String, Any> {
        val data = searchConsoleService.fetchLast30MinAnalyticsWithActiveUsers()
        val pageViews = data["pageViews"] as List<com.si.main.searchinsights.data.PageViewInfo>

        // 카테고리별 페이지뷰 가져오기
        val categoryViews = wordPressCategoryService.getCategoryPageViews(pageViews)

        // 결과에 categoryViews 추가
        return data + mapOf("categoryViews" to categoryViews)
    }

    @GetMapping("/api/custom-date-pageviews")
    fun getCustomDatePageViews(
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): Map<String, Any> {
        val data = searchConsoleService.fetchCustomDateAnalyticsWithActiveUsers(startDate, endDate)
        val pageViews = data["pageViews"] as List<com.si.main.searchinsights.data.PageViewInfo>

        // 카테고리별 페이지뷰 가져오기
        val categoryViews = wordPressCategoryService.getCategoryPageViews(pageViews)

        // 결과에 categoryViews 추가
        return data + mapOf("categoryViews" to categoryViews)
    }

    @GetMapping("/api/sync-wordpress-categories")
    fun syncWordPressCategories(@RequestParam forceFullSync: Boolean = false): Map<String, String> {
        wordPressCategoryService.fullSync(forceFullSync)
        return mapOf("status" to "동기화 완료!")
    }
    
    @GetMapping("/api/hourly-heatmap")
    fun getHourlyHeatmap(
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): Map<String, Any> {
        return searchConsoleService.fetchHourlyHeatmapData(startDate, endDate)
    }
}