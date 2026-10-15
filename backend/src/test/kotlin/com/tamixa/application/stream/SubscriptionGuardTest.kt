package com.tamixa.application.stream

import com.tamixa.application.narration.NarrationPremiumVoiceValidator
import com.tamixa.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.domain.subscription.UpgradeRequiredException
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Streaming a parent's cloned voice ("cloned:{id}") needs a plan that includes voice cloning. */
class SubscriptionGuardTest {

    private val catalog: NarrationVoiceCatalogRepositoryPort = mock()
    private val validator: NarrationPremiumVoiceValidator = mock()
    private val guard = SubscriptionGuard(catalog, mock<SubscriptionService>(), validator)

    @Test
    fun `cloned voice on a plan with voice cloning is allowed`() {
        whenever(validator.canCloneVoice(8L)).thenReturn(true)

        assertThatCode { guard.enforcePremiumVoiceAccess("cloned:12", 8L) }.doesNotThrowAnyException()
        verify(catalog, never()).findByToneMode(any())
    }

    @Test
    fun `cloned voice on a plan without voice cloning needs an upgrade`() {
        whenever(validator.canCloneVoice(8L)).thenReturn(false)

        assertThatThrownBy { guard.enforcePremiumVoiceAccess("cloned:12", 8L) }
            .isInstanceOf(UpgradeRequiredException::class.java)
    }

    @Test
    fun `cloned voice without a parent needs an upgrade`() {
        assertThatThrownBy { guard.enforcePremiumVoiceAccess("cloned:12", null) }
            .isInstanceOf(UpgradeRequiredException::class.java)
    }

    @Test
    fun `default voice is always allowed`() {
        assertThatCode { guard.enforcePremiumVoiceAccess("default", null) }.doesNotThrowAnyException()
    }
}
