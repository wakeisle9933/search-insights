package com.si.main.searchinsights.service

import com.google.api.services.searchconsole.v1.model.ApiDataRow
import com.si.main.searchinsights.data.PageViewInfo
import com.si.main.searchinsights.data.PrefixSummary
import com.si.main.searchinsights.data.PrefixAnalyticsSummary
import com.si.main.searchinsights.data.Backlink
import io.mockk.*
import okhttp3.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.IndexedColors
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * SpreadSheetService 테스트 클래스
 * 
 * FIRST 원칙:
 * - Fast: Mock을 사용하여 외부 API 호출 없이 빠르게 실행
 * - Independent: 각 테스트는 독립적인 Excel 워크북 생성
 * - Repeatable: 동일한 입력에 대해 동일한 결과
 * - Self-Validating: Excel 시트 생성 및 데이터 검증
 * - Timely: Excel 생성 로직과 함께 작성
 * 
 * BCC:
 * - Boundary: 빈 데이터, 대용량 데이터
 * - Correct: 정상적인 Excel 생성
 * - Existence: 시트 존재 여부
 * - Cardinality: 단일/다수 행 처리
 * - Error: API 오류 처리
 */
class SpreadSheetServiceTest {

    private lateinit var service: SpreadSheetService
    private lateinit var mockOkHttpClient: OkHttpClient
    
    private val fullDomain = "https://test.com"
    private val baseDomain = "test.com"
    private val rapidApiKey = "test-api-key"

    @BeforeEach
    fun setUp() {
        mockOkHttpClient = mockk(relaxed = true)
        service = SpreadSheetService(fullDomain, baseDomain, rapidApiKey)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ===========================================
    // Raw Data Sheet 테스트
    // ===========================================

    @Test
    fun `Search Console Raw Data 시트 생성 - 정상 케이스`() {
        // Given
        val workbook = XSSFWorkbook()
        val testData = listOf(
            createApiDataRow("kotlin tutorial", "/page1", 100.0, 1000.0, 0.1, 1.5),
            createApiDataRow("spring boot guide", "/page2", 50.0, 500.0, 0.1, 2.0)
        )
        
        // When
        val sheet = service.createRawDataSheet(workbook, testData)
        
        // Then
        assertNotNull(sheet)
        assertEquals("Search Console Raw Data", sheet.sheetName)
        
        // 헤더 확인
        val headerRow = sheet.getRow(3)
        assertEquals("Query", headerRow.getCell(0).stringCellValue)
        assertEquals("Position", headerRow.getCell(1).stringCellValue)
        assertEquals("Clicks", headerRow.getCell(2).stringCellValue)
        
        // 데이터 확인
        val dataRow1 = sheet.getRow(4)
        assertEquals("kotlin tutorial", dataRow1.getCell(0).stringCellValue)
        assertEquals(100.0, dataRow1.getCell(2).numericCellValue)
    }

    @Test
    fun `빈 데이터로 시트 생성`() {
        // Given
        val workbook = XSSFWorkbook()
        val emptyData = emptyList<ApiDataRow>()
        
        // When
        val sheet = service.createRawDataSheet(workbook, emptyData)
        
        // Then
        assertNotNull(sheet)
        assertEquals("Search Console Raw Data", sheet.sheetName)
        // 빈 데이터에서도 헤더는 생성되어야 함
        val headerRow = sheet.getRow(3)
        assertNotNull(headerRow)
    }

    @Test
    fun `중복 쿼리 제거 테스트`() {
        // Given
        val workbook = XSSFWorkbook()
        val duplicateData = listOf(
            createApiDataRow("kotlin tutorial", "/page1", 100.0, 1000.0, 0.1, 1.5),
            createApiDataRow("kotlin tutorial", "/page2", 50.0, 1500.0, 0.1, 2.0), // 더 높은 impressions
            createApiDataRow("spring boot", "/page3", 30.0, 300.0, 0.1, 3.0)
        )
        
        // When
        val sheet = service.createRawDataSheet(workbook, duplicateData)
        
        // Then
        // 중복 제거 후 2개의 데이터 행만 있어야 함 (헤더 제외)
        var dataRowCount = 0
        for (i in 4..sheet.lastRowNum) {
            if (sheet.getRow(i) != null) {
                dataRowCount++
            }
        }
        assertEquals(2, dataRowCount)
    }

    // ===========================================
    // Prefix Summary 테스트
    // ===========================================

    @Test
    fun `접두어 요약 시트 생성`() {
        // Given
        val workbook = XSSFWorkbook()
        val testData = listOf(
            createApiDataRow("kotlin tutorial basic", "/page1", 100.0, 1000.0, 0.1, 1.5),
            createApiDataRow("kotlin tutorial advanced", "/page2", 50.0, 500.0, 0.1, 2.0),
            createApiDataRow("java guide", "/page3", 30.0, 300.0, 0.1, 3.0)
        )
        
        // When
        val sheets = service.createPrefixSummarySheet(workbook, testData)
        
        // Then
        assertTrue(sheets.isNotEmpty())
        val firstSheet = sheets.first()
        assertEquals("Prefix GSC Summary (1w)", firstSheet.sheetName)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    fun `접두어 추출 테스트`(wordCount: Int) {
        // Given
        val query = "kotlin spring boot tutorial guide"
        
        // When
        val prefix = service.getPrefix(query, wordCount)
        
        // Then
        val expectedWords = query.split(" ").take(wordCount).joinToString(" ")
        assertEquals(expectedWords, prefix)
    }

    @Test
    fun `접두어별 그룹화 테스트`() {
        // Given
        val testData = listOf(
            createApiDataRow("kotlin tutorial", "/page1", 100.0, 1000.0, 0.1, 1.5),
            createApiDataRow("kotlin guide", "/page2", 50.0, 500.0, 0.1, 2.0),
            createApiDataRow("java tutorial", "/page3", 30.0, 300.0, 0.1, 3.0)
        )
        
        // When
        val grouped = service.groupByPrefix(testData, 1)
        
        // Then
        assertEquals(2, grouped.size) // "kotlin"과 "java"
        assertEquals(2, grouped["kotlin"]?.size) // kotlin으로 시작하는 2개
        assertEquals(1, grouped["java"]?.size)   // java로 시작하는 1개
    }

    @Test
    fun `접두어 요약 계산 테스트`() {
        // Given
        val groupedData = mapOf(
            "kotlin" to listOf(
                createApiDataRow("kotlin tutorial", "/page1", 100.0, 1000.0, 0.1, 1.5),
                createApiDataRow("kotlin guide", "/page2", 50.0, 500.0, 0.1, 2.0)
            )
        )
        
        // When
        val summary = service.calculatePrefixSummary(groupedData)
        
        // Then
        assertEquals(1, summary.size)
        val kotlinSummary = summary.first()
        assertEquals("kotlin", kotlinSummary.prefix)
        assertEquals(150.0, kotlinSummary.totalClicks) // 100 + 50
        assertEquals(1500.0, kotlinSummary.totalImpressions) // 1000 + 500
        assertTrue(kotlinSummary.avgPosition > 0) // 평균 포지션 확인
    }

    // ===========================================
    // Analytics Data Sheet 테스트
    // ===========================================

    @Test
    fun `Analytics Raw Data 시트 생성`() {
        // Given
        val workbook = XSSFWorkbook()
        val testData = listOf(
            PageViewInfo("/page1", "Page 1 Title", 100.0),
            PageViewInfo("/page2", "Page 2 Title", 200.0)
        )
        
        // When
        val sheet = service.createRawAnalyticsDataSheet(workbook, testData)
        
        // Then
        assertNotNull(sheet)
        assertEquals("Google Analytics Raw Data", sheet.sheetName)
        
        // 데이터 확인 - 데이터는 row 4부터 시작
        val dataRow1 = sheet.getRow(4)
        assertNotNull(dataRow1)
        assertEquals("Page 1 Title", dataRow1.getCell(0).stringCellValue)
        assertEquals("100.0", dataRow1.getCell(1).stringCellValue) // pageViews는 문자열로 저장됨
    }

    @Test
    fun `Analytics 접두어 요약 시트 생성`() {
        // Given
        val workbook = XSSFWorkbook()
        val testData = listOf(
            PageViewInfo("/kotlin/tutorial", "Kotlin Tutorial", 100.0),
            PageViewInfo("/kotlin/guide", "Kotlin Guide", 50.0),
            PageViewInfo("/java/tutorial", "Java Tutorial", 30.0)
        )
        
        // When
        val sheets = service.createPrefixAnalyticsSummarySheet(workbook, testData)
        
        // Then
        assertTrue(sheets.isNotEmpty())
        val firstSheet = sheets.first()
        assertEquals("Prefix GA Summary (1w)", firstSheet.sheetName)
    }

    // ===========================================
    // High Impressions Low Position 테스트
    // ===========================================

    @Test
    fun `높은 노출 낮은 순위 시트 생성`() {
        // Given
        val workbook = XSSFWorkbook()
        val testData = listOf(
            createApiDataRow("popular keyword", "/page1", 10.0, 10000.0, 0.001, 15.0),
            createApiDataRow("normal keyword", "/page2", 100.0, 1000.0, 0.1, 5.0)
        )
        
        // When
        val sheet = service.createHighImpressionsLowPositionSheet(workbook, testData)
        
        // Then
        assertNotNull(sheet)
        assertEquals("Potential Hits", sheet.sheetName)
        
        // 첫 번째 데이터가 높은 노출, 낮은 순위 조건에 맞음
        val dataRow1 = sheet.getRow(1)
        assertNotNull(dataRow1)
        assertEquals("popular keyword", dataRow1.getCell(0).stringCellValue)
    }

    // ===========================================
    // 스타일 테스트
    // ===========================================

    @Test
    fun `헤더 스타일 생성 테스트`() {
        // Given
        val workbook = XSSFWorkbook()
        
        // When
        val headerStyle = service.createHeaderStyle(workbook)
        
        // Then
        assertNotNull(headerStyle)
        assertTrue(headerStyle.font.bold)
        assertEquals(IndexedColors.GREY_25_PERCENT.index, headerStyle.fillForegroundColor)
    }

    // ===========================================
    // 대용량 데이터 처리 테스트
    // ===========================================

    @Test
    fun `대용량 데이터 처리 성능 테스트`() {
        // Given
        val workbook = XSSFWorkbook()
        val largeData = (1..1000).map { i ->
            createApiDataRow("keyword $i", "/page$i", i.toDouble(), i * 10.0, 0.1, (i % 20).toDouble())
        }
        
        // When
        val startTime = System.currentTimeMillis()
        val sheet = service.createRawDataSheet(workbook, largeData)
        val endTime = System.currentTimeMillis()
        
        // Then
        assertNotNull(sheet)
        assertTrue((endTime - startTime) < 5000) // 5초 이내 처리
        
        // 모든 데이터가 정상적으로 생성되었는지 확인
        val lastDataRow = sheet.getRow(sheet.lastRowNum)
        assertNotNull(lastDataRow)
    }

    // ===========================================
    // 헬퍼 메서드
    // ===========================================

    private fun createApiDataRow(
        query: String,
        page: String,
        clicks: Double,
        impressions: Double,
        ctr: Double,
        position: Double
    ): ApiDataRow {
        return ApiDataRow().apply {
            setKeys(listOf(query, page))
            setClicks(clicks)
            setImpressions(impressions)
            setCtr(ctr)
            setPosition(position)
        }
    }
}