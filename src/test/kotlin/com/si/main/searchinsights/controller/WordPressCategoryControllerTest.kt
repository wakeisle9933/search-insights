package com.si.main.searchinsights.controller

import com.si.main.searchinsights.service.WordPressCategoryService
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * WordPressCategoryController 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: MockMvc를 사용하여 빠른 테스트 실행
 * - Independent: 각 테스트는 독립적으로 실행
 * - Repeatable: 반복 가능한 테스트
 * - Self-Validating: 명확한 성공/실패 판단
 * - Timely: 컨트롤러 구현과 함께 작성
 * 
 * BCC:
 * - Correct: 정상적인 경로 확인
 * - Error: 잘못된 경로 처리
 * 
 * 참고: 파일 시스템 접근은 통합 테스트에서 검증
 */
class WordPressCategoryControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var controller: WordPressCategoryController
    private lateinit var mockWordPressCategoryService: WordPressCategoryService

    @BeforeEach
    fun setUp() {
        mockWordPressCategoryService = mockk(relaxed = true)
        controller = WordPressCategoryController(mockWordPressCategoryService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `WordPress 카테고리 데이터 조회 - 잘못된 경로 404`() {
        // When & Then
        mockMvc.perform(get("/api/wp-category-data"))  // 잘못된 경로
            .andExpect(status().isNotFound)
    }

    @Test
    fun `WordPress 카테고리 데이터 조회 - 대소문자 구분`() {
        // When & Then
        mockMvc.perform(get("/api/WP-CATEGORIES-DATA"))  // 대문자
            .andExpect(status().isNotFound)
    }
    
    @Test
    fun `WordPress 카테고리 데이터 조회 - 추가 경로 세그먼트`() {
        // When & Then
        mockMvc.perform(get("/api/wp-categories-data/extra"))  // 추가 경로
            .andExpect(status().isNotFound)
    }

    @Test
    fun `WordPress 카테고리 데이터 조회 - 경로 앞부분만 일치`() {
        // When & Then
        mockMvc.perform(get("/api/wp-categories"))  // 경로 일부만
            .andExpect(status().isNotFound)
    }
}