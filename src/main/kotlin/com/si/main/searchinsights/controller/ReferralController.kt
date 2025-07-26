package com.si.main.searchinsights.controller

import com.si.main.searchinsights.data.ReferralTraffic
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
@Tag(name = "Referral Traffic API", description = "외부 유입(Referral) 트래픽 분석 API")
class ReferralController(
    private val searchConsoleService: SearchConsoleService
) {
    
    @Operation(
        summary = "Referral 트래픽 데이터 조회",
        description = "지정된 기간 동안의 외부 사이트에서 유입된 트래픽 데이터를 조회합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "성공적으로 데이터를 조회했습니다"),
        ApiResponse(responseCode = "400", description = "잘못된 요청입니다"),
        ApiResponse(responseCode = "500", description = "서버 오류가 발생했습니다")
    )
    @GetMapping("/referral-traffic")
    fun getReferralTraffic(
        @Parameter(description = "시작 날짜 (YYYY-MM-DD)", example = "2024-01-01", required = true)
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate,
        
        @Parameter(description = "종료 날짜 (YYYY-MM-DD)", example = "2024-01-31", required = true)
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate
    ): List<ReferralTraffic> {
        // 날짜 형식을 YYYY-MM-DD 문자열로 변환
        val startDateStr = startDate.toString()
        val endDateStr = endDate.toString()
        
        // SearchConsoleService의 fetchReferralTrafficData 메서드 호출
        return searchConsoleService.fetchReferralTrafficData(startDateStr, endDateStr)
    }
}