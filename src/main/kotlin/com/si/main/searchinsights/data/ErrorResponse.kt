package com.si.main.searchinsights.data

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.si.main.searchinsights.enum.ErrorCode
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val code: String,
    val message: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val path: String? = null,
    val details: Map<String, Any>? = null
) {
    companion object {
        fun of(errorCode: ErrorCode, message: String? = null): ErrorResponse {
            return ErrorResponse(
                code = errorCode.code,
                message = message ?: errorCode.message
            )
        }
        
        fun of(errorCode: ErrorCode, message: String? = null, path: String? = null): ErrorResponse {
            return ErrorResponse(
                code = errorCode.code,
                message = message ?: errorCode.message,
                path = path
            )
        }
        
        fun of(errorCode: ErrorCode, message: String? = null, path: String? = null, details: Map<String, Any>? = null): ErrorResponse {
            return ErrorResponse(
                code = errorCode.code,
                message = message ?: errorCode.message,
                path = path,
                details = details
            )
        }
    }
}