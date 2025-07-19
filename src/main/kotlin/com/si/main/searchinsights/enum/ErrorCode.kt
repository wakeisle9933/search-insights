package com.si.main.searchinsights.enum

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val message: String,
    val status: HttpStatus
) {
    // 400 Bad Request
    INVALID_INPUT("SI400", "입력값이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    INVALID_DATE_FORMAT("SI401", "날짜 형식이 올바르지 않습니다 (yyyy-MM-dd)", HttpStatus.BAD_REQUEST),
    DATE_RANGE_INVALID("SI402", "시작일이 종료일보다 늦을 수 없습니다", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_PARAMETER("SI403", "필수 파라미터가 누락되었습니다", HttpStatus.BAD_REQUEST),
    
    // 404 Not Found
    RESOURCE_NOT_FOUND("SI404", "요청한 리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    CREDENTIAL_NOT_FOUND("SI405", "인증 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    
    // 500 Internal Server Error
    INTERNAL_ERROR("SI500", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_PROCESSING_ERROR("SI501", "파일 처리 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    EXCEL_GENERATION_ERROR("SI502", "엑셀 파일 생성 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // 503 Service Unavailable
    EXTERNAL_API_ERROR("SI503", "외부 API 연동 중 오류가 발생했습니다", HttpStatus.SERVICE_UNAVAILABLE),
    GOOGLE_API_ERROR("SI504", "Google API 호출에 실패했습니다", HttpStatus.SERVICE_UNAVAILABLE),
    WORDPRESS_API_ERROR("SI505", "WordPress API 호출에 실패했습니다", HttpStatus.SERVICE_UNAVAILABLE),
    ANALYTICS_API_ERROR("SI506", "Analytics API 호출에 실패했습니다", HttpStatus.SERVICE_UNAVAILABLE),
    SEARCH_CONSOLE_API_ERROR("SI507", "Search Console API 호출에 실패했습니다", HttpStatus.SERVICE_UNAVAILABLE),
    
    // Email
    EMAIL_SEND_FAILED("SI601", "이메일 발송에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_ATTACHMENT_ERROR("SI602", "이메일 첨부파일 처리 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Data Processing
    DATA_PARSING_ERROR("SI701", "데이터 파싱 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    CATEGORY_SYNC_ERROR("SI702", "카테고리 동기화 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR)
}