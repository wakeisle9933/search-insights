package com.si.main.searchinsights.controller

import com.si.main.searchinsights.data.PageFlowData
import com.si.main.searchinsights.data.PageFlowSource
import com.si.main.searchinsights.data.PageFlowDestination
import com.si.main.searchinsights.service.SearchConsoleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@Tag(name = "Flow Analysis API", description = "사용자 플로우 분석 API")
class FlowController(
    private val searchConsoleService: SearchConsoleService
) {
    
    @Operation(
        summary = "페이지 플로우 분석",
        description = "특정 페이지의 유입 경로(이전 페이지)와 이탈 경로(다음 페이지)를 분석합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "성공적으로 플로우 데이터를 조회했습니다"),
        ApiResponse(responseCode = "400", description = "잘못된 요청입니다"),
        ApiResponse(responseCode = "500", description = "서버 오류가 발생했습니다")
    )
    @GetMapping("/page-flow")
    fun getPageFlow(
        @Parameter(description = "분석할 페이지 경로", example = "/blog/post-1", required = true)
        @RequestParam pagePath: String,
        
        @Parameter(description = "시작 날짜 (YYYY-MM-DD)", example = "2024-01-01", required = true)
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate,
        
        @Parameter(description = "종료 날짜 (YYYY-MM-DD)", example = "2024-01-31", required = true)
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate
    ): PageFlowData {
        // 날짜 형식을 YYYY-MM-DD 문자열로 변환
        val startDateStr = startDate.toString()
        val endDateStr = endDate.toString()
        
        // SearchConsoleService의 fetchPageFlowData 메서드 호출
        return searchConsoleService.fetchPageFlowData(pagePath, startDateStr, endDateStr)
    }
}