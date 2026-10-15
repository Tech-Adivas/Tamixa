package com.tamixa.application.avatar

import com.tamixa.application.port.FamilyAvatarStoragePort
import com.tamixa.application.port.ParentAvatarRepositoryPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.domain.Parent
import com.tamixa.domain.ParentAvatar
import com.tamixa.domain.Role
import com.tamixa.domain.Subscription
import com.tamixa.infrastructure.config.AppProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import java.time.Instant

class FamilyAvatarServicePremiumTest {

    private val parent = Parent(id = 7L, email = "parent@example.com", passwordHash = "x", role = Role.PARENT, createdAt = Instant.EPOCH)
    private val image = byteArrayOf(1, 2, 3)

    private val parentAvatarRepository: ParentAvatarRepositoryPort = mock()
    private val parentRepository: ParentRepositoryPort = mock()
    private val subscriptionService: SubscriptionService = mock()
    private val storage: FamilyAvatarStoragePort = mock()

    private fun service(allowFreeAvatarUpload: Boolean): FamilyAvatarService {
        val storageProvider: ObjectProvider<FamilyAvatarStoragePort> = mock()
        whenever(storageProvider.getIfAvailable()).thenReturn(storage)
        whenever(parentRepository.findByEmail(parent.email)).thenReturn(parent)
        whenever(parentAvatarRepository.findByParentId(parent.id)).thenReturn(null)
        whenever(parentAvatarRepository.save(any())).thenAnswer { it.arguments[0] as ParentAvatar }
        whenever(storage.upload(any(), any(), any())).thenReturn("avatars/7/avatar.jpg")
        val notPremium: Subscription = mock()
        whenever(notPremium.isEntitledToUnlimitedStories()).thenReturn(false)
        whenever(subscriptionService.getOrCreateSubscription(parent.id)).thenReturn(notPremium)
        val props = AppProperties(avatarVideo = AppProperties.AvatarVideoProperties(allowFreeAvatarUpload = allowFreeAvatarUpload))
        return FamilyAvatarService(parentAvatarRepository, parentRepository, subscriptionService, storageProvider, props, null)
    }

    @Test
    fun `free parent is refused avatar upload by default`() {
        val service = service(allowFreeAvatarUpload = false)

        assertThatThrownBy { service.uploadAvatar(parent.email, image, "image/jpeg") }
            .isInstanceOf(AvatarPremiumRequiredException::class.java)
        verify(storage, never()).upload(any(), any(), anyOrNull())
    }

    @Test
    fun `free parent can upload avatar when allowFreeAvatarUpload is on`() {
        val service = service(allowFreeAvatarUpload = true)

        val saved = service.uploadAvatar(parent.email, image, "image/jpeg")

        assertThat(saved.storagePath).isEqualTo("avatars/7/avatar.jpg")
        assertThat(saved.fileSizeBytes).isEqualTo(3L)
        verify(storage).upload(parent.id, image, "image/jpeg")
    }

    @Test
    fun `allowFreeAvatarUpload defaults to false`() {
        assertThat(AppProperties().avatarVideo.allowFreeAvatarUpload).isFalse()
    }
}
