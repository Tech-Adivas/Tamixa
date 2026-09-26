package com.tamixa.infrastructure.notification

import com.tamixa.application.port.EmailSenderPort
import com.tamixa.infrastructure.logging.PiiMask
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Sends email via SendGrid API v3. Enabled when app.email.sendgrid-api-key is non-empty.
 * Falls back to LoggingEmailSender in dev when key is absent.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("!'${'$'}{app.email.sendgrid-api-key:}'.isEmpty()")
class SendGridEmailSender(
    @Qualifier("notificationRestTemplate") private val rest: RestTemplate,
    @Value("\${app.email.sendgrid-api-key}") private val apiKey: String,
    @Value("\${app.email.from-email:noreply@tamixa.in}") private val fromEmail: String,
    @Value("\${app.email.from-name:Tamixa}") private val fromName: String
) : EmailSenderPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = ObjectMapper()

    override fun sendMagicLinkOrCode(toEmail: String, magicLink: String, shortCode: String): Boolean {
        return try {
            val payload = mapOf(
                "personalizations" to listOf(mapOf("to" to listOf(mapOf("email" to toEmail)))),
                "from" to mapOf("email" to fromEmail, "name" to fromName),
                "subject" to "Your Tamixa sign-in code: $shortCode",
                "content" to listOf(
                    mapOf("type" to "text/plain", "value" to buildPlainText(shortCode, magicLink)),
                    mapOf("type" to "text/html", "value" to buildHtml(shortCode, magicLink))
                )
            )
            val body = mapper.writeValueAsString(payload)
            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $apiKey")
                contentType = MediaType.APPLICATION_JSON
            }
            rest.postForEntity(
                URI.create("https://api.sendgrid.com/v3/mail/send"),
                org.springframework.http.HttpEntity(body, headers),
                String::class.java
            )
            log.info("Magic link email sent to {}", PiiMask.maskEmail(toEmail))
            true
        } catch (e: Exception) {
            log.warn("Failed to send magic link email to {}: {}", PiiMask.maskEmail(toEmail), e.message)
            false
        }
    }

    private fun buildPlainText(shortCode: String, magicLink: String) = """
        Your Tamixa sign-in code is: $shortCode

        Or tap the link below to sign in instantly:
        $magicLink

        This code expires in 15 minutes. If you didn't request this, you can safely ignore this email.

        — The Tamixa Team
    """.trimIndent()

    private fun buildHtml(shortCode: String, magicLink: String) = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>Your Tamixa sign-in code</title>
        </head>
        <body style="margin:0;padding:0;background:#f5f5f5;font-family:Arial,Helvetica,sans-serif;">
          <table width="100%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:32px 0;">
            <tr>
              <td align="center">
                <table width="480" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                  <!-- Header -->
                  <tr>
                    <td style="background:#6c3fc5;padding:28px 32px;text-align:center;">
                      <span style="color:#ffffff;font-size:24px;font-weight:bold;letter-spacing:1px;">Tamixa</span>
                    </td>
                  </tr>
                  <!-- Body -->
                  <tr>
                    <td style="padding:36px 32px 24px;">
                      <p style="margin:0 0 8px;font-size:18px;font-weight:bold;color:#1a1a1a;">Your sign-in code</p>
                      <p style="margin:0 0 28px;font-size:14px;color:#555555;line-height:1.5;">
                        Use the code below or click the button to sign in to Tamixa.
                      </p>
                      <!-- Code block -->
                      <div style="background:#f0ebfa;border-radius:8px;padding:20px;text-align:center;margin-bottom:28px;">
                        <span style="font-size:36px;font-weight:bold;letter-spacing:10px;color:#6c3fc5;">$shortCode</span>
                      </div>
                      <!-- Magic link button -->
                      <div style="text-align:center;margin-bottom:28px;">
                        <a href="$magicLink"
                           style="display:inline-block;background:#6c3fc5;color:#ffffff;text-decoration:none;font-size:15px;font-weight:bold;padding:14px 32px;border-radius:8px;">
                          Sign in to Tamixa
                        </a>
                      </div>
                      <p style="margin:0;font-size:12px;color:#999999;line-height:1.5;text-align:center;">
                        This code expires in <strong>15 minutes</strong>.<br/>
                        If you didn't request this, you can safely ignore this email.
                      </p>
                    </td>
                  </tr>
                  <!-- Footer -->
                  <tr>
                    <td style="background:#fafafa;border-top:1px solid #eeeeee;padding:16px 32px;text-align:center;">
                      <p style="margin:0;font-size:11px;color:#bbbbbb;">
                        &copy; 2025 Tamixa &bull; noreply@tamixa.in
                      </p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}
