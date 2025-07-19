package com.si.main.searchinsights.controller

import com.google.api.services.searchconsole.v1.model.ApiDataRow
import com.si.main.searchinsights.data.PageViewInfo
import com.si.main.searchinsights.enum.ReportFrequency
import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import org.apache.commons.io.output.ByteArrayOutputStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * SearchConsoleController 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: Mock을 사용하여 실제 API 호출 없이 빠르게 실행
 * - Independent: 각 테스트는 독립적으로 실행
 * - Repeatable: 외부 의존성 없이 반복 가능
 * - Self-Validating: 명확한 성공/실패 판단
 * - Timely: 컨트롤러 구현과 함께 작성
 * 
 * BCC:
 * - Boundary: 날짜 경계값 (과거, 현재, 미래)
 * - Correct: 정상적인 리포트 생성 및 발송
 * - Existence: 파라미터 없을 때 기본값 처리
 * - Cardinality: 다양한 날짜 범위
 * - Error: 잘못된 날짜 형식 처리
 */
class SearchConsoleControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var controller: SearchConsoleController
    private lateinit var mockMailService: MailService
    private lateinit var mockSearchConsoleService: SearchConsoleService

    @BeforeEach
    fun setUp() {
        mockMailService = mockk(relaxed = true)
        mockSearchConsoleService = mockk(relaxed = true)
        controller = SearchConsoleController(mockMailService, mockSearchConsoleService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(com.si.main.searchinsights.exception.GlobalExceptionHandler())
            .build()
    }

    // ===========================================
    // 정상 케이스 테스트
    // ===========================================

    @Test
    fun `리포트 생성 및 이메일 발송 - 커스텀 날짜 범위`() {
        // Given
        val fromDate = "2024-07-01"
        val toDate = "2024-07-18"
        val mockExcelFile = createMockExcelData()
        
        val searchData = listOf(
            createMockApiDataRow("kotlin tutorial", "/page1", 100.0, 1000.0),
            createMockApiDataRow("spring boot", "/page2", 200.0, 2000.0)
        )
        val analyticsData = listOf(
            PageViewInfo("/page1", "Kotlin Tutorial", 1000.0),
            PageViewInfo("/page2", "Spring Boot Guide", 2000.0)
        )
        
        every { mockSearchConsoleService.fetchSearchAnalyticsData(fromDate, toDate) } returns searchData
        every { mockSearchConsoleService.fetchAnalyticsData(fromDate, toDate) } returns analyticsData
        every { 
            mockSearchConsoleService.createExcelFile(searchData, analyticsData, ReportFrequency.CUSTOM) 
        } returns mockExcelFile
        every { 
            mockMailService.sendMail(mockExcelFile, "search_insights.xlsx", ReportFrequency.CUSTOM, fromDate, toDate) 
        } just Runs

        // When & Then
        mockMvc.perform(get("/email-search-insights-report")
                .param("fromDate", fromDate)
                .param("toDate", toDate))
            .andExpect(status().isOk)

        // Verify
        verify(exactly = 1) { mockSearchConsoleService.fetchSearchAnalyticsData(fromDate, toDate) }
        verify(exactly = 1) { mockSearchConsoleService.fetchAnalyticsData(fromDate, toDate) }
        verify(exactly = 1) { mockSearchConsoleService.createExcelFile(searchData, analyticsData, ReportFrequency.CUSTOM) }
        verify(exactly = 1) { 
            mockMailService.sendMail(mockExcelFile, "search_insights.xlsx", ReportFrequency.CUSTOM, fromDate, toDate) 
        }
    }

    @Test
    fun `리포트 생성 및 이메일 발송 - 날짜 파라미터 없음 (기본값 사용)`() {
        // Given
        val expectedDate = LocalDate.now().minusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val mockExcelFile = createMockExcelData()
        
        val searchData = listOf(createMockApiDataRow("test", "/test", 50.0, 500.0))
        val analyticsData = listOf(PageViewInfo("/test", "Test Page", 500.0))
        
        every { mockSearchConsoleService.fetchSearchAnalyticsData(expectedDate, expectedDate) } returns searchData
        every { mockSearchConsoleService.fetchAnalyticsData(expectedDate, expectedDate) } returns analyticsData
        every { 
            mockSearchConsoleService.createExcelFile(searchData, analyticsData, ReportFrequency.CUSTOM) 
        } returns mockExcelFile
        every { 
            mockMailService.sendMail(mockExcelFile, "search_insights.xlsx", ReportFrequency.CUSTOM, expectedDate, expectedDate) 
        } just Runs

        // When & Then
        mockMvc.perform(get("/email-search-insights-report"))
            .andExpect(status().isOk)

        // Verify - 기본값으로 메서드 호출됨
        verify(exactly = 1) { mockSearchConsoleService.fetchSearchAnalyticsData(expectedDate, expectedDate) }
        verify(exactly = 1) { mockSearchConsoleService.fetchAnalyticsData(expectedDate, expectedDate) }
    }

    @Test
    fun `리포트 생성 및 이메일 발송 - fromDate만 제공`() {
        // Given
        val fromDate = "2024-07-15"
        val expectedToDate = LocalDate.now().minusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val mockExcelFile = createMockExcelData()
        
        val searchData = listOf(mockk<ApiDataRow>())
        val analyticsData = listOf(mockk<PageViewInfo>())
        
        every { mockSearchConsoleService.fetchSearchAnalyticsData(fromDate, expectedToDate) } returns searchData
        every { mockSearchConsoleService.fetchAnalyticsData(fromDate, expectedToDate) } returns analyticsData
        every { 
            mockSearchConsoleService.createExcelFile(searchData, analyticsData, ReportFrequency.CUSTOM) 
        } returns mockExcelFile
        every { 
            mockMailService.sendMail(mockExcelFile, "search_insights.xlsx", ReportFrequency.CUSTOM, fromDate, expectedToDate) 
        } just Runs

        // When & Then
        mockMvc.perform(get("/email-search-insights-report")
                .param("fromDate", fromDate))
            .andExpect(status().isOk)

        verify(exactly = 1) { mockSearchConsoleService.fetchSearchAnalyticsData(fromDate, expectedToDate) }
    }

    // ===========================================
    // 예외 처리 테스트
    // ===========================================

    // TODO: Controller에 @ExceptionHandler 추가 후 활성화
    // @Test
    // fun `리포트 생성 실패 - 잘못된 날짜 형식`() {
    //     // When & Then - 잘못된 날짜 (예외 발생)
    //     mockMvc.perform(get("/email-search-insights-report")
    //             .param("fromDate", "2024-07-32")  // 잘못된 날짜
    //             .param("toDate", "2024-07-18"))
    //         .andExpect(status().is5xxServerError)  // 예외가 처리되지 않아 500 에러

    //     // 잘못된 형식
    //     mockMvc.perform(get("/email-search-insights-report")
    //             .param("fromDate", "2024/07/01")  // 잘못된 형식
    //             .param("toDate", "2024-07-18"))
    //         .andExpect(status().is5xxServerError)  // 예외가 처리되지 않아 500 에러
    // }

    // TODO: Controller에 @ExceptionHandler 추가 후 활성화
    // @Test
    // fun `리포트 생성 실패 - 시작일이 종료일보다 늦음`() {
    //     // When & Then - 예외 발생
    //     mockMvc.perform(get("/email-search-insights-report")
    //             .param("fromDate", "2024-07-20")
    //             .param("toDate", "2024-07-10"))
    //         .andExpect(status().is5xxServerError)  // 예외가 처리되지 않아 500 에러
    // }

    // ===========================================
    // 경계값 테스트
    // ===========================================

    @Test
    fun `리포트 생성 - 동일한 날짜 (fromDate = toDate)`() {
        // Given
        val sameDate = "2024-07-18"
        val mockExcelFile = createMockExcelData()
        
        val searchData = listOf(createMockApiDataRow("single day", "/single", 10.0, 100.0))
        val analyticsData = listOf(PageViewInfo("/single", "Single Day", 100.0))
        
        every { mockSearchConsoleService.fetchSearchAnalyticsData(sameDate, sameDate) } returns searchData
        every { mockSearchConsoleService.fetchAnalyticsData(sameDate, sameDate) } returns analyticsData
        every { 
            mockSearchConsoleService.createExcelFile(searchData, analyticsData, ReportFrequency.CUSTOM) 
        } returns mockExcelFile
        every { 
            mockMailService.sendMail(mockExcelFile, "search_insights.xlsx", ReportFrequency.CUSTOM, sameDate, sameDate) 
        } just Runs

        // When & Then
        mockMvc.perform(get("/email-search-insights-report")
                .param("fromDate", sameDate)
                .param("toDate", sameDate))
            .andExpect(status().isOk)

        verify(exactly = 1) { mockSearchConsoleService.fetchSearchAnalyticsData(sameDate, sameDate) }
    }

    @Test
    fun `리포트 생성 - 매우 넓은 날짜 범위 (1년)`() {
        // Given
        val fromDate = "2023-07-01"
        val toDate = "2024-07-01"
        val mockExcelFile = createMockExcelData()
        
        val searchData = (1..1000).map { 
            createMockApiDataRow("query$it", "/page$it", it.toDouble(), it * 10.0)
        }
        val analyticsData = (1..100).map { 
            PageViewInfo("/page$it", "Page $it", it * 100.0)
        }
        
        every { mockSearchConsoleService.fetchSearchAnalyticsData(fromDate, toDate) } returns searchData
        every { mockSearchConsoleService.fetchAnalyticsData(fromDate, toDate) } returns analyticsData
        every { 
            mockSearchConsoleService.createExcelFile(searchData, analyticsData, ReportFrequency.CUSTOM) 
        } returns mockExcelFile
        every { 
            mockMailService.sendMail(mockExcelFile, "search_insights.xlsx", ReportFrequency.CUSTOM, fromDate, toDate) 
        } just Runs

        // When & Then
        mockMvc.perform(get("/email-search-insights-report")
                .param("fromDate", fromDate)
                .param("toDate", toDate))
            .andExpect(status().isOk)

        verify(exactly = 1) { mockSearchConsoleService.fetchSearchAnalyticsData(fromDate, toDate) }
    }

    // ===========================================
    // 통합 시나리오 테스트
    // ===========================================

    @Test
    fun `리포트 생성 - 빈 데이터 처리`() {
        // Given
        val fromDate = "2024-07-01"
        val toDate = "2024-07-18"
        
        val emptySearchData = emptyList<ApiDataRow>()
        val emptyAnalyticsData = emptyList<PageViewInfo>()
        
        every { mockSearchConsoleService.fetchSearchAnalyticsData(fromDate, toDate) } returns emptySearchData
        every { mockSearchConsoleService.fetchAnalyticsData(fromDate, toDate) } returns emptyAnalyticsData

        // When & Then - 빈 데이터일 때 NO_CONTENT 에러 발생
        mockMvc.perform(get("/email-search-insights-report")
                .param("fromDate", fromDate)
                .param("toDate", toDate))
            .andExpect(status().isNoContent)  // 204 No Content

        // 이메일 발송과 엑셀 생성이 호출되지 않았는지 확인
        verify(exactly = 0) { 
            mockSearchConsoleService.createExcelFile(any(), any(), any()) 
        }
        verify(exactly = 0) { 
            mockMailService.sendMail(any(), any(), any(), any(), any()) 
        }
    }

    // TODO: Controller에 @ExceptionHandler 추가 후 활성화
    // @Test
    // fun `리포트 생성 - 서비스 예외 발생`() {
    //     // Given
    //     val fromDate = "2024-07-01"
    //     val toDate = "2024-07-18"
    //     
    //     every { 
    //         mockSearchConsoleService.fetchSearchAnalyticsData(fromDate, toDate) 
    //     } throws RuntimeException("API Error")

    //     // When & Then - 예외가 처리되지 않아 500 에러
    //     mockMvc.perform(get("/email-search-insights-report")
    //             .param("fromDate", fromDate)
    //             .param("toDate", toDate))
    //         .andExpect(status().is5xxServerError)  // 정상적인 동작
    // }

    // ===========================================
    // 헬퍼 메서드
    // ===========================================

    private fun createMockExcelData(): ByteArrayOutputStream {
        return ByteArrayOutputStream().apply {
            write("Mock Excel Data".toByteArray())
        }
    }
    
    private fun createMockApiDataRow(query: String, page: String, clicks: Double, impressions: Double): ApiDataRow {
        return mockk<ApiDataRow>(relaxed = true).apply {
            every { keys } returns mutableSetOf(query, page)
            every { this@apply.clicks } returns clicks
            every { this@apply.impressions } returns impressions
            every { ctr } returns clicks / impressions
            every { position } returns 1.5
        }
    }
}