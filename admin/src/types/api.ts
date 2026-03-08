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

export interface ParentSummary {
  id: number;
  name?: string;
  email: string;
  role: string;
  plan?: string;
  status?: "ACTIVE" | "SUSPENDED" | "PENDING";
  createdAt: string;
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

export interface CuratedStorySummary {
  id: number;
  title: string | null;
  content: string;
  theme: string;
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
  /** CALM, SOOTHING, ADVENTUROUS. Default CALM for narration tone. */
  emotionMode?: string | null;
}

export const EMOTION_MODES = ["CALM", "SOOTHING", "ADVENTUROUS"] as const;

export interface CreateCuratedStoryRequest {
  title?: string | null;
  content: string;
  theme: string;
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
}

export interface PipelineStatusResponse {
  [lang: string]: string; // e.g. { ta: "READY", hi: "TRANSLATING", en: "TTS_PENDING" }
}

export const STORY_CATEGORIES = [
  "Adventure",
  "Animals",
  "Bedtime",
  "Folktales",
  "Myths",
  "Friendship",
  "Learning",
  "Nature",
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

export interface HealthDto {
  status: string;
  components: Record<string, { status: string; details?: Record<string, unknown> }> | null;
}

export interface AiMetricsDto {
  storyGenerationsTotal: number;
  cacheHits: number;
  cacheMisses: number;
  voiceProcessingCount: number;
  openaiTokensUsed: number | null;
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
export interface Soundscape {
  id: number;
  name: string;
  category: string;
  durationSeconds: number;
  audioUrl: string;
  description: string | null;
}

export interface SoundscapeUsage {
  id: number;
  parentId: number;
  storyId: number | null;
  soundscapeId: number;
  soundscapeName: string;
  usageCount: number;
  lastUsedAt: string;
}

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
