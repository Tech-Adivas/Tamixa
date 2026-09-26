# Tamixa Premium UX Overhaul - Technical Overview

## Introduction

This document provides technical details about the Tamixa Premium UX Overhaul for developers, architects, and technical stakeholders.

## Architecture Overview

### System Components

```
┌─────────────────────────────────────────────────────────┐
│                    Client Layer                         │
├─────────────────────────────────────────────────────────┤
│  Mobile App          Web App          Admin Dashboard   │
│  (KMP Compose)       (React/Vite)     (Next.js)        │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  Design System Layer                    │
├─────────────────────────────────────────────────────────┤
│  Design Tokens    Components    Patterns                │
│  (Colors, Spacing, Typography, Radii, Shadows)         │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                     API Layer                           │
├─────────────────────────────────────────────────────────┤
│  REST API (Spring Boot)                                 │
│  - Story Management                                     │
│  - User Management                                      │
│  - AI Metrics                                           │
│  - Analytics                                            │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                           │
├─────────────────────────────────────────────────────────┤
│  PostgreSQL    Redis Cache    S3 Storage                │
│  (Stories,     (Metrics,      (Media                    │
│   Users,       Sessions)      Assets)                   │
│   Metrics)                                              │
└─────────────────────────────────────────────────────────┘
```

### Technology Stack

**Mobile App**:
- Kotlin Multiplatform (KMP)
- Jetpack Compose for UI
- Ktor for networking
- SQLDelight for local storage
- Coil for image loading

**Web App**:
- React 18
- Vite for build tooling
- React Router for navigation
- Axios for API calls
- Tailwind CSS for styling

**Admin Dashboard**:
- Next.js 14 (App Router)
- React Server Components
- TypeScript
- Tailwind CSS
- shadcn/ui components

**Backend API**:
- Kotlin
- Spring Boot 3
- PostgreSQL 15
- Redis 7
- AWS S3

## Design System

### Design Tokens

Design tokens are the atomic values that define the visual language.

#### Mobile (Kotlin)

**Location**: `mobile/composeApp/src/commonMain/kotlin/com/tamixa/ui/theme/Theme.kt`

```kotlin
object TamixaDesignTokens {
    // Spacing (8dp grid)
    val screenPadding = 24.dp
    val sectionSpacing = 24.dp
    val cardSpacing = 16.dp
    val smallSpacing = 12.dp
    val cardContentPadding = 20.dp
    val minTouchTargetSize = 48.dp
    
    // Border Radii
    val cardRadius = 18.dp
    val cardRadiusMedium = 20.dp
    val cardRadiusLarge = 22.dp
    val buttonRadius = 28.dp
    val inputRadius = 20.dp
    val dialogRadius = 28.dp
    
    // Elevation
    val cardElevation = 6.dp
    val cardElevationHover = 10.dp
    val buttonShadowElevation = 8.dp
    val fabShadowElevation = 12.dp
    
    // Motion
    val durationShort = 150
    val durationMedium = 300
    val durationLong = 500
    val easingStandard = FastOutSlowInEasing
}

object TamixaColors {
    // Storybook Dusk Palette
    val terracotta = Color(0xFFC4625A)      // Warm accent
    val deepTeal = Color(0xFF2D5A5A)        // Primary
    val eduStoryMint = Color(0xFF5CBCA8)    // Learn accent
    val warmSand = Color(0xFFE8DCC8)        // Cream
    val warmCharcoal = Color(0xFF141210)    // Dark base
    val warmSurface = Color(0xFF2C2822)     // Card surface
}
```

#### Web/Admin (TypeScript)

**Location**: `admin/src/lib/design-tokens.ts`

```typescript
export const spacing = {
  xs: '0.25rem',    // 4px
  sm: '0.5rem',     // 8px
  md: '0.75rem',    // 12px
  lg: '1rem',       // 16px
  xl: '1.25rem',    // 20px
  '2xl': '1.5rem',  // 24px
  
  // Semantic
  screenPadding: '1.5rem',
  sectionSpacing: '1.5rem',
  cardSpacing: '1rem',
  minTouchTargetSize: '3rem',
};

export const colors = {
  // Storybook Dusk Palette
  terracotta: '#C4625A',
  deepTeal: '#2D5A5A',
  eduStoryMint: '#5CBCA8',
  warmSand: '#E8DCC8',
  warmCharcoal: '#141210',
  warmSurface: '#2C2822',
};

export const radius = {
  sm: '0.5rem',     // 8px
  md: '0.75rem',    // 12px
  lg: '1rem',       // 16px
  xl: '1.25rem',    // 20px
  '2xl': '1.5rem',  // 24px
  
  // Semantic
  input: '1.25rem',
  card: '1.125rem',
  button: '1.75rem',
  dialog: '1.75rem',
};
```

### Component Library

#### Mobile Components

**TamixaCard**:
```kotlin
@Composable
fun TamixaCard(
    modifier: Modifier = Modifier,
    colors: CardColors = TamixaCardColors.surface(),
    elevation: Dp = TamixaDesignTokens.cardElevation,
    radius: Dp = TamixaDesignTokens.cardRadius,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
)
```

**TamixaButton**:
```kotlin
@Composable
fun TamixaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: (@Composable () -> Unit)? = null
)
```

**TamixaTextField**:
```kotlin
@Composable
fun TamixaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true
)
```

#### Admin Components

**Card**:
```typescript
interface CardProps {
  variant?: 'surface' | 'elevated' | 'outlined';
  className?: string;
  children: React.ReactNode;
}

export function Card({ variant = 'surface', className, children }: CardProps)
```

**Button**:
```typescript
interface ButtonProps {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  disabled?: boolean;
  onClick?: () => void;
  children: React.ReactNode;
}

export function Button({ variant = 'primary', size = 'md', ...props }: ButtonProps)
```

## API Endpoints

### Story Management

#### Linear Stories

**List Stories**:
```
GET /admin/stories
Query Parameters:
  - page: number (default: 0)
  - size: number (default: 20)
  - status: StoryStatus (optional)
  - category: string (optional)
  - search: string (optional)

Response: PagedResponse<AdminStoryResponse>
```

**Get Story**:
```
GET /admin/stories/{id}
Response: AdminStoryResponse
```

**Create Story**:
```
POST /admin/stories
Body: AdminStoryRequest
Response: AdminStoryResponse
```

**Update Story**:
```
PUT /admin/stories/{id}
Body: AdminStoryRequest
Response: AdminStoryResponse
```

**Submit for Review**:
```
POST /admin/stories/{id}/submit
Response: AdminStoryResponse
```

**Approve & Publish**:
```
POST /admin/stories/{id}/publish
Response: AdminStoryResponse
```

**Request Changes**:
```
POST /admin/stories/{id}/request-changes
Body: { comment: string }
Response: AdminStoryResponse
```

#### Interactive Stories

**Create Interactive Story**:
```
POST /admin/interactive-stories
Body: InteractiveStoryRequest
Response: InteractiveStoryResponse
```

**Validate Graph**:
```
POST /admin/interactive-stories/{id}/validate
Response: ValidationResult
```

**Submit for Review**:
```
POST /admin/interactive-stories/{id}/submit
Response: InteractiveStoryResponse
```

### AI Metrics

**Get Summary**:
```
GET /admin/ai-metrics/summary
Query Parameters:
  - period: string (1h, 24h, 7d, 30d)

Response: AIMetricsSummaryResponse
```

**Get Trend**:
```
GET /admin/ai-metrics/trend
Query Parameters:
  - period: string
  - interval: string (hour, day, week)

Response: AIMetricsTrendResponse
```

**Get Models**:
```
GET /admin/ai-metrics/models
Query Parameters:
  - period: string

Response: AIMetricsModelsResponse
```

### Mobile API

**Get Home**:
```
GET /home
Response: HomeResponse
```

**Get Library Stories**:
```
GET /stories/library
Query Parameters:
  - category: string (optional)
  - language: string (optional)
  - page: number
  - size: number

Response: PagedResponse<LibraryStoryResponse>
```

**Get Story Stream URL**:
```
GET /stories/{id}/stream-url
Response: StreamUrlResponse
```

**Save Playback Position**:
```
POST /playback/position
Body: {
  storyId: number,
  position: number,
  completed: boolean
}
Response: Success
```

## Database Schema

### Story Status Tracking

```sql
-- Add status column to library_stories
ALTER TABLE library_stories 
ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'DRAFT',
ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;

CREATE INDEX idx_library_stories_status ON library_stories(status);
CREATE INDEX idx_library_stories_created_by ON library_stories(created_by);
```

### Pipeline Status Tracking

```sql
-- Track translation/TTS progress per language
CREATE TABLE pipeline_status (
    id BIGSERIAL PRIMARY KEY,
    story_id BIGINT NOT NULL REFERENCES library_stories(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL,
    translation_status VARCHAR(50) DEFAULT 'NOT_STARTED',
    rewrite_status VARCHAR(50) DEFAULT 'NOT_STARTED',
    tts_status VARCHAR(50) DEFAULT 'NOT_STARTED',
    overall_status VARCHAR(50) DEFAULT 'NOT_STARTED',
    error_message TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(story_id, language)
);

CREATE INDEX idx_pipeline_status_story_id ON pipeline_status(story_id);
CREATE INDEX idx_pipeline_status_overall_status ON pipeline_status(overall_status);
```

### AI Metrics Tracking

```sql
-- Detailed AI generation logs
CREATE TABLE ai_generation_logs (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES parents(id) ON DELETE SET NULL,
    model_name VARCHAR(100) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    input_tokens INT,
    output_tokens INT,
    total_tokens INT,
    latency_ms INT,
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_generation_logs_created_at ON ai_generation_logs(created_at);
CREATE INDEX idx_ai_generation_logs_model_name ON ai_generation_logs(model_name);
CREATE INDEX idx_ai_generation_logs_operation_type ON ai_generation_logs(operation_type);
```

## Performance Optimizations

### Mobile App

**Image Loading**:
- Coil library for efficient caching
- Progressive loading with placeholders
- Appropriate image resolutions (1x, 2x, 3x)
- WebP format for smaller file sizes

**List Performance**:
- LazyColumn for efficient scrolling
- Pagination (20 items per page)
- Item key for stable identity
- Avoid unnecessary recomposition

**Memory Management**:
- Release resources when not visible
- Use remember for expensive calculations
- Avoid memory leaks in ViewModels
- Profile with Android Studio Profiler

### Admin Dashboard

**Server-Side Pagination**:
```typescript
const { data, isLoading } = useQuery({
  queryKey: ['stories', page, filters],
  queryFn: () => api.getStories({ page, size: 20, ...filters }),
  keepPreviousData: true,
});
```

**Debounced Search**:
```typescript
const debouncedSearch = useMemo(
  () => debounce((query: string) => {
    setSearchQuery(query);
  }, 300),
  []
);
```

**Cached Metrics**:
```typescript
const { data: metrics } = useQuery({
  queryKey: ['ai-metrics', period],
  queryFn: () => api.getAIMetrics(period),
  staleTime: 30000, // 30 seconds
  refetchInterval: 30000,
});
```

### Backend API

**Database Indexing**:
```sql
-- Optimize story queries
CREATE INDEX idx_library_stories_status_category ON library_stories(status, category);
CREATE INDEX idx_library_stories_created_at ON library_stories(created_at DESC);

-- Optimize metrics queries
CREATE INDEX idx_ai_generation_logs_created_at_model ON ai_generation_logs(created_at, model_name);
```

**Redis Caching**:
```kotlin
@Cacheable(value = ["ai-metrics"], key = "#period")
fun getAIMetricsSummary(period: String): AIMetricsSummaryResponse {
    // Expensive calculation
}
```

**Query Optimization**:
```kotlin
// Avoid N+1 queries
@Query("SELECT s FROM StoryEntity s LEFT JOIN FETCH s.translations WHERE s.id = :id")
fun findByIdWithTranslations(id: Long): StoryEntity?
```

## Security

### Authentication

**JWT Tokens**:
- Access token: 15 minutes expiry
- Refresh token: 7 days expiry
- Stored securely (Keychain on iOS, EncryptedSharedPreferences on Android)

**Token Refresh**:
```kotlin
suspend fun refreshToken(): Result<TokenResponse> {
    val refreshToken = secureStorage.getRefreshToken()
    return api.refreshToken(RefreshTokenRequest(refreshToken))
}
```

### Authorization

**Role-Based Access Control**:
```kotlin
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/admin/stories")
fun createStory(@Valid @RequestBody request: AdminStoryRequest): AdminStoryResponse
```

**Resource Ownership**:
```kotlin
fun verifyOwnership(parentId: Long, storyId: Long) {
    val story = storyRepository.findById(storyId)
    if (story.parentId != parentId) {
        throw ForbiddenException("Not authorized to access this story")
    }
}
```

### Data Protection

**Encryption**:
- TLS 1.3 for data in transit
- AES-256 for data at rest
- Encrypted database backups

**Input Validation**:
```kotlin
data class AdminStoryRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title must be 200 characters or less")
    val title: String,
    
    @field:NotBlank(message = "Content is required")
    @field:Size(max = 10000, message = "Content must be 10000 characters or less")
    val content: String,
    
    @field:NotBlank(message = "Category is required")
    val category: String
)
```

**SQL Injection Prevention**:
```kotlin
// Use parameterized queries
@Query("SELECT * FROM library_stories WHERE title LIKE :search")
fun searchByTitle(@Param("search") search: String): List<StoryEntity>
```

## Testing

### Mobile App Tests

**Unit Tests**:
```kotlin
class DashboardViewModelTest {
    @Test
    fun `loadDashboard success updates state`() = runTest {
        val expectedHome = HomeResponse(/* ... */)
        coEvery { mockRepository.getHome() } returns expectedHome
        
        viewModel.loadDashboard()
        
        assertEquals(LoadState.Success, viewModel.loadState.value)
        assertEquals(expectedHome, viewModel.homeData.value)
    }
}
```

**UI Tests**:
```kotlin
class DashboardScreenTest {
    @Test
    fun `dashboard displays greeting with user name`() {
        composeTestRule.setContent {
            DashboardScreen(userName = "Maya")
        }
        
        composeTestRule.onNodeWithText("Good evening, Maya!")
            .assertIsDisplayed()
    }
}
```

### Backend Tests

**Unit Tests**:
```kotlin
class StoryServiceTest {
    @Test
    fun `createStory saves story with DRAFT status`() {
        val request = AdminStoryRequest(
            title = "Test Story",
            content = "Content",
            category = "adventure"
        )
        
        val result = service.createStory(request, "admin@example.com")
        
        assertEquals(StoryStatus.DRAFT, result.status)
        verify { mockRepository.save(any()) }
    }
}
```

**Integration Tests**:
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StoryControllerIntegrationTest {
    @Test
    fun `POST admin stories creates story`() {
        val request = AdminStoryRequest(/* ... */)
        
        val response = restTemplate
            .withBasicAuth("admin", "password")
            .postForEntity("/admin/stories", request, AdminStoryResponse::class.java)
        
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body?.id)
    }
}
```

### Admin Dashboard Tests

**Component Tests**:
```typescript
describe('StoryCard', () => {
  it('displays story title and status', () => {
    const story = { id: 1, title: 'Test Story', status: 'DRAFT' };
    render(<StoryCard story={story} />);
    
    expect(screen.getByText('Test Story')).toBeInTheDocument();
    expect(screen.getByText('DRAFT')).toBeInTheDocument();
  });
});
```

**E2E Tests**:
```typescript
test('creates new story', async ({ page }) => {
  await page.goto('/dashboard/stories');
  await page.click('text=New Story');
  
  await page.fill('[name="title"]', 'E2E Test Story');
  await page.fill('[name="content"]', 'Test content');
  await page.selectOption('[name="category"]', 'adventure');
  
  await page.click('text=Save Draft');
  
  await expect(page.locator('text=Story saved successfully')).toBeVisible();
});
```

## Monitoring & Observability

### Application Metrics

**Spring Boot Actuator**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Custom Metrics**:
```kotlin
@Component
class ApplicationMetrics(private val meterRegistry: MeterRegistry) {
    fun recordStoryGeneration(success: Boolean, latency: Long) {
        meterRegistry.counter("story.generation", 
            "success", success.toString()
        ).increment()
        
        meterRegistry.timer("story.generation.latency").record(latency, TimeUnit.MILLISECONDS)
    }
}
```

### Logging

**Structured Logging**:
```kotlin
logger.info("Story created", mapOf(
    "storyId" to story.id,
    "userId" to userId,
    "category" to story.category,
    "duration" to duration
))
```

**Log Levels**:
- ERROR: Failures requiring immediate attention
- WARN: Recoverable issues
- INFO: Business events
- DEBUG: Detailed diagnostic information

### Error Tracking

**Sentry Integration**:
```kotlin
Sentry.captureException(exception, mapOf(
    "userId" to userId,
    "storyId" to storyId,
    "operation" to "story_generation"
))
```

## Deployment

### Mobile App

**Android**:
```bash
./gradlew :mobile:androidApp:assembleRelease
```

**iOS**:
```bash
cd mobile/iosApp
xcodebuild -scheme iosApp -configuration Release archive
```

### Admin Dashboard

**Build**:
```bash
cd admin
npm run build
```

**Deploy to Vercel**:
```bash
vercel --prod
```

### Backend API

**Build**:
```bash
./gradlew :backend:bootJar
```

**Deploy**:
```bash
docker build -t tamixa-backend .
docker push tamixa-backend:latest
kubectl apply -f k8s/deployment.yaml
```

## Migration Guide

### For Existing Users

**Data Migration**:
1. Existing stories automatically get PUBLISHED status
2. Listening history preserved
3. Favorites maintained
4. Progress synced

**No Action Required**: Update happens automatically on app update.

### For Developers

**Update Dependencies**:
```kotlin
// Mobile
implementation("io.coil-kt:coil-compose:2.5.0")
implementation("androidx.compose.material3:material3:1.2.0")

// Backend
implementation("org.springframework.boot:spring-boot-starter-cache")
implementation("org.springframework.boot:spring-boot-starter-data-redis")
```

**Run Migrations**:
```bash
./gradlew :backend:flywayMigrate
```

**Update Environment Variables**:
```bash
# Add to .env
REDIS_HOST=localhost
REDIS_PORT=6379
AI_METRICS_CACHE_TTL=30
```

## Troubleshooting

### Common Issues

**Mobile App Won't Build**:
- Clean build: `./gradlew clean`
- Invalidate caches: Android Studio > File > Invalidate Caches
- Update dependencies: `./gradlew --refresh-dependencies`

**Admin Dashboard Build Fails**:
- Clear node_modules: `rm -rf node_modules && npm install`
- Clear Next.js cache: `rm -rf .next`
- Check Node version: `node --version` (should be 18+)

**Backend Tests Fail**:
- Check database connection
- Verify test data setup
- Run with `--info` flag for details

**Pipeline Stuck**:
- Check Redis connection
- Verify AI service availability
- Review error logs in database

## Support

### For Developers

**Documentation**:
- API Docs: https://api.tamixa.com/docs
- Mobile Docs: `/docs/mobile/`
- Admin Docs: `/docs/admin/`

**Code Repository**:
- GitHub: https://github.com/tamixa/tamixa-platform
- Issues: https://github.com/tamixa/tamixa-platform/issues

**Communication**:
- Slack: #engineering channel
- Email: dev@tamixa.com

### For Operations

**Monitoring**:
- Grafana: https://grafana.tamixa.com
- Sentry: https://sentry.io/tamixa
- AWS CloudWatch: Production logs

**Runbooks**:
- `/docs/runbooks/` directory
- Incident response procedures
- Deployment checklists

---

**Version**: 2.0 (Premium UX Overhaul)  
**Last Updated**: January 2025  
**Maintained By**: Engineering Team

For questions or clarifications, contact: engineering@tamixa.com
