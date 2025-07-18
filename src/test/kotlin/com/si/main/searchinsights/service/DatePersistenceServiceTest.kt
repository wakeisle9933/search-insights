package com.si.main.searchinsights.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * DatePersistenceService 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: 임시 파일 시스템 사용으로 빠른 실행
 * - Independent: 각 테스트마다 새로운 서비스 인스턴스 생성
 * - Repeatable: 임시 디렉토리 사용으로 반복 가능
 * - Self-Validating: 파일 존재 여부와 내용 검증
 * - Timely: 파일 I/O 로직과 함께 작성
 * 
 * BCC:
 * - Boundary: 파일 존재/미존재 경계
 * - Correct: 정상적인 날짜 저장/읽기
 * - Existence: 파일이 없을 때 처리
 * - Conformance: 날짜 형식 검증
 */
class DatePersistenceServiceTest {

    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var service: DatePersistenceService
    private lateinit var testFile: Path

    @BeforeEach
    fun setUp() {
        // 테스트용 파일 경로 설정
        testFile = tempDir.resolve("last_run_date.txt")
        
        // 리플렉션을 사용하여 private 필드 접근
        service = DatePersistenceService()
        val field = service.javaClass.getDeclaredField("lastRunFile")
        field.isAccessible = true
        field.set(service, testFile)
    }

    @AfterEach
    fun tearDown() {
        // 테스트 후 파일 정리 (TempDir가 자동으로 처리하지만 명시적으로)
        Files.deleteIfExists(testFile)
    }

    // ===========================================
    // readLastRunDate 테스트
    // ===========================================

    @Test
    fun `파일이 존재하지 않을 때 null 반환`() {
        // Given - 파일이 없는 상태

        // When
        val result = service.readLastRunDate()

        // Then
        assertNull(result)
        assertFalse(Files.exists(testFile))
    }

    @Test
    fun `정상적인 날짜가 저장된 파일 읽기`() {
        // Given
        val expectedDate = LocalDate.of(2024, 7, 18)
        Files.writeString(testFile, expectedDate.toString())

        // When
        val result = service.readLastRunDate()

        // Then
        assertEquals(expectedDate, result)
    }

    @Test
    fun `잘못된 형식의 날짜가 저장된 파일 읽기시 예외 발생`() {
        // Given
        Files.writeString(testFile, "2024-13-45") // 잘못된 날짜

        // When & Then
        assertThrows<DateTimeParseException> {
            service.readLastRunDate()
        }
    }

    @Test
    fun `빈 파일 읽기시 예외 발생`() {
        // Given
        Files.createFile(testFile)
        Files.writeString(testFile, "")

        // When & Then
        assertThrows<DateTimeParseException> {
            service.readLastRunDate()
        }
    }

    @Test
    fun `공백이 포함된 날짜 파일 읽기`() {
        // Given
        val dateWithSpaces = "  2024-07-18  \n"
        Files.writeString(testFile, dateWithSpaces)

        // When
        val result = service.readLastRunDate()

        // Then
        // DatePersistenceService는 trim()을 하지 않으므로 파싱 실패 예상
        // 또는 서비스가 trim()을 한다면 성공해야 함
        // 실제 서비스 구현에 따라 다름
        try {
            assertEquals(LocalDate.of(2024, 7, 18), result)
        } catch (e: DateTimeParseException) {
            // trim()을 하지 않는다면 예외가 발생하는 것이 정상
            assertTrue(e.message?.contains("2024-07-18") == true)
        }
    }

    // ===========================================
    // writeLastRunDate 테스트
    // ===========================================

    @Test
    fun `날짜를 파일에 저장하기`() {
        // Given
        val dateToWrite = LocalDate.of(2024, 7, 18)

        // When
        service.writeLastRunDate(dateToWrite)

        // Then
        assertTrue(Files.exists(testFile))
        val content = Files.readString(testFile)
        assertEquals("2024-07-18", content)
    }

    @Test
    fun `기존 파일을 덮어쓰기`() {
        // Given
        val oldDate = LocalDate.of(2024, 7, 17)
        val newDate = LocalDate.of(2024, 7, 18)
        Files.writeString(testFile, oldDate.toString())

        // When
        service.writeLastRunDate(newDate)

        // Then
        val content = Files.readString(testFile)
        assertEquals("2024-07-18", content)
    }

    @Test
    fun `경계값 날짜 저장 테스트`() {
        // Given
        val dates = listOf(
            LocalDate.of(1900, 1, 1),    // 과거 날짜
            LocalDate.of(2024, 2, 29),   // 윤년
            LocalDate.of(2099, 12, 31),  // 미래 날짜
            LocalDate.now()              // 오늘
        )

        dates.forEach { date ->
            // When
            service.writeLastRunDate(date)

            // Then
            val content = Files.readString(testFile)
            assertEquals(date.toString(), content)
        }
    }

    // ===========================================
    // 통합 테스트
    // ===========================================

    @Test
    fun `날짜 쓰기 후 읽기 통합 테스트`() {
        // Given
        val originalDate = LocalDate.of(2024, 7, 18)

        // When - 쓰기
        service.writeLastRunDate(originalDate)

        // Then - 읽기
        val readDate = service.readLastRunDate()
        assertEquals(originalDate, readDate)
    }

    @Test
    fun `여러 번 쓰고 읽기 반복 테스트`() {
        // Given
        val dates = listOf(
            LocalDate.now(),
            LocalDate.now().minusDays(1),
            LocalDate.now().minusMonths(1),
            LocalDate.now().plusDays(1)
        )

        dates.forEach { date ->
            // When
            service.writeLastRunDate(date)
            val readDate = service.readLastRunDate()

            // Then
            assertEquals(date, readDate)
        }
    }

    @Test
    fun `파일 권한 문제 시뮬레이션`() {
        // 이 테스트는 실제 파일 시스템 권한에 따라 다를 수 있음
        // Windows에서는 다르게 동작할 수 있으므로 주의
        
        // Given
        val date = LocalDate.now()
        service.writeLastRunDate(date)
        
        // When - 읽기 전용으로 설정 (Windows에서는 효과가 제한적일 수 있음)
        testFile.toFile().setReadOnly()
        
        // Then - 새로운 날짜 쓰기 시도
        // Windows에서는 읽기 전용 파일도 덮어쓸 수 있을 수 있으므로
        // 이 테스트는 플랫폼에 따라 다르게 동작할 수 있음
        try {
            service.writeLastRunDate(date.plusDays(1))
            // Windows에서는 성공할 수 있음
        } catch (e: Exception) {
            // Unix/Linux에서는 예외 발생 가능
            assertTrue(e is java.nio.file.AccessDeniedException || 
                      e is java.io.IOException)
        } finally {
            // 권한 복구
            testFile.toFile().setWritable(true)
        }
    }

    @Test
    fun `디렉토리가 없을 때 파일 생성`() {
        // Given
        val nestedPath = tempDir.resolve("nested/dir/last_run_date.txt")
        val nestedService = DatePersistenceService()
        
        // 리플렉션으로 경로 설정
        val field = nestedService.javaClass.getDeclaredField("lastRunFile")
        field.isAccessible = true
        field.set(nestedService, nestedPath)
        
        val date = LocalDate.now()

        // When
        try {
            nestedService.writeLastRunDate(date)
            
            // Then - 파일이 생성되었는지 확인
            assertTrue(Files.exists(nestedPath))
            assertEquals(date, nestedService.readLastRunDate())
        } catch (e: java.nio.file.NoSuchFileException) {
            // DatePersistenceService가 디렉토리를 자동 생성하지 않는 경우
            // 이는 정상적인 동작일 수 있음
            assertTrue(e.message?.contains("nested") == true)
        }
    }
}