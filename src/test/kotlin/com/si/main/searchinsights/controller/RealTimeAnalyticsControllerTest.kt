package com.si.main.searchinsights.controller

import com.si.main.searchinsights.data.PageViewInfo
import com.si.main.searchinsights.service.SearchConsoleService
import com.si.main.searchinsights.service.WordPressCategoryService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * RealTimeAnalyticsController 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: Mock을 사용하여 빠른 실행
 * - Independent: 각 테스트는 독립적 실행
 * - Repeatable: 외부 API 호출 없이 반복 가능
 * - Self-Validating: 명확한 성공/실패 판단
 * - Timely: 컨트롤러 구현과 함께 작성
 * 
 * BCC:
 * - Boundary: 다양한 날짜 범위
 * - Correct: 정상적인 API 응답
 * - Existence: 빈 데이터 처리
 * - Cardinality: 여러 페이지뷰 처리
 * - Error: 서비스 예외 처리
 */
class RealTimeAnalyticsControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var controller: RealTimeAnalyticsController
    private lateinit var mockSearchConsoleService: SearchConsoleService
    private lateinit var mockWordPressCategoryService: WordPressCategoryService

    @BeforeEach
    fun setUp() {
        mockSearchConsoleService = mockk(relaxed = true)
        mockWordPressCategoryService = mockk(relaxed = true)
        controller = RealTimeAnalyticsController(mockSearchConsoleService, mockWordPressCategoryService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    // ===========================================
    // 실시간 페이지뷰 테스트
    // ===========================================

    @Test
    fun `실시간 페이지뷰 조회 - 정상 케이스`() {
        // Given
        val pageViews = listOf(
            PageViewInfo("/page1", "Page 1", 100.0),
            PageViewInfo("/page2", "Page 2", 200.0)
        )
        val activeUsers = 50
        val serviceResponse = mapOf(
            "pageViews" to pageViews,
            "activeUsers" to activeUsers
        )
        val categoryViews = mapOf(
            "Technology" to 150.0,
            "News" to 150.0
        )
        
        every { mockSearchConsoleService.fetchRealTimeAnalyticsWithActiveUsers() } returns serviceResponse
        every { mockWordPressCategoryService.getCategoryPageViews(pageViews) } returns categoryViews

        // When & Then
        mockMvc.perform(get("/api/realtime-pageviews")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pageViews").isArray)
            .andExpect(jsonPath("$.pageViews[0].pagePath").value("/page1"))
            .andExpect(jsonPath("$.pageViews[0].pageViews").value(100.0))
            .andExpect(jsonPath("$.activeUsers").value(50))
            .andExpect(jsonPath("$.categoryViews.Technology").value(150.0))
            .andExpect(jsonPath("$.categoryViews.News").value(150.0))

        verify(exactly = 1) { mockSearchConsoleService.fetchRealTimeAnalyticsWithActiveUsers() }
        verify(exactly = 1) { mockWordPressCategoryService.getCategoryPageViews(pageViews) }
    }

    @Test
    fun `실시간 페이지뷰 조회 - 빈 데이터`() {
        // Given
        val emptyPageViews = emptyList<PageViewInfo>()
        val serviceResponse = mapOf(
            "pageViews" to emptyPageViews,
            "activeUsers" to 0
        )
        val emptyCategoryViews = emptyMap<String, Double>()
        
        every { mockSearchConsoleService.fetchRealTimeAnalyticsWithActiveUsers() } returns serviceResponse
        every { mockWordPressCategoryService.getCategoryPageViews(emptyPageViews) } returns emptyCategoryViews

        // When & Then
        mockMvc.perform(get("/api/realtime-pageviews"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pageViews").isArray)
            .andExpect(jsonPath("$.pageViews").isEmpty)
            .andExpect(jsonPath("$.activeUsers").value(0))
            .andExpect(jsonPath("$.categoryViews").isEmpty)
    }

    // ===========================================
    // 최근 30분 페이지뷰 테스트
    // ===========================================

    @Test
    fun `최근 30분 페이지뷰 조회 - 정상 케이스`() {
        // Given
        val pageViews = listOf(
            PageViewInfo("/article1", "Article 1", 50.0),
            PageViewInfo("/article2", "Article 2", 75.0),
            PageViewInfo("/article3", "Article 3", 125.0)
        )
        val activeUsers = 30
        val serviceResponse = mapOf(
            "pageViews" to pageViews,
            "activeUsers" to activeUsers
        )
        val categoryViews = mapOf(
            "Blog" to 250.0
        )
        
        every { mockSearchConsoleService.fetchLast30MinAnalyticsWithActiveUsers() } returns serviceResponse
        every { mockWordPressCategoryService.getCategoryPageViews(pageViews) } returns categoryViews

        // When & Then
        mockMvc.perform(get("/api/last30min-pageviews"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pageViews").isArray)
            .andExpect(jsonPath("$.pageViews.length()").value(3))
            .andExpect(jsonPath("$.activeUsers").value(30))
            .andExpect(jsonPath("$.categoryViews.Blog").value(250.0))
    }

    // ===========================================
    // 커스텀 날짜 범위 페이지뷰 테스트
    // ===========================================

    @Test
    fun `커스텀 날짜 범위 페이지뷰 조회 - 정상 케이스`() {
        // Given
        val startDate = "2024-07-01"
        val endDate = "2024-07-18"
        val pageViews = listOf(
            PageViewInfo("/post1", "Post 1", 1000.0),
            PageViewInfo("/post2", "Post 2", 2000.0)
        )
        val activeUsers = 150
        val serviceResponse = mapOf(
            "pageViews" to pageViews,
            "activeUsers" to activeUsers
        )
        val categoryViews = mapOf(
            "Tutorial" to 1500.0,
            "Guide" to 1500.0
        )
        
        every { 
            mockSearchConsoleService.fetchCustomDateAnalyticsWithActiveUsers(startDate, endDate) 
        } returns serviceResponse
        every { mockWordPressCategoryService.getCategoryPageViews(pageViews) } returns categoryViews

        // When & Then
        mockMvc.perform(get("/api/custom-date-pageviews")
                .param("startDate", startDate)
                .param("endDate", endDate))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pageViews[0].pagePath").value("/post1"))
            .andExpect(jsonPath("$.pageViews[1].pageViews").value(2000.0))
            .andExpect(jsonPath("$.categoryViews.Tutorial").value(1500.0))

        verify(exactly = 1) { 
            mockSearchConsoleService.fetchCustomDateAnalyticsWithActiveUsers(startDate, endDate) 
        }
    }

    @Test
    fun `커스텀 날짜 범위 페이지뷰 조회 - 파라미터 누락`() {
        // When & Then - startDate 누락
        mockMvc.perform(get("/api/custom-date-pageviews")
                .param("endDate", "2024-07-18"))
            .andExpect(status().isBadRequest)

        // When & Then - endDate 누락
        mockMvc.perform(get("/api/custom-date-pageviews")
                .param("startDate", "2024-07-01"))
            .andExpect(status().isBadRequest)
    }

    // ===========================================
    // WordPress 카테고리 동기화 테스트
    // ===========================================

    @Test
    fun `WordPress 카테고리 동기화 - 기본 동기화`() {
        // Given
        every { mockWordPressCategoryService.fullSync(false) } returns Unit

        // When & Then
        mockMvc.perform(get("/api/sync-wordpress-categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("동기화 완료!"))

        verify(exactly = 1) { mockWordPressCategoryService.fullSync(false) }
    }

    @Test
    fun `WordPress 카테고리 동기화 - 강제 전체 동기화`() {
        // Given
        every { mockWordPressCategoryService.fullSync(true) } returns Unit

        // When & Then
        mockMvc.perform(get("/api/sync-wordpress-categories")
                .param("forceFullSync", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("동기화 완료!"))

        verify(exactly = 1) { mockWordPressCategoryService.fullSync(true) }
    }

    // ===========================================
    // 경계값 및 예외 처리 테스트
    // ===========================================

    @Test
    fun `실시간 페이지뷰 조회 - 매우 많은 페이지뷰 처리`() {
        // Given
        val manyPageViews = (1..1000).map { 
            PageViewInfo("/page$it", "Page $it", it.toDouble())
        }
        val serviceResponse = mapOf(
            "pageViews" to manyPageViews,
            "activeUsers" to 5000
        )
        val categoryViews = mapOf("All" to 500500.0)  // Sum of 1..1000
        
        every { mockSearchConsoleService.fetchRealTimeAnalyticsWithActiveUsers() } returns serviceResponse
        every { mockWordPressCategoryService.getCategoryPageViews(manyPageViews) } returns categoryViews

        // When & Then
        mockMvc.perform(get("/api/realtime-pageviews"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.pageViews.length()").value(1000))
            .andExpect(jsonPath("$.categoryViews.All").value(500500.0))
    }

    @Test
    fun `실시간 페이지뷰 조회 - 서비스 예외 발생`() {
        // Given
        every { 
            mockSearchConsoleService.fetchRealTimeAnalyticsWithActiveUsers() 
        } throws RuntimeException("Analytics API Error")

        // When & Then - 예외가 발생하면 500 에러
        try {
            controller.getRealTimePageViews()
            fail("예외가 발생해야 함")
        } catch (e: RuntimeException) {
            assertEquals("Analytics API Error", e.message)
        }
    }

    // ===========================================
    // 통합 테스트 (Unit Test 범위 내에서)
    // ===========================================

    @Test
    fun `컨트롤러 메서드 직접 호출 테스트`() {
        // Given
        val pageViews = listOf(PageViewInfo("/test", "Test", 42.0))
        val serviceResponse = mapOf(
            "pageViews" to pageViews,
            "activeUsers" to 10
        )
        val categoryViews = mapOf("Test" to 42.0)
        
        every { mockSearchConsoleService.fetchRealTimeAnalyticsWithActiveUsers() } returns serviceResponse
        every { mockWordPressCategoryService.getCategoryPageViews(pageViews) } returns categoryViews

        // When
        val result = controller.getRealTimePageViews()

        // Then
        assertEquals(pageViews, result["pageViews"])
        assertEquals(10, result["activeUsers"])
        assertEquals(categoryViews, result["categoryViews"])
    }
}