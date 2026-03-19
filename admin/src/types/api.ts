export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
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
  /** Resolved path/URL for Sora-generated cover video (MP4). Shown when SORA_ENABLED. */
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
  /** Rewritten narration script when pipeline has run. Use for further edits (prefer over content when present). */
  narratedContent?: string | null;
  /** When set, admin has marked this story for reject from the language view. */
  rejectMarkedAt?: string | null;
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
}

export interface BulkGenerateStoriesRequest {
  languages: string[];
  categories: string[];
  totalStories: number;
  publish?: boolean;
}

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
  /** True when all pipeline languages have a review row in library_story_language_reviews */
  allLanguagesReviewed?: string;
  /** Derived overall status for admin UX. COMPLETED when all audio exists. */
  overallStatus?: "PENDING" | "TRANSLATING_LANGUAGES" | "TTS_PROCESSING" | "COMPLETED" | "READY_FOR_REVIEW" | "FAILED";
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
}

/**
 * Story categories for admin (bulk generate, filters, edit). Must stay aligned with:
 * - Mobile: composeApp/src/commonMain/kotlin/.../SampleData.kt categories (excluding "All")
 * - Backend: StoryCategories.canonical in application/storylibrary/StoryCategories.kt
 */
export const STORY_CATEGORIES = [
  "Animals",
  "Friendship",
  "Adventure",
  "Village Life",
  "Moral Stories",
  "Funny Stories",
  "Family Stories",
  "Fantasy",
  "Nature",
  "Bravery",
] as const;

export const AGE_GROUPS = [
  { value: 1, label: "1-2 years" },
  { value: 3, label: "3-4 years" },
  { value: 5, label: "5-6 years" },
  { value: 7, label: "7-8 years" },
  { value: 9, label: "9-10 years" },
  { value: 11, label: "11-12 years" },
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
  migrationMode: "MIGRATION_ENABLED" | "APP_ONLY";
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
  storyGenerationsToday: number;
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

// Medium priority features types
export interface VoiceCloningJob {
  id: number;
  parentId: number;
  audioStoragePath: string;
  audioFileSizeBytes: number;
  voiceName: string;
  elevenLabsVoiceId: string | null;
  status: string;
  errorMessage: string | null;
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
