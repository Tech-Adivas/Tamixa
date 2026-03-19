package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.AccountDeletionPort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Deletes all parent-related data and the parent record in FK-safe order.
 */
@Component
@Primary
class AccountDeletionAdapter(
    private val parentJpaRepository: ParentJpaRepository,
    private val subscriptionEventJpaRepository: SubscriptionEventJpaRepository,
    private val invoiceJpaRepository: InvoiceJpaRepository,
    private val subscriptionJpaRepository: SubscriptionJpaRepository,
    private val storyPlaybackPositionJpaRepository: StoryPlaybackPositionJpaRepository,
    private val favoriteStoryJpaRepository: FavoriteStoryJpaRepository,
    private val storyFeedbackJpaRepository: StoryFeedbackJpaRepository,
    private val storyAnalyticsJpaRepository: StoryAnalyticsJpaRepository,
    private val dataExportJobJpaRepository: DataExportJobJpaRepository,
    private val parentConsentJpaRepository: ParentConsentJpaRepository,
    private val billingAuditLogJpaRepository: BillingAuditLogJpaRepository,
    private val usageTrackingJpaRepository: UsageTrackingJpaRepository,
    private val trialUsageJpaRepository: TrialUsageJpaRepository,
    private val deviceJpaRepository: DeviceJpaRepository,
    private val childAchievementJpaRepository: ChildAchievementJpaRepository,
    private val storyTokenUsageJpaRepository: StoryTokenUsageJpaRepository,
    private val storyJpaRepository: StoryJpaRepository,
    private val childJpaRepository: ChildJpaRepository,
    private val storyFamilyVoiceJpaRepository: StoryFamilyVoiceJpaRepository,
    private val storyAvatarVideoJpaRepository: StoryAvatarVideoJpaRepository,
    private val voiceCloningJobJpaRepository: VoiceCloningJobJpaRepository,
    private val voiceProfileJpaRepository: VoiceProfileJpaRepository,
    private val parentAvatarJpaRepository: ParentAvatarJpaRepository,
    private val magicLinkTokenJpaRepository: MagicLinkTokenJpaRepository
) : AccountDeletionPort {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun deleteAccount(email: String): Boolean {
        val parent = parentJpaRepository.findByEmail(email) ?: return false
        val parentId = parent.id
        log.info("Deleting account data for parentId={}", parentId)

        subscriptionEventJpaRepository.deleteBySubscription_Parent_Id(parentId)
        invoiceJpaRepository.deleteByParent_Id(parentId)
        subscriptionJpaRepository.deleteByParent_Id(parentId)
        storyPlaybackPositionJpaRepository.deleteByParent_Id(parentId)
        favoriteStoryJpaRepository.deleteByParent_Id(parentId)
        storyFeedbackJpaRepository.deleteByParent_Id(parentId)
        storyAnalyticsJpaRepository.deleteByParent_Id(parentId)
        dataExportJobJpaRepository.deleteByParent_Id(parentId)
        parentConsentJpaRepository.deleteByParent_Id(parentId)
        billingAuditLogJpaRepository.deleteByParent_Id(parentId)
        usageTrackingJpaRepository.deleteByParent_Id(parentId)
        trialUsageJpaRepository.deleteByParent_Id(parentId)
        deviceJpaRepository.deleteByParent_Id(parentId)
        childAchievementJpaRepository.deleteByChild_Parent_Id(parentId)
        storyTokenUsageJpaRepository.deleteByStory_Parent_Id(parentId)
        storyJpaRepository.deleteByParent_Id(parentId)
        childJpaRepository.deleteByParent_Id(parentId)
        storyFamilyVoiceJpaRepository.deleteByParentId(parentId)
        storyAvatarVideoJpaRepository.deleteByParentId(parentId)
        voiceCloningJobJpaRepository.deleteByParentId(parentId)
        voiceProfileJpaRepository.deleteByParent_Id(parentId)
        parentAvatarJpaRepository.deleteByParentId(parentId)
        magicLinkTokenJpaRepository.deleteByEmail(email)
        parentJpaRepository.deleteById(parentId)

        return true
    }
}
