package com.si.main.searchinsights.service

import com.google.analytics.data.v1beta.*
import com.google.api.services.searchconsole.v1.SearchConsole
import com.google.api.services.searchconsole.v1.model.*
import com.si.main.searchinsights.enum.ReportFrequency
import com.si.main.searchinsights.data.PageViewInfo
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * SearchConsoleService 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: Mock을 사용하여 실제 API 호출 없이 빠르게 실행
 * - Independent: 각 테스트는 독립적인 Mock 설정
 * - Repeatable: 외부 의존성 없이 반복 가능
 * - Self-Validating: API 응답과 Excel 생성 검증
 * - Timely: Google API 통합 로직과 함께 작성
 * 
 * BCC:
 * - Boundary: 날짜 범위, 데이터 제한
 * - Correct: 정상적인 API 응답 처리
 * - Existence: 자격 증명 파일 존재
 * - Cardinality: 다수의 검색 쿼리와 페이지
 * - Error: API 오류 처리
 */
class SearchConsoleServiceTest {

    private lateinit var service: SearchConsoleService
    private lateinit var mockSpreadSheetService: SpreadSheetService
    private lateinit var mockSearchConsole: SearchConsole
    private lateinit var mockAnalyticsClient: BetaAnalyticsDataClient
    
    private val domain = "https://test.com"
    private val propId = "12345"

    @BeforeEach
    fun setUp() {
        // Mock 설정
        mockSpreadSheetService = mockk(relaxed = true)
        mockSearchConsole = mockk(relaxed = true)
        mockAnalyticsClient = mockk(relaxed = true)
        
        // Service 생성 - 변경된 생성자에 맞게 수정
        service = spyk(SearchConsoleService(
            mockSpreadSheetService, 
            mockSearchConsole, 
            mockAnalyticsClient, 
            domain, 
            propId
        ))
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ===========================================
    // Search Console API 테스트
    // ===========================================

    @Test
    fun `검색 쿼리 데이터 조회 성공`() {
        // Given
        val mockResponse = SearchAnalyticsQueryResponse().apply {
            rows = listOf(
                ApiDataRow().apply {
                    setKeys(listOf("kotlin tutorial", "/page1"))
                    setClicks(100.0)
                    setImpressions(1000.0)
                    setCtr(0.1)
                    setPosition(1.5)
                },
                ApiDataRow().apply {
                    setKeys(listOf("spring boot", "/page2"))
                    setClicks(50.0)
                    setImpressions(500.0)
                    setCtr(0.1)
                    setPosition(2.0)
                }
            )
        }

        every { 
            mockSearchConsole.searchanalytics()
                .query(any(), any())
                .execute() 
        } returns mockResponse

        // When
        val result = service.fetchSearchAnalyticsData(
            LocalDate.now().minusDays(7).toString(),
            LocalDate.now().toString()
        )

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("kotlin tutorial", result[0].getKeys()[0])
        assertEquals(100.0, result[0].getClicks())
    }

    @Test
    fun `빈 검색 결과 처리`() {
        // Given
        val mockResponse = SearchAnalyticsQueryResponse().apply {
            rows = emptyList()
        }

        every { 
            mockSearchConsole.searchanalytics()
                .query(any(), any())
                .execute() 
        } returns mockResponse

        // When
        val result = service.fetchSearchAnalyticsData(
            LocalDate.now().minusDays(7).toString(),
            LocalDate.now().toString()
        )

        // Then
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `Excel 파일 생성 테스트`() {
        // Given
        val mockRows = listOf(
            ApiDataRow().apply {
                setKeys(listOf("test query", "/test-page"))
                setClicks(100.0)
                setImpressions(1000.0)
                setCtr(0.1)
                setPosition(1.5)
            }
        )
        val mockPageViews = listOf(
            PageViewInfo("/page1", "Page 1", 100.0)
        )
        
        // When
        val result = service.createExcelFile(mockRows, mockPageViews, ReportFrequency.DAILY)
        
        // Then
        assertNotNull(result)
        assertTrue(result.size() > 0)
    }

    // ===========================================
    // Analytics API 테스트
    // ===========================================

    @Test
    fun `Analytics 실시간 데이터 조회`() {
        // Given
        val mockResponse = RunReportResponse.newBuilder()
            .addRows(
                Row.newBuilder()
                    .addDimensionValues(DimensionValue.newBuilder().setValue("Page 1"))
                    .addDimensionValues(DimensionValue.newBuilder().setValue("/page1"))
                    .addMetricValues(MetricValue.newBuilder().setValue("100"))
            )
            .addRows(
                Row.newBuilder()
                    .addDimensionValues(DimensionValue.newBuilder().setValue("Page 2"))
                    .addDimensionValues(DimensionValue.newBuilder().setValue("/page2"))
                    .addMetricValues(MetricValue.newBuilder().setValue("50"))
            )
            .build()

        every { 
            mockAnalyticsClient.runReport(match<RunReportRequest> {
                it.dateRangesList.any { range -> range.startDate == "today" }
            }) 
        } returns mockResponse

        // When
        val result = service.fetchRealTimeAnalyticsData()

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("/page1", result[0].pagePath)
        assertEquals(100.0, result[0].pageViews)
    }

    @Test
    fun `Analytics 기간별 데이터 조회`() {
        // Given
        val mockResponse = RunReportResponse.newBuilder()
            .addRows(
                Row.newBuilder()
                    .addDimensionValues(DimensionValue.newBuilder().setValue("/page1"))
                    .addDimensionValues(DimensionValue.newBuilder().setValue("Page 1"))
                    .addMetricValues(MetricValue.newBuilder().setValue("500"))
            )
            .build()

        every { 
            mockAnalyticsClient.runReport(any<RunReportRequest>()) 
        } returns mockResponse

        // When
        val result = service.fetchAnalyticsData(
            LocalDate.now().minusDays(7).toString(),
            LocalDate.now().toString()
        )

        // Then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("/page1", result[0].pagePath)
        assertEquals(500.0, result[0].pageViews)
    }

    // ===========================================
    // 예외 처리 테스트
    // ===========================================

    @Test
    fun `API 호출 실패시 예외 처리`() {
        // Given
        every { 
            mockSearchConsole.searchanalytics()
                .query(any(), any())
                .execute() 
        } throws Exception("API Error")

        // When - fetchSearchAnalyticsData는 예외를 처리하지 않음
        try {
            service.fetchSearchAnalyticsData(
                LocalDate.now().minusDays(7).toString(),
                LocalDate.now().toString()
            )
        } catch (e: Exception) {
            // Then
            assertTrue(e.message?.contains("API Error") == true)
        }
    }

    // ===========================================
    // 실시간 데이터 조회 테스트
    // ===========================================

    @Test
    fun `실시간 분석 데이터와 활성 사용자 조회`() {
        // Given
        val mockActiveUsersResponse = RunReportResponse.newBuilder()
            .addRows(
                Row.newBuilder()
                    .addMetricValues(MetricValue.newBuilder().setValue("50"))
            )
            .build()
            
        val mockPageViewsResponse = RunReportResponse.newBuilder()
            .addRows(
                Row.newBuilder()
                    .addDimensionValues(DimensionValue.newBuilder().setValue("Page 1"))
                    .addDimensionValues(DimensionValue.newBuilder().setValue("/page1"))
                    .addMetricValues(MetricValue.newBuilder().setValue("100"))
            )
            .build()

        every { 
            mockAnalyticsClient.runReport(match<RunReportRequest> {
                it.metricsList.any { metric -> metric.name == "activeUsers" } &&
                it.dateRangesList.any { range -> range.startDate == "today" }
            }) 
        } returns mockActiveUsersResponse
        
        every { 
            mockAnalyticsClient.runReport(match<RunReportRequest> {
                it.dimensionsList.any { dim -> dim.name == "pagePath" } &&
                it.dateRangesList.any { range -> range.startDate == "today" }
            }) 
        } returns mockPageViewsResponse

        // When
        val result = service.fetchRealTimeAnalyticsWithActiveUsers()

        // Then
        assertNotNull(result)
        assertTrue(result.containsKey("pageViews"))
        assertTrue(result.containsKey("activeUsers"))
        assertEquals(50, result["activeUsers"])
        assertTrue(result["pageViews"] is List<*>)
    }

    @Test
    fun `30분 분석 데이터 조회`() {
        // Given
        val mockResponse = RunRealtimeReportResponse.newBuilder()
            .addRows(
                Row.newBuilder()
                    .addDimensionValues(DimensionValue.newBuilder().setValue("/page1"))
                    .addDimensionValues(DimensionValue.newBuilder().setValue("Page 1"))
                    .addMetricValues(MetricValue.newBuilder().setValue("200"))
            )
            .build()
            
        val mockActiveUsersResponse = RunRealtimeReportResponse.newBuilder()
            .addRows(
                Row.newBuilder()
                    .addMetricValues(MetricValue.newBuilder().setValue("25"))
            )
            .build()

        every { 
            mockAnalyticsClient.runRealtimeReport(match<RunRealtimeReportRequest> {
                it.minuteRangesList.any { range -> 
                    range.startMinutesAgo == 29 && range.endMinutesAgo == 0
                } && it.dimensionsList.any { dim -> dim.name == "unifiedScreenName" }
            }) 
        } returns mockResponse
        
        every { 
            mockAnalyticsClient.runRealtimeReport(match<RunRealtimeReportRequest> {
                it.metricsList.any { metric -> metric.name == "activeUsers" }
            }) 
        } returns mockActiveUsersResponse

        // When
        val result = service.fetchLast30MinAnalyticsWithActiveUsers()

        // Then
        assertNotNull(result)
        assertTrue(result.containsKey("pageViews"))
        assertTrue(result.containsKey("activeUsers"))
        assertEquals(25, result["activeUsers"])
        assertTrue(result["pageViews"] is List<*>)
    }


    @Test
    fun `커스텀 날짜 범위 분석 데이터 조회`() {
        // Given
        val mockResponse = RunReportResponse.newBuilder()
            .addRows(
                Row.newBuilder()
                    .addDimensionValues(DimensionValue.newBuilder().setValue("/custom-page"))
                    .addDimensionValues(DimensionValue.newBuilder().setValue("Custom Page"))
                    .addMetricValues(MetricValue.newBuilder().setValue("300"))
            )
            .build()

        every { 
            mockAnalyticsClient.runReport(any<RunReportRequest>()) 
        } returns mockResponse

        // When
        val result = service.fetchCustomDateAnalyticsWithActiveUsers(
            "2024-07-01",
            "2024-07-18"
        )

        // Then
        assertNotNull(result)
        assertTrue(result.containsKey("pageViews"))
        assertTrue(result.containsKey("activeUsers"))
    }
}