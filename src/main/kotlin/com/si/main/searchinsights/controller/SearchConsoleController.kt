package com.si.main.searchinsights.controller

import com.si.main.searchinsights.data.PrefixSummary
import com.si.main.searchinsights.enum.ErrorCode
import com.si.main.searchinsights.enum.ReportFrequency
import com.si.main.searchinsights.exception.BusinessException
import com.si.main.searchinsights.extension.logger
import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
class SearchConsoleController(
    private val mailService: MailService,
    private val searchConsoleService: SearchConsoleService,
) {
    
    private val logger = logger()

    @Operation(
        summary = "Search Insight Custom Report 메일 발송",
        description = "지정된 기간의 검색 인사이트 리포트를 생성하고 메일로 발송 (기본값: 오늘로부터 3일 전)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "메일 발송 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 날짜 형식")
    ])
    @GetMapping("/email-search-insights-report")
    fun sendDailyMailing(
        @Parameter(
            description = "시작일 (yyyy-MM-dd)",
            example = "2025-02-01",  // 예시값
            schema = Schema(
                type = "string",
                pattern = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$"
            )
        )
        @RequestParam(required = false) fromDate: String? = null,

        @Parameter(
            description = "종료일 (yyyy-MM-dd)",
            example = "2025-02-01",  // 예시값
            schema = Schema(
                type = "string",
                pattern = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$"
            )
        )
        @RequestParam(required = false) toDate: String? = null
    ) {
        // 기본값 설정 - 날짜 값이 null이면 3일 전 날짜 사용
        val defaultDate = LocalDate.now().minusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val effectiveFromDate = fromDate ?: defaultDate
        val effectiveToDate = toDate ?: defaultDate

        // 날짜 유효성 검사
        val dateRange = validateDateRange(effectiveFromDate, effectiveToDate)

        // 🚀 병렬 API 호출로 성능 2배 향상!
        val (searchAnalyticsData, analyticsData) = runBlocking {
            val searchDataDeferred = async { 
                searchConsoleService.fetchSearchAnalyticsData(dateRange.first, dateRange.second) 
            }
            val analyticsDataDeferred = async { 
                searchConsoleService.fetchAnalyticsData(dateRange.first, dateRange.second) 
            }
            
            // 두 API 호출이 동시에 실행되고 결과를 기다림
            Pair(searchDataDeferred.await(), analyticsDataDeferred.await())
        }

        // 🔍 데이터 체크 - 둘 다 비어있으면 이메일 발송 안 함!
        if (searchAnalyticsData.isEmpty() && analyticsData.isEmpty()) {
            logger.warn("📭 데이터가 없어서 이메일 발송을 건너뜁니다. (기간: ${dateRange.first} ~ ${dateRange.second})")
            throw BusinessException(
                errorCode = ErrorCode.NO_DATA_AVAILABLE,
                message = "선택한 기간에 데이터가 없습니다. 이메일이 발송되지 않았습니다."
            )
        }
        
        val excelFile = searchConsoleService.createExcelFile(
            searchAnalyticsData,
            analyticsData,
            ReportFrequency.CUSTOM
        )

        mailService.sendMail(excelFile, "search_insights.xlsx", ReportFrequency.CUSTOM, dateRange.first, dateRange.second)
        logger.info("✅ 이메일 발송 완료! 데이터 수: Search Console ${searchAnalyticsData.size}개, Analytics ${analyticsData.size}개")
    }
    
    private fun validateDateRange(fromDate: String, toDate: String): Pair<String, String> {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        val parsedFromDate = try {
            LocalDate.parse(fromDate, dateFormatter)
        } catch (e: DateTimeParseException) {
            throw BusinessException(
                errorCode = ErrorCode.INVALID_DATE_FORMAT,
                message = "시작일의 날짜 형식이 올바르지 않습니다: $fromDate"
            )
        }
        
        val parsedToDate = try {
            LocalDate.parse(toDate, dateFormatter)
        } catch (e: DateTimeParseException) {
            throw BusinessException(
                errorCode = ErrorCode.INVALID_DATE_FORMAT,
                message = "종료일의 날짜 형식이 올바르지 않습니다: $toDate"
            )
        }
        
        if (parsedFromDate.isAfter(parsedToDate)) {
            throw BusinessException(
                errorCode = ErrorCode.DATE_RANGE_INVALID
            )
        }
        
        return Pair(fromDate, toDate)
    }
    
    @Operation(
        summary = "Search Console 유입 쿼리 조회",
        description = "지정된 기간의 검색 쿼리 및 페이지 정보를 조회 (기본값: 오늘로부터 3일 전)"
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 날짜 형식")
    ])
    @GetMapping("/api/search-queries")
    fun getSearchQueries(
        @Parameter(
            description = "시작일 (yyyy-MM-dd)",
            example = "2025-02-01",
            schema = Schema(
                type = "string",
                pattern = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$"
            )
        )
        @RequestParam(required = false) fromDate: String? = null,

        @Parameter(
            description = "종료일 (yyyy-MM-dd)",
            example = "2025-02-01",
            schema = Schema(
                type = "string",
                pattern = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$"
            )
        )
        @RequestParam(required = false) toDate: String? = null
    ): SearchQueryResponse {
        // 기본값 설정 - 날짜 값이 null이면 3일 전 날짜 사용
        val defaultDate = LocalDate.now().minusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val effectiveFromDate = fromDate ?: defaultDate
        val effectiveToDate = toDate ?: defaultDate

        // 날짜 유효성 검사
        val dateRange = validateDateRange(effectiveFromDate, effectiveToDate)

        // Search Console API 호출
        val searchAnalyticsData = searchConsoleService.fetchSearchAnalyticsData(dateRange.first, dateRange.second)

        // 데이터 체크
        if (searchAnalyticsData.isEmpty()) {
            logger.warn("📭 선택한 기간에 데이터가 없습니다. (기간: ${dateRange.first} ~ ${dateRange.second})")
            return SearchQueryResponse(
                summary = SearchQuerySummary(0, 0, 0, 0.0),
                queries = emptyList(),
                prefixSummaries = emptyList()
            )
        }

        // 중복 제거 (SpreadSheetService의 로직과 동일)
        val uniqueQueries = searchAnalyticsData.asSequence()
            .groupBy { it.getKeys()[0] }  // query로 그룹화
            .map { (_, rows) -> 
                rows.maxByOrNull { it.impressions } ?: rows.first()  // 가장 높은 impressions 유지
            }
            .toList()

        // 항상 전체 데이터를 반환 (클라이언트에서 접두어 분석 처리)
        // 전체 데이터 기준 요약 정보 계산
        val totalQueries = uniqueQueries.size
        val totalClicks = uniqueQueries.sumOf { it.clicks.toInt() }
        val totalImpressions = uniqueQueries.sumOf { it.impressions.toInt() }
        val avgPosition = if (uniqueQueries.isEmpty()) 0.0 else 
            uniqueQueries.map { it.position }.average()

        // 전체 데이터를 변환하여 반환 (클라이언트에서 접두어 분석을 위해)
        val allQueries = uniqueQueries.asSequence()
            .map { row ->
                SearchQueryData(
                    query = row.getKeys()[0],
                    position = kotlin.math.floor(row.position * 100) / 100,
                    clicks = row.clicks.toInt(),
                    impressions = row.impressions.toInt(),
                    ctr = kotlin.math.floor(row.ctr * 100) / 100,
                    pageLink = row.getKeys()[1]
                )
            }
            .sortedByDescending { it.impressions }  // impressions 기준 내림차순 정렬
            .toList()  // 전체 데이터

        return SearchQueryResponse(
            summary = SearchQuerySummary(
                totalQueries = totalQueries,
                totalClicks = totalClicks,
                totalImpressions = totalImpressions,
                avgPosition = kotlin.math.floor(avgPosition * 10) / 10
            ),
            queries = allQueries,
            prefixSummaries = emptyList()
        )
    }

}

data class SearchQueryResponse(
    val summary: SearchQuerySummary,
    val queries: List<SearchQueryData>,
    val prefixSummaries: List<PrefixSummary> = emptyList()
)

data class SearchQuerySummary(
    val totalQueries: Int,
    val totalClicks: Int,
    val totalImpressions: Int,
    val avgPosition: Double
)

data class SearchQueryData(
    val query: String,
    val position: Double,
    val clicks: Int,
    val impressions: Int,
    val ctr: Double,
    val pageLink: String
)