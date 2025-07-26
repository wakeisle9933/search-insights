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
    
    @GetMapping("/api/hourly-detail-pageviews")
    fun getHourlyDetailPageViews(
        @RequestParam date: String,
        @RequestParam hour: Int,
        @RequestParam(required = false) dayOfWeek: Int? = null
    ): Map<String, Any> {
        val data = searchConsoleService.fetchHourlyDetailPageViews(date, hour)
        val pageViews = data["pageViews"] as? List<com.si.main.searchinsights.data.PageViewInfo> ?: emptyList()
        
        // 카테고리별 페이지뷰 가져오기
        val categoryViews = wordPressCategoryService.getCategoryPageViews(pageViews)
        
        // 결과에 categoryViews 추가
        return data + mapOf("categoryViews" to categoryViews)
    }
    
    @GetMapping("/api/demographics-heatmap")
    fun getDemographicsHeatmap(
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): Map<String, Any> {
        return searchConsoleService.fetchDemographicsHeatmapData(startDate, endDate)
    }
    
    @GetMapping("/api/demographics-detail")
    fun getDemographicsDetail(
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam gender: String,
        @RequestParam ageGroup: String
    ): Map<String, Any> {
        val data = searchConsoleService.fetchDemographicsDetailData(startDate, endDate, gender, ageGroup)
        val pageViews = data["pageViews"] as? List<com.si.main.searchinsights.data.PageViewInfo> ?: emptyList()
        
        // 카테고리별 페이지뷰 가져오기
        val categoryViews = wordPressCategoryService.getCategoryPageViews(pageViews)
        
        // 결과에 categoryViews 추가
        return data + mapOf("categoryViews" to categoryViews)
    }
    
    @GetMapping("/api/traffic-by-source")
    fun getTrafficBySource(
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): Map<String, Any> {
        val trafficSources = searchConsoleService.fetchTrafficBySource(startDate, endDate)
        return mapOf(
            "trafficSources" to trafficSources,
            "totalSessions" to trafficSources.sumOf { it.sessions }
        )
    }
    
    @GetMapping("/api/pageviews-by-source")
    fun getPageViewsBySource(
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam sourceDomain: String
    ): Map<String, Any> {
        val data = searchConsoleService.fetchPageViewsBySource(startDate, endDate, sourceDomain)
        val pageViews = data["pageViews"] as? List<com.si.main.searchinsights.data.PageViewInfo> ?: emptyList()
        
        // 카테고리별 페이지뷰 가져오기
        val categoryViews = wordPressCategoryService.getCategoryPageViews(pageViews)
        
        // 결과에 categoryViews 추가
        return data + mapOf("categoryViews" to categoryViews)
    }
}