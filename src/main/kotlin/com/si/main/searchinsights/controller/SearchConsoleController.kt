package com.si.main.searchinsights.controller

import com.si.main.searchinsights.enum.ReportFrequency
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
        summary = "검색 인사이트 리포트 메일 발송",
        description = "지정된 기간의 검색 인사이트 리포트를 생성하고 메일로 발송"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "메일 발송 성공"
        ),
        ApiResponse(
            responseCode = "400",
            description = "잘못된 날짜 형식"
        )
    ])
    @GetMapping("/email-search-insights-report")
    fun sendDailyMailing(
        @Parameter(
            description = "시작일 (yyyy-MM-dd)",
            example = "2024-01-20",
            schema = Schema(
                type = "string",
                pattern = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$"
            )
        )
        @RequestParam(required = false) fromDate: String? = null,

        @Parameter(
            description = "종료일 (yyyy-MM-dd)",
            example = "2024-02-20",
            schema = Schema(
                type = "string",
                pattern = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$"
            )
        )
        @RequestParam(required = false) toDate: String? = null
    ) {
        // 여기에 실제 날짜 유효성 검사도 추가하면 좋을 것 같아!
        if (fromDate != null && toDate != null) {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            try {
                val parsedFromDate = LocalDate.parse(fromDate, dateFormatter)
                val parsedToDate = LocalDate.parse(toDate, dateFormatter)

                if (parsedFromDate.isAfter(parsedToDate)) {
                    throw IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다!")
                }
            } catch (e: DateTimeParseException) {
                throw IllegalArgumentException("날짜 형식이 올바르지 않습니다! yyyy-MM-dd 형식으로 입력해주세요!")
            }
        }
        val excelFile = if (fromDate != null && toDate != null) {
            searchConsoleService.createExcelFile(
                searchConsoleService.fetchSearchAnalyticsData(fromDate, toDate), searchConsoleService.fetchAnalyticsData(fromDate, toDate), ReportFrequency.CUSTOM
            )
        } else {
            searchConsoleService.createExcelFile(
                searchConsoleService.fetchSearchAnalyticsData(), searchConsoleService.fetchAnalyticsData(fromDate, toDate), ReportFrequency.CUSTOM
            )
        }
        mailService.sendMail(excelFile, "search_insights.xlsx", ReportFrequency.CUSTOM, fromDate, toDate)
    }

}

