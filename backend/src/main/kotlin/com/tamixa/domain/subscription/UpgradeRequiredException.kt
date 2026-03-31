package com.tamixa.domain.subscription

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Thrown when user attempts to access premium content (e.g. premium voice)
 * without an active premium subscription.
 *
 * [ResponseStatus] ensures MockMvc and the default MVC resolver chain return 402 reliably.
 * [com.tamixa.api.exception.GlobalExceptionHandler] also maps this type for a consistent JSON [com.tamixa.api.exception.ErrorResponse] body in the servlet stack.
 */
@ResponseStatus(code = HttpStatus.PAYMENT_REQUIRED)
class UpgradeRequiredException(message: String = "Premium subscription required") : RuntimeException(message)
