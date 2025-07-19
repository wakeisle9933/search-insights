package com.si.main.searchinsights.controller

import com.si.main.searchinsights.exception.GlobalExceptionHandler
import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * SearchConsoleController 예외 처리 테스트
 * 
 * FIRST 원칙:
 * - Fast: MockMvc를 사용한 빠른 테스트
 * - Independent: 각 테스트가 독립적으로 실행
 * - Repeatable: 모킹을 통한 일관된 결과
 * - Self-Validating: 명확한 HTTP 응답 검증
 * - Timely: 컨트롤러 로직과 함께 작성
 * 
 * BCC:
 * - Boundary: 날짜 형식 경계 조건 테스트
 * - Correct: 정상적인 날짜 처리
 * - Error: 다양한 예외 상황 테스트
 */
class SearchConsoleControllerExceptionTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var mailService: MailService
    private lateinit var searchConsoleService: SearchConsoleService

    @BeforeEach
    fun setUp() {
        mailService = mockk(relaxed = true)
        searchConsoleService = mockk(relaxed = true)
        
        val controller = SearchConsoleController(mailService, searchConsoleService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `잘못된 날짜 형식 - 시작일`() {
        mockMvc.perform(
            get("/email-search-insights-report")
                .param("fromDate", "2024-13-45")
                .param("toDate", "2024-01-01")
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("SI401"))
            .andExpect(jsonPath("$.message").value("시작일의 날짜 형식이 올바르지 않습니다: 2024-13-45"))
    }

    @Test
    fun `잘못된 날짜 형식 - 종료일`() {
        mockMvc.perform(
            get("/email-search-insights-report")
                .param("fromDate", "2024-01-01")
                .param("toDate", "invalid-date")
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("SI401"))
            .andExpect(jsonPath("$.message").value("종료일의 날짜 형식이 올바르지 않습니다: invalid-date"))
    }

    @Test
    fun `시작일이 종료일보다 늦은 경우`() {
        mockMvc.perform(
            get("/email-search-insights-report")
                .param("fromDate", "2024-12-31")
                .param("toDate", "2024-01-01")
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("SI402"))
            .andExpect(jsonPath("$.message").value("시작일이 종료일보다 늦을 수 없습니다"))
    }

    @Test
    fun `날짜 파라미터가 없는 경우 - 기본값 사용`() {
        mockMvc.perform(
            get("/email-search-insights-report")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `정상적인 날짜 범위`() {
        mockMvc.perform(
            get("/email-search-insights-report")
                .param("fromDate", "2024-01-01")
                .param("toDate", "2024-01-31")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `같은 날짜 시작일과 종료일`() {
        mockMvc.perform(
            get("/email-search-insights-report")
                .param("fromDate", "2024-01-15")
                .param("toDate", "2024-01-15")
        )
            .andExpect(status().isOk)
    }
}