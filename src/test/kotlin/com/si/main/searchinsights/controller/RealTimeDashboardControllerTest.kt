package com.si.main.searchinsights.controller

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * RealTimeDashboardController 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: MockMvc를 사용하여 빠른 테스트 실행
 * - Independent: 단일 엔드포인트 테스트로 독립적
 * - Repeatable: 외부 의존성 없이 반복 가능
 * - Self-Validating: 명확한 성공/실패 판단
 * - Timely: 컨트롤러 구현과 함께 작성
 * 
 * BCC:
 * - Boundary: 단일 경로만 존재하므로 경계값 테스트 불필요
 * - Correct: 정상적인 뷰 반환 확인
 * - Existence: 올바른 템플릿 이름 반환 확인
 * - Error: 간단한 컨트롤러로 에러 케이스 없음
 */
class RealTimeDashboardControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var controller: RealTimeDashboardController

    @BeforeEach
    fun setUp() {
        controller = RealTimeDashboardController()
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `대시보드 페이지 요청 - 정상 케이스`() {
        // When & Then
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk)
            .andExpect(view().name("realtime-dashboard"))
    }

    @Test
    fun `대시보드 페이지 요청 - 뷰 이름 확인`() {
        // When
        val result = controller.dashboard()
        
        // Then
        assert(result == "realtime-dashboard") { 
            "Expected view name 'realtime-dashboard' but got '$result'" 
        }
    }

    @Test
    fun `대시보드 페이지 요청 - HTTP 메서드 GET 확인`() {
        // When & Then - GET 요청 성공
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk)
    }

    @Test
    fun `대시보드 페이지 요청 - 잘못된 경로`() {
        // When & Then - 404 에러
        mockMvc.perform(get("/dashboards"))  // 's' 추가
            .andExpect(status().isNotFound)
    }

    @Test
    fun `대시보드 페이지 요청 - 대소문자 구분 확인`() {
        // When & Then - 경로는 대소문자 구분
        mockMvc.perform(get("/Dashboard"))  // 대문자 D
            .andExpect(status().isNotFound)
    }

    @Test
    fun `대시보드 페이지 요청 - 쿼리 파라미터 포함`() {
        // When & Then - 쿼리 파라미터는 무시되고 정상 작동
        mockMvc.perform(get("/dashboard?refresh=true"))
            .andExpect(status().isOk)
            .andExpect(view().name("realtime-dashboard"))
    }
}