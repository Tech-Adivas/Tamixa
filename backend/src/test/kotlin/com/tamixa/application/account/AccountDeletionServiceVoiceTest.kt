package com.tamixa.application.account

import com.tamixa.application.port.AccountDeletionPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.TokenRevocationPort
import com.tamixa.application.voice.ParentVoiceData
import com.tamixa.application.voice.VoiceCloningService
import com.tamixa.domain.Parent
import com.tamixa.domain.Role
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Deleting an account also removes the parent's voice recordings and provider voices, after the rows are gone. */
class AccountDeletionServiceVoiceTest {

    private val email = "parent@test.com"
    private val port: AccountDeletionPort = mock()
    private val voiceCloningService: VoiceCloningService = mock()
    private val voiceData = ParentVoiceData(setOf("voices/8/a.m4a"), setOf("el-1"), emptySet())
    private val service: AccountDeletionService

    init {
        val parent = mock<Parent> {
            on { id } doReturn 8L
            on { role } doReturn Role.PARENT
            on { suspendedAt } doReturn null
        }
        val parents = mock<ParentRepositoryPort> { on { findByEmail(email) } doReturn parent }
        whenever(voiceCloningService.collectVoiceData(8L)).thenReturn(voiceData)
        service = AccountDeletionService(port, parents, mock<TokenRevocationPort>(), voiceCloningService)
    }

    @Test
    fun `voice data is collected before and purged after the account rows are deleted`() {
        whenever(port.deleteAccount(email)).thenReturn(true)

        service.deleteAccount(email)

        inOrder(voiceCloningService, port) {
            verify(voiceCloningService).collectVoiceData(8L)
            verify(port).deleteAccount(email)
            verify(voiceCloningService).purgeVoiceData(8L, voiceData)
        }
    }

    @Test
    fun `nothing is purged when the account could not be deleted`() {
        whenever(port.deleteAccount(email)).thenReturn(false)

        assertThatThrownBy { service.deleteAccount(email) }.isInstanceOf(AccountDeletionException::class.java)

        verify(voiceCloningService, never()).purgeVoiceData(any(), any())
    }
}
