import com.araro.application.auth.AccountSuspendedException
import com.araro.application.auth.InvalidCredentialsException
import com.araro.application.subscription.SubscriptionNotFoundException
import com.araro.application.familyvoice.FamilyVoiceAccessDeniedException
import com.araro.application.familyvoice.FamilyVoiceFileTooLargeException
import com.araro.application.familyvoice.FamilyVoiceNotFoundException
import com.araro.application.familyvoice.InvalidVoiceFileException as FamilyInvalidVoiceFileException
import com.araro.application.avatar.AvatarFileTooLargeException
import com.araro.application.avatar.AvatarPremiumRequiredException
import com.araro.application.avatar.InvalidAvatarFileException
import com.araro.application.avatar.AvatarNotFoundException
import com.araro.domain.narration.NarrationJobCapacityExceededException
import com.araro.domain.subscription.UpgradeRequiredException
import com.araro.infrastructure.openai.OpenAIException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(e: InvalidCredentialsException): ResponseEntity<Map<String, Any>> {
        log.debug("Login failed: invalid credentials")
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to (e.message ?: "Invalid email or password")))
    }

    @ExceptionHandler(AccountSuspendedException::class)
    fun handleAccountSuspended(e: AccountSuspendedException): ResponseEntity<Map<String, Any>> {
        log.debug("Login failed: account suspended")
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to (e.message ?: "Account suspended")))
    }
}
