package com.si.main.searchinsights.service

import com.si.main.searchinsights.data.PageViewInfo
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WordPressCategoryService 간단한 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: Mock을 사용하여 빠른 테스트
 * - Independent: 각 테스트는 독립적
 * - Repeatable: 반복 가능
 * - Self-Validating: 명확한 판단
 * - Timely: 서비스 구현과 함께 작성
 * 
 * 참고: WordPress API 통합 부분은 통합 테스트에서 검증
 */
class WordPressCategoryServiceTest {

    private lateinit var service: WordPressCategoryService
    private val domain = "https://test.com"

    @BeforeEach
    fun setUp() {
        service = WordPressCategoryService(domain)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `카테고리별 페이지뷰 집계 - 빈 리스트`() {
        // Given
        val pageViews = emptyList<PageViewInfo>()
        
        // When
        val result = service.getCategoryPageViews(pageViews)
        
        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `카테고리별 페이지뷰 집계 - 카테고리가 없는 페이지들`() {
        // Given
        val pageViews = listOf(
            PageViewInfo("/page1", "Page 1", 100.0),
            PageViewInfo("/page2", "Page 2", 200.0)
        )
        
        // When
        val result = service.getCategoryPageViews(pageViews)
        
        // Then - 캐시 데이터가 없으므로 빈 맵 반환
        assertTrue(result.isEmpty())
    }
}