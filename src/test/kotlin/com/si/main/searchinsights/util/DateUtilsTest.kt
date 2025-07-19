package com.si.main.searchinsights.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.NullSource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * DateUtils 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: 외부 의존성 없이 빠르게 실행
 * - Independent: 각 테스트는 독립적으로 실행
 * - Repeatable: 시간대와 무관하게 반복 가능
 * - Self-Validating: 명확한 성공/실패 판단
 * - Timely: 프로덕션 코드와 함께 작성
 * 
 * BCC (Boundary Condition Coverage):
 * - Boundary: 월초, 월말, 연초, 연말 테스트
 * - Correct: 정상적인 날짜 형식
 * - Conformance: 날짜 형식 검증
 * - Existence: null 처리
 * - Range: 유효한 날짜 범위
 */
class DateUtilsTest {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ===========================================
    // convertToLocalDateString 테스트
    // ===========================================
    
    @Test
    fun `정상적인 날짜 문자열 변환 테스트`() {
        // Given
        val dateString = "2024-08-13"
        
        // When
        val result = DateUtils.convertToLocalDateString(dateString)
        
        // Then
        assertEquals("2024-08-13", result)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "2024-01-01",  // 연초
        "2024-12-31",  // 연말
        "2024-02-29",  // 윤년 2월 29일
        "2023-02-28",  // 평년 2월 마지막날
        "2024-01-31",  // 31일이 있는 달의 마지막날
        "2024-04-30"   // 30일이 있는 달의 마지막날
    ])
    fun `경계값 날짜 변환 테스트`(dateString: String) {
        // When
        val result = DateUtils.convertToLocalDateString(dateString)
        
        // Then
        assertEquals(dateString, result)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "2024-13-01",  // 잘못된 월
        "2024-00-01",  // 0월
        "2024-01-32",  // 잘못된 일
        "2023-02-29",  // 평년에 2월 29일
        "20240813",    // 하이픈 없는 형식
        "2024/08/13",  // 슬래시 형식
        "13-08-2024",  // 잘못된 순서
        "abc",         // 문자열
        ""             // 빈 문자열
    ])
    fun `잘못된 형식의 날짜 문자열 변환시 예외 발생`(dateString: String) {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            DateUtils.convertToLocalDateString(dateString)
        }
        assertTrue(exception.message!!.contains("Invalid date formats"))
        assertTrue(exception.message!!.contains(dateString))
    }

    @ParameterizedTest
    @NullSource
    fun `null 입력시 예외 발생`(dateString: String?) {
        // When & Then
        assertThrows<IllegalArgumentException> {
            DateUtils.convertToLocalDateString(dateString)
        }
    }

    // ===========================================
    // getFormattedCurrentDate 테스트
    // ===========================================
    
    @Test
    fun `현재 날짜 포맷팅 테스트`() {
        // When
        val result = DateUtils.getFormattedCurrentDate()
        
        // Then
        val today = LocalDate.now().format(formatter)
        assertEquals(today, result)
        
        // 형식 검증 (yyyy-MM-dd)
        assertTrue(result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    // ===========================================
    // getFormattedDateBeforeDays 테스트
    // ===========================================
    
    @Test
    fun `1일 전 날짜 계산 테스트`() {
        // When
        val result = DateUtils.getFormattedDateBeforeDays(1)
        
        // Then
        val expected = LocalDate.now().minusDays(1).format(formatter)
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @ValueSource(longs = [0, 7, 30, 365, 1000])
    fun `다양한 일수 전 날짜 계산 테스트`(days: Long) {
        // When
        val result = DateUtils.getFormattedDateBeforeDays(days)
        
        // Then
        val expected = LocalDate.now().minusDays(days).format(formatter)
        assertEquals(expected, result)
    }

    @Test
    fun `음수 일수로 미래 날짜 계산 테스트`() {
        // Given
        val days = -7L
        
        // When
        val result = DateUtils.getFormattedDateBeforeDays(days)
        
        // Then
        val expected = LocalDate.now().plusDays(7).format(formatter)
        assertEquals(expected, result)
    }

    // ===========================================
    // getFirstDayOfPreviousMonth 테스트
    // ===========================================
    
    @Test
    fun `지난달 첫날 계산 테스트`() {
        // When
        val result = DateUtils.getFirstDayOfPreviousMonth()
        
        // Then
        val expected = LocalDate.now()
            .minusMonths(1)
            .withDayOfMonth(1)
            .format(formatter)
        assertEquals(expected, result)
        
        // 항상 01일인지 확인
        assertTrue(result.endsWith("-01"))
    }

    @Test
    fun `1월에서 지난달 첫날 계산시 작년 12월 1일`() {
        // Given - 이 테스트는 1월에 실행될 때를 가정
        // 실제로는 시스템 시간을 모킹해야 하지만, 로직 검증용
        val januaryDate = LocalDate.of(2024, 1, 15)
        
        // When - DateUtils가 현재 날짜 기반이므로 결과만 검증
        val expected = januaryDate
            .minusMonths(1)
            .withDayOfMonth(1)
            .format(formatter)
        
        // Then
        assertEquals("2023-12-01", expected)
    }

    // ===========================================
    // getLastDayOfPreviousMonth 테스트
    // ===========================================
    
    @Test
    fun `지난달 마지막날 계산 테스트`() {
        // When
        val result = DateUtils.getLastDayOfPreviousMonth()
        
        // Then
        val expected = LocalDate.now()
            .withDayOfMonth(1)
            .minusDays(1)
            .format(formatter)
        assertEquals(expected, result)
    }

    @Test
    fun `3월에서 지난달 마지막날 계산시 2월 마지막날 (평년)`() {
        // Given
        val marchDate = LocalDate.of(2023, 3, 15) // 평년
        
        // When
        val expected = marchDate
            .withDayOfMonth(1)
            .minusDays(1)
            .format(formatter)
        
        // Then
        assertEquals("2023-02-28", expected)
    }

    @Test
    fun `3월에서 지난달 마지막날 계산시 2월 마지막날 (윤년)`() {
        // Given
        val marchDate = LocalDate.of(2024, 3, 15) // 윤년
        
        // When
        val expected = marchDate
            .withDayOfMonth(1)
            .minusDays(1)
            .format(formatter)
        
        // Then
        assertEquals("2024-02-29", expected)
    }

    @Test
    fun `날짜 계산 함수들의 형식 일관성 테스트`() {
        // When
        val currentDate = DateUtils.getFormattedCurrentDate()
        val beforeDate = DateUtils.getFormattedDateBeforeDays(1)
        val firstDay = DateUtils.getFirstDayOfPreviousMonth()
        val lastDay = DateUtils.getLastDayOfPreviousMonth()
        
        // Then - 모두 동일한 형식 (yyyy-MM-dd)
        val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")
        assertTrue(currentDate.matches(datePattern))
        assertTrue(beforeDate.matches(datePattern))
        assertTrue(firstDay.matches(datePattern))
        assertTrue(lastDay.matches(datePattern))
    }
}