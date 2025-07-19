package com.si.main.searchinsights.exception

import com.si.main.searchinsights.data.ErrorResponse
import com.si.main.searchinsights.enum.ErrorCode
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.format.DateTimeParseException

/**
 * GlobalExceptionHandler 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: 모든 테스트가 밀리초 단위로 실행
 * - Independent: 각 테스트가 독립적으로 실행
 * - Repeatable: 실행 환경에 관계없이 동일한 결과
 * - Self-Validating: 명확한 성공/실패 판단
 * - Timely: 예외 처리 로직과 함께 작성
 * 
 * BCC:
 * - Boundary: 다양한 예외 타입 경계 테스트
 * - Correct: 각 예외에 대한 올바른 응답 검증
 * - Conformance: HTTP 상태 코드 및 응답 형식 검증
 */
class GlobalExceptionHandlerTest {

    private lateinit var handler: GlobalExceptionHandler
    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
        request = mockk<HttpServletRequest>()
        every { request.requestURI } returns "/test/path"
    }

    @Test
    fun `BusinessException 처리 테스트`() {
        // given
        val exception = BusinessException(
            errorCode = ErrorCode.INVALID_DATE_FORMAT,
            message = "커스텀 메시지"
        )

        // when
        val response = handler.handleBusinessException(exception, request)

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("SI401", response.body?.code)
        assertEquals("커스텀 메시지", response.body?.message)
        assertEquals("/test/path", response.body?.path)
    }

    @Test
    fun `ExternalApiException 처리 테스트`() {
        // given
        val exception = ExternalApiException(
            errorCode = ErrorCode.GOOGLE_API_ERROR,
            message = "Google API 연동 실패"
        )

        // when
        val response = handler.handleExternalApiException(exception, request)

        // then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals("SI504", response.body?.code)
        assertEquals("Google API 연동 실패", response.body?.message)
    }

    @Test
    fun `IllegalArgumentException 처리 테스트`() {
        // given
        val exception = IllegalArgumentException("잘못된 입력값입니다")

        // when
        val response = handler.handleIllegalArgumentException(exception, request)

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("SI400", response.body?.code)
        assertEquals("잘못된 입력값입니다", response.body?.message)
    }

    @Test
    fun `DateTimeParseException 처리 테스트`() {
        // given
        val exception = DateTimeParseException("날짜 파싱 오류", "2024-13-45", 0)

        // when
        val response = handler.handleDateTimeParseException(exception, request)

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("SI401", response.body?.code)
        assertEquals("날짜 형식이 올바르지 않습니다 (yyyy-MM-dd)", response.body?.message)
    }

    @Test
    fun `FileNotFoundException - 인증 파일 처리 테스트`() {
        // given
        val exception = java.io.FileNotFoundException("credential/search-insights.json not found")

        // when
        val response = handler.handleFileNotFoundException(exception, request)

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("SI405", response.body?.code)
    }

    @Test
    fun `FileNotFoundException - 일반 파일 처리 테스트`() {
        // given
        val exception = java.io.FileNotFoundException("some-file.txt not found")

        // when
        val response = handler.handleFileNotFoundException(exception, request)

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("SI404", response.body?.code)
    }

    @Test
    fun `일반 Exception 처리 테스트`() {
        // given
        val exception = RuntimeException("예상치 못한 오류")

        // when
        val response = handler.handleGeneralException(exception, request)

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("SI500", response.body?.code)
        assertEquals("서버 내부 오류가 발생했습니다", response.body?.message)
    }

    @Test
    fun `ErrorResponse timestamp 포함 확인`() {
        // given
        val exception = BusinessException(ErrorCode.INTERNAL_ERROR)

        // when
        val response = handler.handleBusinessException(exception, request)

        // then
        assertNotNull(response.body?.timestamp)
    }
}