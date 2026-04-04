export interface AuditEntry {
  id: number;
  adminEmail: string;
  action: string;
  resourceType: string;
  resourceId: string;
  details: string;
  createdAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

/** AI control plane — matches backend admin DTOs under `/api/v1/admin/ai-control-plane`. */
export interface AiProjectSummary {
  id: string;
  code: string;
  name: string;
  status: string;
}

export interface AiWorkflowSummary {
  id: string;
  workflowKey: string;
  name: string;
  category: string;
  status: string;
  version: number;
}

export interface AiWorkflowRunSummary {
  id: string;
  workflowKey: string;
  status: string;
  currentStepKey: string | null;
  entityType: string;
  entityId: string | null;
  startedAtIso: string;
}

export interface ExecuteWorkflowRequest {
  projectCode: string;
  workflowKey: string;
  entityType: string;
  entityId?: string | null;
  input?: Record<string, unknown>;
}

export interface WorkflowRunStarted {
  workflowRunId: string;
  status: string;
  currentStep: string | null;
  trackingPath: string;
}

export interface WorkflowRunDetail {
  workflowRunId: string;
  status: string;
  currentStepKey: string | null;
  output: Record<string, unknown> | null;
  startedAtIso: string | null;
  completedAtIso: string | null;
}

export interface PromptVersion {
  assetId: string;
  assetKey: string;
  version: number;
  checksum: string;
  approvalStatus: string;
}

export interface PublishPromptRequest {
  projectCode: string;
  assetKey: string;
  content: string;
  metadata?: Record<string, unknown> | null;
}

export interface AdminUser {
  id: number;
  email: string;
  role: string;
  createdAt: string;
}

export interface VoiceProfile {
  id: number;
  parentId: number;
  createdAt: string;
  profileName?: string | null;
  /** HeyGen voice_id for cloned-voice TTS; set via PATCH /admin/parents/:parentId/voice/:voiceProfileId */
  heygenVoiceId?: string | null;
}

export interface ParentSummary {
  id: number;
  name?: string;
  email: string;
  role: string;
  plan?: string;
  status?: "ACTIVE" | "SUSPENDED" | "PENDING";
  createdAt: string;
}

export interface ParentDetail {
  id: number;
  email: string;
  role: string;
  status: string;
  plan: string;
  phone: string | null;
  createdAt: string;
  suspendedAt: string | null;
}

export interface CreateParentRequest {
  email: string;
  password: string;
  phone?: string;
  role?: string;
}

export interface UpdateParentRequest {
  email?: string;
  phone?: string;
  role?: string;
}

export interface ChildSummary {
  id: number;
  parentId: number;
  parentEmail: string | null;
  name: string;
  dateOfBirth: string;
  age: number;
  languagePreference: string | null;
  createdAt: string;
}

export interface StorySummary {
  id: number;
  parentId: number;
  childId: number | null;
  theme: string;
  language: string;
  age: number;
  childName: string;
  wordCount: number;
  status: string;
  safetyScore: number | null;
  createdAt: string;
}

export interface LibraryStorySummary {
  id: number;
  title: string | null;
  content: string;
  theme: string;
  category?: string | null;
  language: string;
  age: number;
  childName: string;
  wordCount: number;
  readingTimeMinutes: number;
  moral: string | null;
  audioFileUrl: string | null;
  status: string;
  coverImageUrl: string | null;
  /** Resolved path/URL for animated cover (GIF) when image-to-video pipeline produced one. */
  coverVideoUrl?: string | null;
  createdAt: string;
  modifiedAt?: string;
  storyOwner?: string | null;
  convertPromptUsed?: string | null;
  /** CALM, SOOTHING, ADVENTUROUS. Default CALM for narration tone. */
  emotionMode?: string | null;
  /** When set, narration was approved for final delivery by a human. */
  narrationApprovedAt?: string | null;
  /** Per-language narration approval: language code -> approved. */
  translationApproval?: Record<string, boolean>;
  /** Rewritten narration script when pipeline has run. */
  narratedContent?: string | null;
  /** Primary story text for this language (translation/master), not the narration script. */
  sourceContent?: string;
  /** When true, a conversational narration script exists for this language. */
  preferNarratedContentForEditor?: boolean;
  /** When set, admin has marked this story for reject from the language view. */
  rejectMarkedAt?: string | null;
  /** Reviewer notes when status is CHANGES_REQUESTED or REJECTED. */
  reviewNotes?: string | null;
  /** When true, content managers need elevated approval to use Regenerate with prompt. */
  regeneratePromptLocked?: boolean;
  regeneratePromptLockApproved?: boolean;
  regeneratePromptUnlockRequestedAt?: string | null;
  /** ISO timestamp when soft-deleted (trash). */
  deletedAt?: string | null;
  /** Parent-facing discussion prompts (library metadata). */
  parentDiscussionPrompts?: string[] | null;
  parentContentNote?: string | null;
  speakAlongPrompt?: string | null;
  /** Branching episode graph (JSON object from API; edit as JSON string in admin). */
  interactiveGraph?: unknown;
  postStoryMission?: string | null;
  postStoryResourceUrl?: string | null;
}

export const EMOTION_MODES = ["CALM", "SOOTHING", "ADVENTUROUS"] as const;

export interface CreateLibraryStoryRequest {
  title?: string | null;
  content: string;
  theme: string;
  category?: string | null;
  language?: string;
  age: number;
  childName?: string;
  moral?: string | null;
  audioFileUrl?: string | null;
  status?: string;
  coverImageUrl?: string | null;
  coverVideoUrl?: string | null;
  /** CALM, SOOTHING, ADVENTUROUS. Default CALM for narration tone. */
  emotionMode?: string | null;
  /** When false and status=PUBLISHED: update only, no pipeline (pipeline runs after approval in Story for review). */
  regenerateNarration?: boolean | null;
  /** Optional per-language content (e.g. { hi: "...", en: "..." }). Deprecated: prefer translationContentEntries. */
  translationContents?: Record<string, string> | null;
  /** Per-language content with optional title and moral for each language. */
  translationContentEntries?: Record<string, { content: string; title?: string | null; moral?: string | null }> | null;
  /**
   * When sent on PUT, updates the narration script for the story’s current language.
   * Omit on create or when leaving the script unchanged; send "" to clear the script.
   */
  narratedContent?: string | null;
  /** Up to 10 short strings; stored as JSON on the library row. */
  parentDiscussionPrompts?: string[] | null;
  parentContentNote?: string | null;
  speakAlongPrompt?: string | null;
  /** Valid JSON string: startSegmentId, segments map with audioUrl and choices[]. */
  interactiveGraph?: string | null;
  postStoryMission?: string | null;
  postStoryResourceUrl?: string | null;
}

export interface BulkGenerateStoriesRequest {
  languages: string[];
  categories: string[];
  totalStories: number;
  publish?: boolean;
  /** Same backend allowlist as parent generate; invalid values are ignored server-side. */
  learningFocus?: string | null;
}

/** Mirrors backend StoryPromptBuilder allowed learning-focus keys (labels for admin UI). */
export const BULK_LEARNING_FOCUS_OPTIONS: ReadonlyArray<{ value: string; label: string }> = [
  { value: "empathy", label: "Empathy" },
  { value: "problem_solving", label: "Problem solving" },
  { value: "vocabulary", label: "Vocabulary" },
  { value: "curiosity", label: "Curiosity" },
  { value: "perseverance", label: "Perseverance" },
  { value: "sharing", label: "Sharing" },
  { value: "honesty", label: "Honesty" },
  { value: "courage", label: "Courage" },
  { value: "kindness", label: "Kindness" },
  { value: "friendship", label: "Friendship" },
  { value: "responsibility", label: "Responsibility" },
  { value: "public_speaking", label: "Public speaking" },
  { value: "money_literacy", label: "Money literacy" },
  { value: "research_skills", label: "Research & facts" },
] as const;

export interface BulkGeneratedStoryItem {
  id: number;
  language: string;
  category: string;
  title: string;
}

export interface BulkGenerateStoriesResponse {
  requested: number;
  createdCount: number;
  failedCount: number;
  publish: boolean;
  created: BulkGeneratedStoryItem[];
  failed: Array<{ language: string; category: string; error: string }>;
}

/** Async bulk job: POST returns jobId, poll GET for status and result */
export type BulkJobStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";

export interface BulkGenerateJobResponse {
  jobId: string;
}

export interface BulkGenerateJobStatusResponse {
  jobId: string;
  status: BulkJobStatus;
  requestedTotal: number;
  currentIndex: number;
  createdCount: number;
  failedCount: number;
  publish: boolean;
  progress: { current: number; total: number };
  errorMessage?: string;
  result?: BulkGenerateStoriesResponse;
  startedAt?: string;
  completedAt?: string;
}

export interface StoryWithIssues {
  storyId: number;
  title: string;
  theme: string;
  status: string;
  issues: LanguageIssue[];
}

export interface LanguageIssue {
  language: string;
  status: string;
  error: string;
}

export interface PipelineStatusResponse {
  /** Per-language status: ta, hi, en, etc. */
  [lang: string]: string | undefined;
  /** Current language being processed (e.g. "hi") */
  processing?: string;
  /** Comma-separated languages marked as reviewed (persisted in DB) */
  reviewedLanguages?: string;
  /** Languages reviewed but story text / script changed since review — admin should re-open and tap Have reviewed */
  reviewStaleLanguages?: string;
  /** True when all pipeline languages have a review row in library_story_language_reviews */
  allLanguagesReviewed?: string;
  /** Derived overall status for admin UX. COMPLETED when all audio exists. */
  overallStatus?:
    | "PENDING"
    | "TRANSLATING_LANGUAGES"
    | "TTS_PROCESSING"
    | "FINALIZING_STORY"
    | "COMPLETED"
    | "READY_FOR_REVIEW"
    | "FAILED";
  /** 0-100 progress */
  progress?: string;
  /** When audio was last generated/regenerated, in IST (e.g. "18-Mar-2026 17:15:23 IST") */
  generatedAtIst?: string;
  /** Audio duration in seconds (source language or max of completed languages) */
  durationSeconds?: string;
  /** Languages where audio may be truncated (duration < 50% of expected). Comma-separated. */
  audioCoverageWarnings?: string;
  /** Count of languages with failed status (translation/rewrite/TTS failed). */
  failedLanguagesCount?: string;
}

/** Response from GET /admin/stories/:id/stream-url (Voice & Avatar Studio). */
export interface LibraryStoryStreamUrlResponse {
  streamUrl: string;
  avatarUrl?: string;
  avatarVideoUrl?: string;
  avatarVideoStatus?: string;
  avatarVideoError?: string;
  avatarVideoProvider?: string;
  /** Same optional host bumper as parent stream-url (muted in app). */
  hostStoryClipUrl?: string;
}

/**
 * Story categories for admin (bulk generate, filters, edit). Must stay aligned with:
 * - Mobile: composeApp/src/commonMain/kotlin/.../SampleData.kt categories (excluding "All")
 * - Backend: StoryCategories.canonical in application/storylibrary/StoryCategories.kt
 * - Taxonomy: docs/admin/EDU_METADATA_CONVENTIONS.md
 *
 * "Fun stories" / "Funny Stories" are the light-classics lane in the parent app (Fun corner filter).
 * "Learn · …" rows remain valid editorial categories; there is no separate Learn-only hub in the app.
 * "Learn · Digital Safety" / "Learn · Simulator · Digital Safety" = Edu painkiller lane (scams, digital judgment).
 */
/**
 * Library story categories (backend allowlist). Parent apps group the catalog into hubs:
 * Browse (all), Fun (e.g. Fun stories / Funny Stories), Learn & safety (Learn · * including Digital Safety),
 * Practice (e.g. Learn · Simulator · Digital Safety, interactive graph stories).
 */
export const STORY_CATEGORIES = [
  "Animals",
  "Friendship",
  "Adventure",
  "Village Life",
  "Moral Stories",
  "Fun stories",
  "Funny Stories",
  "Family Stories",
  "Fantasy",
  "Nature",
  "Bravery",
  "Learn · History",
  "Learn · Science & Nature",
  "Learn · Culture & Heritage",
  "Learn · Life Skills",
  "Learn · Digital Safety",
  "Learn · Simulator · Digital Safety",
] as const;

export const AGE_GROUPS = [
  { value: 1, label: "1-2 years" },
  { value: 3, label: "3-4 years" },
  { value: 5, label: "5-6 years" },
  { value: 7, label: "7-8 years" },
  { value: 9, label: "9-10 years" },
  { value: 11, label: "11-12 years" },
  { value: 13, label: "13-15 years" },
  { value: 16, label: "16-17 years" },
  { value: 18, label: "18-24 years" },
  { value: 30, label: "25-40 years" },
  { value: 50, label: "41-60 years" },
  { value: 70, label: "61-99 years" },
] as const;

export const MIN_WORD_COUNT = 50;

export interface VoiceUploadLog {
  id: number;
  parentId: number;
  parentEmail: string | null;
  createdAt: string;
}

export interface SubscriptionStatus {
  parentId: number;
  email: string;
  status: string;
  plan: string | null;
}

export interface ReferralCode {
  id: number;
  shortcode: string;
  shopName: string;
  offerPercent: number;
  expiresAt: string;
  active: boolean;
  stripeCouponId: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Short content: riddles, thought for the day, proverbs, tongue twisters, etc. */
export interface ShortContentDto {
  id: number;
  type: string;
  content: string;
  answer: string | null;
  language: string;
  ageMin: number | null;
  ageMax: number | null;
  displayDate: string | null;
  audioUrl: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateShortContentRequest {
  type: string;
  content: string;
  answer?: string | null;
  language?: string;
  ageMin?: number | null;
  ageMax?: number | null;
  displayDate?: string | null;
  audioUrl?: string | null;
  status?: string;
}

export interface UpdateShortContentRequest {
  type?: string | null;
  content?: string | null;
  answer?: string | null;
  language?: string | null;
  ageMin?: number | null;
  ageMax?: number | null;
  displayDate?: string | null;
  audioUrl?: string | null;
  status?: string | null;
}

export interface GenerateShortContentRequest {
  type: string;
  language?: string;
  count?: number;
}

export interface GeneratedShortContentItem {
  content: string;
  answer?: string | null;
}

export interface GenerateShortContentResponse {
  items: GeneratedShortContentItem[];
}

/** Spring Data Page for admin short-content list */
export interface ShortContentPage {
  content: ShortContentDto[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface HealthDto {
  status: string;
  components: Record<string, { status: string; details?: Record<string, unknown> }> | null;
}

export interface RuntimeConfigDto {
  instanceId: string;
  flywayEnabled: boolean;
  flywayLockRetryCount: number;
  datasourceTarget?: string;
  migrationMode: "MIGRATION_ENABLED" | "APP_ONLY";
  keepNarrationApprovalOnMetadataOnlyPublishedUpdate?: boolean;
  storyApprovalRetentionMode?: "STRICT_REVIEW_CYCLE" | "RELAXED_METADATA_ONLY";
  operatorHint: string;
}

/** Per-API usage for AI metrics breakdown. */
export interface ApiUsageDto {
  api: string;
  displayName: string;
  requests: number;
  tokensOrCharacters?: number | null;
  costEstimateUsd?: number | null;
}

/** Per-story AI usage for admin story-wise metrics. */
export interface StoryAiUsageDto {
  storyId: number;
  title: string | null;
  totalTokens: number;
  avatarVideoCount: number;
  costInr: number;
}

export interface AiMetricsDto {
  storyGenerationsTotal: number;
  cacheHits: number;
  cacheMisses: number;
  voiceProcessingCount: number;
  openaiTokensUsed: number | null;
  /** Per-API usage for admin UI. */
  apiBreakdown?: ApiUsageDto[];
  /** Per-story AI usage (tokens, avatar count, cost in ₹). */
  storyBreakdown?: StoryAiUsageDto[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresInSeconds: number;
}

export interface CurrentUserResponse {
  email: string;
  role: string;
  /** Permission names for RBAC (e.g. MANAGE_STORIES, MODERATE_STORIES). Empty for non-admin. */
  permissions?: string[];
}

// Dashboard & mock data types
export interface DashboardKpis {
  activeSubscriptions: number;
  monthlyRevenue: number;
  storyGenerationsTotal: number;
  aiTokenUsage: number;
  moderationFlags: number;
}

export interface RevenueChartPoint {
  month: string;
  revenue: number;
}

export interface StoryUsageChartPoint {
  date: string;
  count: number;
}

export interface InvoiceRow {
  id: string;
  parentId: number;
  email: string;
  amount: number;
  currency: string;
  status: "PAID" | "PENDING" | "REFUNDED" | "FAILED";
  plan: string;
  dueDate: string;
  paidAt: string | null;
}

/** Admin API invoice response */
export interface AdminInvoice {
  id: number;
  providerInvoiceId: string;
  parentId: number;
  email: string;
  amount: number;
  currency: string;
  status: string;
  plan: string;
  dueDate: string;
  paidAt: string | null;
}

export interface SystemMonitoringMetrics {
  apiLatencyMs: number;
  kafkaLag: number;
  redisHitRatio: number;
  errorRate: number;
  aiCostUsd: number;
}

// Revenue Analytics types
export interface RevenueMetricsDto {
  month: string;
  revenue: number;
  currency: string;
}

export interface SubscriptionMetricsDto {
  activeSubscriptions: number;
  trialCount: number;
  planDistribution: Record<string, number>;
  mrr: number;
  trialConversionRate?: number;
  churnRate?: number;
}

export interface StoryLengthProfileDto {
  windowDays: number;
  totalStories: number;
  avgWordCount: number;
  avgReadingTimeMinutes: number;
  avgExpectedMinutesByWords: number;
  wpmAssumption: number;
}

export interface DoraMetricsDto {
  windowDays: number;
  serviceName?: string | null;
  environment?: string | null;
  successfulDeployments: number;
  failedDeployments: number;
  deploymentFrequencyPerDay: number;
  changeFailureRatePercent: number;
  leadTimeMinutesP50?: number | null;
  leadTimeMinutesP95?: number | null;
  meanTimeToRestoreMinutes?: number | null;
  openIncidents: number;
  resolvedIncidents: number;
}

export interface RevenueRow {
  parentId: number;
  email: string;
  plan: string;
  subscriptionState: string;
  monthlyPayment: number;
  createdAt: string;
}

export interface PlanDistributionPoint {
  name: string;
  value: number;
}

export interface ChurnTrendPoint {
  month: string;
  churnRate: number;
}

export interface StoryGenerationByPlanPoint {
  plan: string;
  count: number;
}

export interface RevenueAlerts {
  paymentFailureRate?: number;
  paymentFailureThreshold?: number;
  aiTokenSpike?: boolean;
  churnIncrease?: boolean;
  churnRate?: number;
}

/** Story retention metrics for revenue dashboard */
export interface RetentionMetricsDto {
  averageListenTimeSeconds: number;
  mostPopularCategory: string;
  retentionByLanguage: Record<string, number>;
  periodDays: number;
}

export interface CompletionRatePerStory {
  storyId: number;
  storySource: string;
  completionRate: number;
}

/** Story completion metrics for revenue dashboard */
export interface CompletionMetricsDto {
  completionRatePerStory: CompletionRatePerStory[];
  overallCompletionRate: number;
  retentionByLanguage: Record<string, number>;
  periodDays: number;
}

/** Admin aggregate of interactive Edu choices (life_skill_choice_events). */
export interface LifeSkillChoiceAnalyticsRowResponse {
  libraryStoryId: number;
  storyTitle?: string | null;
  segmentId: string;
  choiceId: string;
  eventCount: number;
}

export interface LifeSkillChoiceAnalyticsResponse {
  periodDays: number;
  totalEvents: number;
  rows: LifeSkillChoiceAnalyticsRowResponse[];
}

// Medium priority features types
export interface VoiceCloningJob {
  id: number;
  parentId: number;
  audioStoragePath: string;
  audioFileSizeBytes: number;
  voiceName: string;
  elevenLabsVoiceId: string | null;
  /** Fish Audio TTS model id (managed clone alternative to ElevenLabs). */
  fishAudioModelId?: string | null;
  status: string;
  errorMessage: string | null;
  providerAuthFailed?: boolean;
  providerQuotaFailed?: boolean;
  providerStatusMessage?: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface VoiceTier {
  id: number;
  name: string;
  priceMonthly: number;
  priceYearly: number;
  maxChildren: number;
  maxVoices: number;
  maxAvatarVideos: number;
  maxSoundscapes: number;
  allowsVoiceCloning: boolean;
  allowsAvatarVideo: boolean;
  allowsSoundscapes: boolean;
  allowsFamilySharing: boolean;
  analyticsEnabled: boolean;
}

export interface Soundscape {
  id: number;
  name: string;
  category?: string;
  description?: string | null;
  durationSeconds?: number;
}

export interface SoundscapeUsage {
  id?: number;
  soundscapeId?: number;
  storyId?: number;
  [key: string]: unknown;
}
