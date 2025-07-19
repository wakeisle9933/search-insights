package com.si.main.searchinsights.service

import com.si.main.searchinsights.enum.ReportFrequency
import com.si.main.searchinsights.util.DateUtils
import io.mockk.*
import jakarta.mail.internet.MimeMessage
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.core.io.InputStreamSource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import java.io.ByteArrayInputStream
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.jupiter.api.assertThrows
import io.mockk.just
import io.mockk.Runs
import io.mockk.slot

/**
 * MailService 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: Mock을 사용하여 실제 이메일 전송 없이 빠르게 실행
 * - Independent: 각 테스트는 독립적인 Mock 설정
 * - Repeatable: 외부 의존성 없이 반복 가능
 * - Self-Validating: 이메일 구성 요소 검증
 * - Timely: 이메일 전송 로직과 함께 작성
 * 
 * BCC:
 * - Boundary: 다양한 날짜 범위
 * - Correct: 정상적인 이메일 전송
 * - Existence: 첨부 파일 존재
 * - Cardinality: 단일 수신자
 * - Error: 메일 전송 실패 처리
 */
class MailServiceTest {

    private lateinit var service: MailService
    private lateinit var mockMailSender: JavaMailSender
    private lateinit var mockMimeMessage: MimeMessage
    
    private val receiverEmail = "test@example.com"

    @BeforeEach
    fun setUp() {
        mockMailSender = mockk(relaxed = true)
        mockMimeMessage = mockk(relaxed = true)
        
        every { mockMailSender.createMimeMessage() } returns mockMimeMessage
        
        service = MailService(mockMailSender, receiverEmail)
        
        // DateUtils object 모킹
        mockkObject(DateUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ===========================================
    // 정상 케이스 테스트
    // ===========================================

    @Test
    fun `일일 리포트 이메일 전송 테스트`() {
        // Given
        val testDate = "2024-07-18"
        every { DateUtils.getFormattedDateBeforeDays(3) } returns testDate
        
        val excelData = createTestExcelData()
        val fileName = "daily-report.xlsx"
        
        // When
        service.sendMail(excelData, fileName, ReportFrequency.DAILY)
        
        // Then - 메일 전송이 호출되었는지만 확인
        verify(exactly = 1) { mockMailSender.send(mockMimeMessage) }
        verify(exactly = 1) { mockMailSender.createMimeMessage() }
    }

    @Test
    fun `주간 리포트 이메일 전송 테스트`() {
        // Given
        val startDate = "2024-07-08"
        val endDate = "2024-07-15"
        every { DateUtils.getFormattedDateBeforeDays(10) } returns startDate
        every { DateUtils.getFormattedDateBeforeDays(3) } returns endDate
        
        val excelData = createTestExcelData()
        val fileName = "weekly-report.xlsx"
        
        // When
        service.sendMail(excelData, fileName, ReportFrequency.WEEKLY)
        
        // Then
        verify(exactly = 1) { mockMailSender.send(mockMimeMessage) }
    }

    @Test
    fun `월간 리포트 이메일 전송 테스트`() {
        // Given
        val firstDay = "2024-06-01"
        val lastDay = "2024-06-30"
        every { DateUtils.getFirstDayOfPreviousMonth() } returns firstDay
        every { DateUtils.getLastDayOfPreviousMonth() } returns lastDay
        
        val excelData = createTestExcelData()
        val fileName = "monthly-report.xlsx"
        
        // When
        service.sendMail(excelData, fileName, ReportFrequency.MONTHLY)
        
        // Then
        verify(exactly = 1) { mockMailSender.send(mockMimeMessage) }
    }

    @Test
    fun `커스텀 날짜 범위 리포트 이메일 전송 테스트`() {
        // Given
        val customStart = "2024-07-01"
        val customEnd = "2024-07-18"
        
        val excelData = createTestExcelData()
        val fileName = "custom-report.xlsx"
        
        // When
        service.sendMail(
            excelData, 
            fileName, 
            ReportFrequency.CUSTOM,
            customStartDate = customStart,
            customEndDate = customEnd
        )
        
        // Then
        verify(exactly = 1) { mockMailSender.send(mockMimeMessage) }
    }

    // ===========================================
    // Excel 수정 테스트
    // ===========================================

    @Test
    fun `Excel 파일에 날짜 범위 추가 테스트`() {
        // Given
        val testDate = "2024-07-18"
        every { DateUtils.getFormattedDateBeforeDays(3) } returns testDate
        
        val originalExcelData = createTestExcelData()
        
        // MimeMessageHelper mock
        mockkConstructor(MimeMessageHelper::class)
        val capturedAttachment = slot<InputStreamSource>()
        every { 
            anyConstructed<MimeMessageHelper>().addAttachment(any(), capture(capturedAttachment)) 
        } just Runs
        every { anyConstructed<MimeMessageHelper>().setSubject(any()) } just Runs
        every { anyConstructed<MimeMessageHelper>().setText(any<String>(), any<Boolean>()) } just Runs
        every { anyConstructed<MimeMessageHelper>().addBcc(any<String>()) } just Runs
        
        // When
        service.sendMail(originalExcelData, "test.xlsx", ReportFrequency.DAILY)
        
        // Then
        verify(exactly = 1) { mockMailSender.send(mockMimeMessage) }
        
        // 첨부파일이 수정되었는지 확인
        val attachmentStream = capturedAttachment.captured.inputStream
        val modifiedWorkbook = XSSFWorkbook(attachmentStream)
        val sheet = modifiedWorkbook.getSheetAt(0)
        val cellValue = sheet.getRow(1).getCell(0).stringCellValue
        assertTrue(cellValue.contains(testDate))
        modifiedWorkbook.close()
    }

    // ===========================================
    // 파라미터화 테스트
    // ===========================================

    @ParameterizedTest
    @EnumSource(value = ReportFrequency::class, names = ["CUSTOM"], mode = EnumSource.Mode.EXCLUDE)
    fun `다양한 리포트 빈도 테스트`(frequency: ReportFrequency) {
        // Given
        setupDateMocks(frequency)
        val excelData = createTestExcelData()
        
        // When
        service.sendMail(excelData, "report.xlsx", frequency)
        
        // Then
        verify(exactly = 1) { mockMailSender.send(mockMimeMessage) }
    }

    // ===========================================
    // 예외 처리 테스트
    // ===========================================

    @Test
    fun `메일 전송 실패시 예외 발생`() {
        // Given
        every { DateUtils.getFormattedDateBeforeDays(3) } returns "2024-07-18"
        every { mockMailSender.send(any<MimeMessage>()) } throws RuntimeException("Mail server error")
        
        val excelData = createTestExcelData()
        
        // When & Then
        try {
            service.sendMail(excelData, "report.xlsx", ReportFrequency.DAILY)
            fail("예외가 발생해야 함")
        } catch (e: RuntimeException) {
            assertEquals("Mail server error", e.message)
        }
    }

    @Test
    fun `빈 Excel 데이터 처리`() {
        // Given
        every { DateUtils.getFormattedDateBeforeDays(3) } returns "2024-07-18"
        val emptyExcelData = ByteArrayOutputStream()
        
        // When & Then - 빈 데이터로 예외 발생
        assertThrows<Exception> {
            service.sendMail(emptyExcelData, "empty.xlsx", ReportFrequency.DAILY)
        }
    }

    // ===========================================
    // 경계값 테스트
    // ===========================================

    @Test
    fun `매우 긴 파일명 처리`() {
        // Given
        every { DateUtils.getFormattedDateBeforeDays(3) } returns "2024-07-18"
        val longFileName = "a".repeat(255) + ".xlsx"
        val excelData = createTestExcelData()
        
        // When
        service.sendMail(excelData, longFileName, ReportFrequency.DAILY)
        
        // Then
        verify(exactly = 1) { mockMailSender.send(mockMimeMessage) }
    }

    @Test
    fun `특수 문자가 포함된 파일명 처리`() {
        // Given
        every { DateUtils.getFormattedDateBeforeDays(3) } returns "2024-07-18"
        val specialFileName = "report@#$%&().xlsx"
        val excelData = createTestExcelData()
        
        // When
        service.sendMail(excelData, specialFileName, ReportFrequency.DAILY)
        
        // Then
        verify(exactly = 1) { mockMailSender.send(mockMimeMessage) }
    }

    // ===========================================
    // 헬퍼 메서드
    // ===========================================

    private fun createTestExcelData(): ByteArrayOutputStream {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Test Sheet")
        
        // 헤더 행 생성
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Date Range")
        headerRow.createCell(1).setCellValue("Data")
        
        // 데이터 행 생성 - A2 셀이 수정될 위치
        val dataRow = sheet.createRow(1)
        dataRow.createCell(0).setCellValue("Original Value")
        dataRow.createCell(1).setCellValue("Test Data")
        
        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()
        
        return outputStream
    }

    private fun setupDateMocks(frequency: ReportFrequency) {
        when (frequency) {
            ReportFrequency.DAILY -> {
                every { DateUtils.getFormattedDateBeforeDays(3) } returns "2024-07-18"
            }
            ReportFrequency.WEEKLY -> {
                every { DateUtils.getFormattedDateBeforeDays(10) } returns "2024-07-08"
                every { DateUtils.getFormattedDateBeforeDays(3) } returns "2024-07-15"
            }
            ReportFrequency.MONTHLY -> {
                every { DateUtils.getFirstDayOfPreviousMonth() } returns "2024-06-01"
                every { DateUtils.getLastDayOfPreviousMonth() } returns "2024-06-30"
            }
            ReportFrequency.CUSTOM -> {
                // CUSTOM은 파라미터로 날짜를 받으므로 모킹 불필요
            }
        }
    }
}