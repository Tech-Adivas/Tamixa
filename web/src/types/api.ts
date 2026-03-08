// Web API types

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresInSeconds: number;
}

export interface CurrentUser {
  email: string;
  role: string;
}

export interface Child {
  id: number;
  name: string;
  dateOfBirth: string;
  languagePreference: string | null;
  interests?: string | null;
  favoriteColor?: string | null;
  favoriteAnimal?: string | null;
  characterTraits?: string | null;
  avatarChoice?: string | null;
}

export interface CuratedStory {
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
  coverImageUrl?: string | null;
  coverVideoUrl?: string | null;
  createdAt: string;
}

export interface Story {
  id: number;
  parentId: number;
  childId: number | null;
  content: string;
  theme: string;
  language: string;
  age: number;
  childName: string;
  wordCount: number;
  readingTimeMinutes: number;
  title: string | null;
  moral: string | null;
  status: string;
  audioFileUrl: string | null;
  coverImageUrl?: string | null;
  coverVideoUrl?: string | null;
  createdAt: string;
}

export interface StoriesPage {
  content: Story[];
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface GenerateStoryRequest {
  age: number;
  language?: string;
  theme: string;
  childName: string;
  childId?: number | null;
  emotionMode?: string | null;
  parentCustomPrompt?: string | null;
  conversationMessages?: string[] | null;
}

export interface StreamUrlResponse {
  streamUrl: string;
}

export interface VoiceOption {
  voiceProfile: string;
  isPremium: boolean;
}

export interface VoicesResponse {
  voices: VoiceOption[];
}

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

export interface Subscription {
  plan: string;
  status: string;
  provider: string | null;
  currentPeriodEnd: string | null;
  trialEnd: string | null;
  cancelAtPeriodEnd: boolean;
  maxChildren: number;
  voicePremium: boolean;
  isEntitledToUnlimitedStories: boolean;
}

export interface Usage {
  month: string;
  storiesUsed: number;
  storiesLimit: number | null;
  voiceUsed: number;
  voiceLimit: number;
}

export interface VoiceProfile {
  id: number;
  parentId: number;
  createdAt: string;
}

export interface ConsentRecord {
  consentType: string;
  version: number;
  grantedAt: string;
}

export interface ExportJob {
  id: number;
  status: string;
  requestedAt: string;
  downloadUrl: string | null;
}

export interface Achievement {
  type: string;
  name: string;
  description: string;
  requiredCompletions: number;
  earned: boolean;
  earnedAt?: string | null;
}

export interface PlaybackPosition {
  storyId: number;
  storySource: string;
  positionSeconds: number;
  updatedAt: string;
}

export interface RecommendedStory {
  storyId: number;
  storySource: string;
  title: string;
  theme: string;
  age: number;
  reason: string;
}

export interface FavoriteStory {
  storyId: number;
  storySource: string;
}

export interface ListeningProgress {
  periodDays: number;
  storiesStarted: number;
  storiesCompleted: number;
  completionRate: number;
}
