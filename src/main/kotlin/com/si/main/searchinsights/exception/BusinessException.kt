package com.si.main.searchinsights.exception

import com.si.main.searchinsights.enum.ErrorCode

open class BusinessException(
    val errorCode: ErrorCode,
    override val message: String? = null,
    override val cause: Throwable? = null
) : RuntimeException(message ?: errorCode.message, cause)