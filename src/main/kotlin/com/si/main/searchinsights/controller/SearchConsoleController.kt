package com.si.main.searchinsights.controller

import com.si.main.searchinsights.enum.ErrorCode
import com.si.main.searchinsights.enum.ReportFrequency
import com.si.main.searchinsights.exception.BusinessException
import com.si.main.searchinsights.service.MailService
import com.si.main.searchinsights.service.SearchConsoleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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

        val excelFile = searchConsoleService.createExcelFile(
            searchConsoleService.fetchSearchAnalyticsData(dateRange.first, dateRange.second),
            searchConsoleService.fetchAnalyticsData(dateRange.first, dateRange.second),
            ReportFrequency.CUSTOM
        )

        mailService.sendMail(excelFile, "search_insights.xlsx", ReportFrequency.CUSTOM, dateRange.first, dateRange.second)
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

}