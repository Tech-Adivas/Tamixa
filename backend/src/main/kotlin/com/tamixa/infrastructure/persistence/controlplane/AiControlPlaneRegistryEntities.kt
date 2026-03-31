package com.tamixa.infrastructure.persistence.controlplane

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ai_projects")
class AiProjectEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(nullable = false, length = 120)
    var name: String = "",

    @Column(nullable = false, length = 50, unique = true)
    var code: String = "",

    @Column(nullable = false, length = 30)
    var status: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_workflows")
class AiWorkflowEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    var project: AiProjectEntity,

    @Column(name = "workflow_key", nullable = false, length = 120, unique = true)
    var workflowKey: String = "",

    @Column(nullable = false, length = 150)
    var name: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, length = 50)
    var category: String = "",

    @Column(nullable = false, length = 30)
    var status: String = "",

    @Column(nullable = false)
    var version: Int = 1,

    @Column(name = "definition_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var definitionJson: Map<String, Any?> = emptyMap(),

    @Column(name = "requires_human_approval", nullable = false)
    var requiresHumanApproval: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_workflow_steps")
class AiWorkflowStepEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    var workflow: AiWorkflowEntity,

    @Column(name = "step_key", nullable = false, length = 120)
    var stepKey: String = "",

    @Column(name = "step_type", nullable = false, length = 50)
    var stepType: String = "",

    @Column(name = "step_order", nullable = false)
    var stepOrder: Int = 0,

    @Column(name = "config_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var configJson: Map<String, Any?> = emptyMap(),

    @Column(name = "is_required", nullable = false)
    var isRequired: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_prompt_assets")
class AiPromptAssetEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    var project: AiProjectEntity,

    @Column(name = "asset_key", nullable = false, length = 150)
    var assetKey: String = "",

    @Column(name = "asset_type", nullable = false, length = 40)
    var assetType: String = "",

    @Column(nullable = false, length = 150)
    var name: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "language_code", length = 20)
    var languageCode: String? = null,

    @Column(nullable = false, length = 30)
    var scope: String = "",

    @Column(nullable = false, length = 30)
    var status: String = "",

    @Column(name = "current_version", nullable = false)
    var currentVersion: Int = 0,

    @Column(name = "owner_team", length = 80)
    var ownerTeam: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_prompt_asset_versions")
class AiPromptAssetVersionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    var asset: AiPromptAssetEntity,

    @Column(nullable = false)
    var version: Int = 0,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String = "",

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadataJson: Map<String, Any?>? = null,

    @Column(nullable = false, length = 128)
    var checksum: String = "",

    @Column(name = "created_by", nullable = false, length = 120)
    var createdBy: String = "",

    @Column(name = "approved_by", length = 120)
    var approvedBy: String? = null,

    @Column(name = "approval_status", nullable = false, length = 30)
    var approvalStatus: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_language_profiles")
class AiLanguageProfileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(name = "language_code", nullable = false, length = 20, unique = true)
    var languageCode: String = "",

    @Column(name = "language_name", nullable = false, length = 100)
    var languageName: String = "",

    @Column(name = "script_name", nullable = false, length = 50)
    var scriptName: String = "",

    @Column(name = "supports_generation", nullable = false)
    var supportsGeneration: Boolean = false,

    @Column(name = "supports_translation", nullable = false)
    var supportsTranslation: Boolean = false,

    @Column(name = "supports_transliteration", nullable = false)
    var supportsTransliteration: Boolean = false,

    @Column(name = "supports_tts", nullable = false)
    var supportsTts: Boolean = false,

    @Column(name = "supports_voice_clone", nullable = false)
    var supportsVoiceClone: Boolean = false,

    @Column(name = "preferred_provider_rules", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var preferredProviderRules: Map<String, Any?>? = null,

    @Column(name = "quality_thresholds", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var qualityThresholds: Map<String, Any?>? = null,

    @Column(name = "moderation_profile_key", length = 120)
    var moderationProfileKey: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_model_providers")
class AiModelProviderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(name = "provider_key", nullable = false, length = 80, unique = true)
    var providerKey: String = "",

    @Column(nullable = false, length = 120)
    var name: String = "",

    @Column(name = "provider_type", nullable = false, length = 40)
    var providerType: String = "",

    @Column(nullable = false, length = 30)
    var status: String = "",

    @Column(name = "capabilities_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var capabilitiesJson: Map<String, Any?> = emptyMap(),

    @Column(name = "health_status", nullable = false, length = 30)
    var healthStatus: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_models")
class AiModelEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: AiModelProviderEntity,

    @Column(name = "model_key", nullable = false, length = 120, unique = true)
    var modelKey: String = "",

    @Column(name = "display_name", nullable = false, length = 120)
    var displayName: String = "",

    @Column(name = "task_family", nullable = false, length = 50)
    var taskFamily: String = "",

    @Column(name = "context_window")
    var contextWindow: Int? = null,

    @Column(name = "max_output_tokens")
    var maxOutputTokens: Int? = null,

    @Column(name = "pricing_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var pricingJson: Map<String, Any?>? = null,

    @Column(name = "quality_tier", nullable = false, length = 20)
    var qualityTier: String = "",

    @Column(nullable = false, length = 30)
    var status: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_routing_policies")
class AiRoutingPolicyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(name = "policy_key", nullable = false, length = 120, unique = true)
    var policyKey: String = "",

    @Column(nullable = false, length = 120)
    var name: String = "",

    @Column(name = "task_type", nullable = false, length = 50)
    var taskType: String = "",

    @Column(name = "language_code", length = 20)
    var languageCode: String? = null,

    @Column(name = "audience_type", length = 30)
    var audienceType: String? = null,

    @Column(name = "quality_tier", length = 20)
    var qualityTier: String? = null,

    @Column(name = "budget_tier", length = 20)
    var budgetTier: String? = null,

    @Column(name = "rule_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var ruleJson: Map<String, Any?> = emptyMap(),

    @Column(nullable = false, length = 30)
    var status: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_policy_rules")
class AiPolicyRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(name = "rule_key", nullable = false, length = 120, unique = true)
    var ruleKey: String = "",

    @Column(nullable = false, length = 150)
    var name: String = "",

    @Column(name = "rule_type", nullable = false, length = 50)
    var ruleType: String = "",

    @Column(nullable = false, length = 30)
    var scope: String = "",

    @Column(nullable = false, length = 20)
    var severity: String = "",

    @Column(name = "condition_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var conditionJson: Map<String, Any?> = emptyMap(),

    @Column(name = "action_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var actionJson: Map<String, Any?> = emptyMap(),

    @Column(nullable = false, length = 30)
    var status: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_evaluation_profiles")
class AiEvaluationProfileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(name = "profile_key", nullable = false, length = 120, unique = true)
    var profileKey: String = "",

    @Column(nullable = false, length = 150)
    var name: String = "",

    @Column(name = "task_type", nullable = false, length = 50)
    var taskType: String = "",

    @Column(name = "language_code", length = 20)
    var languageCode: String? = null,

    @Column(name = "rubric_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var rubricJson: Map<String, Any?> = emptyMap(),

    @Column(name = "thresholds_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var thresholdsJson: Map<String, Any?> = emptyMap(),

    @Column(nullable = false, length = 30)
    var status: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
