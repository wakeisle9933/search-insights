package com.si.main.searchinsights.exception

import com.si.main.searchinsights.enum.ErrorCode

class EmailException(
    errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null
) : BusinessException(errorCode, message, cause)