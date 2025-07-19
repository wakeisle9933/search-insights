package com.si.main.searchinsights.exception

import com.si.main.searchinsights.data.ErrorResponse
import com.si.main.searchinsights.enum.ErrorCode
import com.si.main.searchinsights.extension.logger
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.io.FileNotFoundException
import java.time.format.DateTimeParseException

@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val logger = logger()
    
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        e: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Business exception occurred: ${e.errorCode.code} - ${e.message}")
        
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ErrorResponse.of(
                errorCode = e.errorCode,
                message = e.message,
                path = request.requestURI
            ))
    }
    
    @ExceptionHandler(ExternalApiException::class)
    fun handleExternalApiException(
        e: ExternalApiException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("External API error: ${e.errorCode.code} - ${e.message}", e)
        
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ErrorResponse.of(
                errorCode = e.errorCode,
                message = e.message,
                path = request.requestURI
            ))
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        e: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Validation error: ${e.message}")
        
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(
                errorCode = ErrorCode.INVALID_INPUT,
                message = e.message,
                path = request.requestURI
            ))
    }
    
    @ExceptionHandler(DateTimeParseException::class)
    fun handleDateTimeParseException(
        e: DateTimeParseException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Date parsing error: ${e.message}")
        
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(
                errorCode = ErrorCode.INVALID_DATE_FORMAT,
                path = request.requestURI
            ))
    }
    
    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFoundException(
        e: FileNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("File not found: ${e.message}")
        
        val errorCode = when {
            e.message?.contains("credential") == true -> ErrorCode.CREDENTIAL_NOT_FOUND
            else -> ErrorCode.RESOURCE_NOT_FOUND
        }
        
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse.of(
                errorCode = errorCode,
                message = e.message,
                path = request.requestURI
            ))
    }
    
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(
        e: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Missing parameter: ${e.parameterName}")
        
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(
                errorCode = ErrorCode.MISSING_REQUIRED_PARAMETER,
                message = "필수 파라미터 '${e.parameterName}'이(가) 누락되었습니다",
                path = request.requestURI,
                details = mapOf("parameter" to e.parameterName)
            ))
    }
    
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        e: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Type mismatch: ${e.name} - ${e.value}")
        
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(
                errorCode = ErrorCode.INVALID_INPUT,
                message = "파라미터 '${e.name}'의 타입이 올바르지 않습니다",
                path = request.requestURI,
                details = mapOf(
                    "parameter" to e.name,
                    "invalidValue" to (e.value ?: "null"),
                    "expectedType" to (e.requiredType?.simpleName ?: "unknown")
                )
            ))
    }
    
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(
        e: NoResourceFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        // Chrome DevTools 관련 요청은 DEBUG 레벨로 로깅
        if (request.requestURI.contains(".well-known") || request.requestURI.contains("devtools")) {
            logger.debug("Chrome DevTools resource request: ${request.requestURI}")
        } else {
            logger.warn("Resource not found: ${request.requestURI}")
        }
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(
                errorCode = ErrorCode.RESOURCE_NOT_FOUND,
                message = "요청한 리소스를 찾을 수 없습니다: ${request.requestURI}",
                path = request.requestURI
            ))
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", e)
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(
                errorCode = ErrorCode.INTERNAL_ERROR,
                path = request.requestURI
            ))
    }
}