# Tamixa Admin Dashboard User Guide

## Overview

The Tamixa Admin Dashboard is your content operations hub for managing stories, monitoring AI metrics, and overseeing the storytelling platform.

**Access**: https://admin.tamixa.com

## Getting Started

### Login

1. Navigate to the admin dashboard URL
2. Enter your admin credentials
3. Complete two-factor authentication if enabled

**First-time users**: Contact your administrator for account setup.

### Dashboard Layout

The dashboard consists of:
- **Top Navigation**: Main sections and user menu
- **Sidebar**: Quick navigation within sections
- **Main Content**: Current page content
- **Status Bar**: System notifications and alerts

## Story Management

### Linear Stories

Traditional sequential stories without branching.

#### Story List

**View all stories**:
1. Click "Stories" in top navigation
2. Use filters to narrow results:
   - Status (Draft, Published, Processing, etc.)
   - Category (Adventure, Learning, Fun, Safety)
   - Language
   - Created by

**Search**: Use the search bar to find stories by title or content.

**Pagination**: Navigate through pages (20 stories per page).

#### Story Status

Stories progress through these states:

- **DRAFT**: Being edited, not yet submitted
- **PROCESSING**: Translation and audio generation in progress
- **READY**: All languages complete, awaiting approval
- **PUBLISHED**: Live and available to users
- **CHANGES_REQUESTED**: Needs revision before approval
- **REJECTED**: Not approved for publication

#### Create New Story

1. Click "+ New Story" button
2. Fill in required fields:
   - **Title**: Story name (max 200 characters)
   - **Content**: Story text (max 10,000 characters)
   - **Category**: Adventure, Learning, Fun, or Safety
   - **Moral** (optional): Key lesson or takeaway
   - **Reading Level** (optional): 1-10
   - **Tags** (optional): Keywords for search

3. **Cover Image**:
   - Upload existing image, or
   - Click "Generate with AI" for automatic creation

4. Click "Save Draft" to save without submitting

#### Edit Story

1. Find story in list
2. Click "Edit" button
3. Make changes
4. Click "Save Draft"

**Note**: Cannot edit while status is PROCESSING.

#### Submit for Review

Once your story is ready:

1. Open story editor
2. Review all fields for accuracy
3. Click "Submit for Review"
4. Pipeline automatically starts:
   - Translation to all languages
   - Content rewriting for each language
   - Text-to-speech generation

**Pipeline Progress**: Watch real-time status for each language:
- Tamil
- English
- Hindi
- Telugu
- Kannada
- Malayalam

Each language shows:
- Translation status
- Rewrite status
- TTS status
- Overall status

**Typical Duration**: 5-10 minutes for all languages.

#### Pipeline Operations

**Regenerate All**: Restart pipeline for all languages
- Use when: Major content changes made
- Warning: Overwrites existing translations/audio

**Regenerate Failed**: Retry only failed languages
- Use when: Some languages failed but others succeeded
- Preserves: Successful language versions

**Regenerate Single Language**:
1. Click language-specific regenerate button
2. Confirm action
3. Monitor progress

#### Approve & Publish

When status is READY:

1. Review story content in all languages
2. Listen to audio samples (see Narration Workflow)
3. Click "Approve & Publish"
4. Story becomes available to users immediately

#### Request Changes

If story needs revision:

1. Click "Request Changes"
2. Add comment explaining what needs fixing
3. Story returns to DRAFT status
4. Creator receives notification

#### Unpublish Story

To remove from user access:

1. Open published story
2. Click "Unpublish"
3. Confirm action
4. Story status changes to DRAFT

**Note**: Users who already started listening can still finish.

### Interactive Stories

Branching narratives with decision points.

#### Interactive Story Structure

Interactive stories use a graph structure:
- **Segments**: Individual story nodes
- **Choices**: Decisions that branch to other segments
- **Paths**: Routes through the story
- **Endings**: Terminal segments with no choices

#### Create Interactive Story

1. Click "Interactive Stories" in navigation
2. Click "+ New Interactive Story"
3. Enter basic information:
   - Title
   - Category
   - Description

4. Choose editing mode:
   - **Graph View**: Visual node editor
   - **Outline View**: Text-based list

#### Graph View

Visual editor showing story structure.

**Add Segment**:
1. Click "+ Add Segment" button
2. Enter segment details:
   - Segment ID (unique identifier, e.g., "forest_entrance")
   - Title (displayed to users)
   - Content (story text)
   - Emotion Mode (neutral, happy, suspenseful, sad, excited)

3. Add choices:
   - Choice text (what user sees)
   - Target segment (where choice leads)

4. Click "Save"

**Connect Segments**:
- Drag from choice to target segment
- Visual line shows connection
- Click line to edit or delete

**Navigate Graph**:
- Zoom: Mouse wheel or pinch gesture
- Pan: Click and drag background
- Select: Click segment to edit

**Visual Indicators**:
- **Green border**: START segment
- **Red border**: Ending segment
- **Yellow border**: Orphaned segment (unreachable)
- **Dotted line**: Invalid connection

#### Outline View

Text-based alternative to graph view.

**Structure**:
```
START: forest_entrance
  → Choice 1: "Enter the forest" → deep_forest
  → Choice 2: "Go around" → village_path

deep_forest
  → Choice 1: "Follow the sound" → mysterious_cave
  → Choice 2: "Turn back" → forest_entrance

mysterious_cave (ENDING)
  (No choices - story ends)
```

**Edit in Outline**:
1. Click segment to expand
2. Edit text inline
3. Add/remove choices
4. Changes sync to graph view

#### Validate Graph

Before submitting, validate structure:

1. Click "Validate Graph" button
2. Review validation results:
   - ✅ All segments reachable from START
   - ✅ All choices point to valid segments
   - ✅ At least one ending segment
   - ❌ Orphaned segments detected
   - ❌ Circular references without escape

3. Fix any errors highlighted in red
4. Re-validate until all checks pass

**Common Issues**:

**Orphaned Segment**: Not reachable from START
- Solution: Add choice pointing to this segment

**Missing Ending**: No terminal segments
- Solution: Mark at least one segment as ending

**Invalid Choice**: Points to non-existent segment
- Solution: Update target segment ID or create missing segment

**Circular Loop**: Segments reference each other with no exit
- Solution: Add choice leading to different path

#### Submit Interactive Story

1. Validate graph (must pass all checks)
2. Click "Submit for Review"
3. Pipeline processes each segment:
   - Translation
   - Rewriting
   - TTS generation

**Progress Tracking**: Monitor per-segment, per-language status.

#### Bulk Operations

Select multiple segments:
1. Click checkbox on segments
2. Choose bulk action:
   - Delete selected
   - Change emotion mode
   - Regenerate audio

3. Confirm action

## Narration Workflow

Manage audio generation and approval.

### Access Narration Page

1. Navigate to story (linear or interactive)
2. Click "Narration" tab
3. View audio status for all languages

### Audio Status

Each language shows:
- **NOT_STARTED**: Audio not yet generated
- **QUEUED**: Waiting for generation
- **PROCESSING**: Currently generating
- **COMPLETED**: Audio ready for review
- **FAILED**: Generation error (see error message)

### Generate Audio

**Prerequisites**: Story must be in READY status.

**Generate All Languages**:
1. Click "Generate All Audio"
2. Confirm action
3. Monitor progress (typically 2-3 minutes per language)

**Generate Single Language**:
1. Click language-specific "Generate" button
2. Wait for completion

### Preview Audio

Before approving:

1. Click "Preview" button for language
2. Audio player opens with:
   - Play/pause controls
   - Progress bar
   - Duration
   - Waveform visualization

3. Listen to entire narration
4. Check for:
   - Correct pronunciation
   - Appropriate emotion/tone
   - Audio quality
   - Pacing

### Approve Audio

If audio is satisfactory:

1. Click "Approve" button for language
2. Add optional comment
3. Confirm approval

**Effect**: Audio becomes available to users for that language.

### Reject Audio

If audio needs regeneration:

1. Click "Reject" button
2. Add comment explaining issue:
   - Pronunciation errors
   - Wrong emotion
   - Audio artifacts
   - Pacing issues

3. Click "Regenerate" to create new version

### Bulk Audio Operations

**Approve All**:
- Use when: All languages sound good
- Click "Approve All Languages"
- Confirm action

**Regenerate All**:
- Use when: Major content changes
- Click "Regenerate All"
- Warning: Overwrites existing audio

### Audio Metadata

View technical details:
- File size
- Duration
- Sample rate
- Bit rate
- Generation timestamp
- Narrator voice ID

## AI Metrics Dashboard

Monitor AI system performance and usage.

### Access Metrics

1. Click "AI Metrics" in top navigation
2. Select time period:
   - Last Hour
   - Last 24 Hours
   - Last 7 Days
   - Last 30 Days

3. Click "Refresh" to update data

**Auto-refresh**: Dashboard updates every 30 seconds automatically.

### Key Metrics

**Stories Generated**: Total AI-generated stories in period
- Includes both successful and failed attempts
- Hover for breakdown by success/failure

**Tokens Used**: Total AI tokens consumed
- Input tokens (prompts)
- Output tokens (generated content)
- Total cost estimate

**Average Latency**: Mean response time for AI operations
- Story generation
- Translation
- Rewriting
- TTS

**Error Rate**: Percentage of failed AI operations
- Target: < 5%
- Warning indicator if > 5%
- Critical indicator if > 10%

### Story Generation Trend

Line chart showing generation volume over time.

**Insights**:
- Peak usage hours
- Day-of-week patterns
- Growth trends
- Anomaly detection

**Interactions**:
- Hover over points for exact values
- Click legend to show/hide series
- Drag to zoom into time range

### Token Consumption by Model

Pie chart showing token distribution across AI models.

**Models**:
- GPT-4: High-quality generation
- GPT-3.5: Faster, lower-cost option
- Gemini: Alternative provider

**Metrics per model**:
- Token count
- Percentage of total
- Cost estimate

### Model Performance Table

Detailed comparison of AI models.

**Columns**:
- **Model Name**: AI model identifier
- **Avg Latency**: Mean response time
- **Error Rate**: Failure percentage
- **Success Rate**: Success percentage
- **Request Count**: Total requests
- **Tokens Used**: Total tokens consumed

**Sorting**: Click column header to sort.

**Filtering**: Use search to filter by model name.

### Alerts & Warnings

Dashboard shows alerts when:
- Error rate exceeds 5%
- Latency exceeds 2 seconds
- Token usage exceeds budget
- Model unavailable

**Alert Actions**:
- View details
- Acknowledge
- Create incident ticket

### Export Data

Download metrics for analysis:

1. Click "Export" button
2. Select format:
   - CSV
   - JSON
   - Excel

3. Choose date range
4. Click "Download"

**Use cases**:
- Monthly reporting
- Cost analysis
- Performance tracking
- Capacity planning

## User Management

### View Users

1. Click "Users" in navigation
2. View list of all users:
   - Parents
   - Children
   - Admins

**Filters**:
- User type
- Subscription status
- Registration date
- Activity status

### User Details

Click user to view:
- Profile information
- Subscription details
- Listening history
- Generated stories
- Payment history

### Manage Subscriptions

**View subscription**:
- Current plan
- Billing cycle
- Next payment date
- Payment method

**Actions**:
- Upgrade/downgrade plan
- Cancel subscription
- Refund payment
- Extend trial

### Support Actions

**Reset password**:
1. Find user
2. Click "Reset Password"
3. Email sent to user

**Suspend account**:
1. Click "Suspend"
2. Enter reason
3. Confirm action
4. User cannot log in until unsuspended

**Delete account**:
1. Click "Delete"
2. Review data deletion policy
3. Confirm action
4. Permanent deletion after 30-day grace period

## Settings

### Admin Profile

Manage your admin account:
- Name and email
- Password
- Two-factor authentication
- Notification preferences

### System Settings

**Content Moderation**:
- Safety filters
- Blocked words list
- Age restrictions
- Content review queue

**AI Configuration**:
- Model selection
- Token limits
- Timeout settings
- Fallback options

**Notifications**:
- Email alerts
- Slack integration
- Webhook endpoints

### Audit Log

View all admin actions:
- User who performed action
- Action type
- Timestamp
- Affected resources
- IP address

**Filters**:
- Date range
- Admin user
- Action type
- Resource type

**Export**: Download audit log for compliance.

## Keyboard Shortcuts

Speed up your workflow:

**Navigation**:
- `Ctrl/Cmd + K`: Quick search
- `Ctrl/Cmd + /`: Show shortcuts
- `Esc`: Close modal/dialog

**Story Management**:
- `Ctrl/Cmd + S`: Save draft
- `Ctrl/Cmd + Enter`: Submit for review
- `Ctrl/Cmd + P`: Preview

**General**:
- `Ctrl/Cmd + R`: Refresh page
- `Ctrl/Cmd + F`: Find in page

## Best Practices

### Story Creation

1. **Write clear, engaging content**: Use age-appropriate language
2. **Include moral/lesson**: Help children learn
3. **Test in multiple languages**: Ensure translations make sense
4. **Review audio before approval**: Catch pronunciation errors
5. **Use descriptive titles**: Help users find stories

### Interactive Stories

1. **Plan structure first**: Sketch graph before creating
2. **Limit choices**: 2-3 options per decision point
3. **Provide meaningful choices**: Each should lead to different outcome
4. **Test all paths**: Ensure every route is playable
5. **Balance length**: Not too short or too long

### Content Review

1. **Check all languages**: Don't just approve English
2. **Listen to full audio**: Don't skip to save time
3. **Verify metadata**: Correct category, tags, reading level
4. **Test on mobile**: Ensure good user experience
5. **Document issues**: Leave clear feedback for creators

### Performance Monitoring

1. **Check metrics daily**: Catch issues early
2. **Set up alerts**: Get notified of problems
3. **Review trends weekly**: Identify patterns
4. **Export monthly reports**: Track progress over time
5. **Investigate anomalies**: Don't ignore unusual spikes/drops

## Troubleshooting

### Pipeline Stuck

**Symptoms**: Story processing for > 15 minutes

**Solutions**:
1. Check pipeline status page for errors
2. Click "Regenerate Failed" to retry
3. Contact technical support if persists

### Audio Generation Failed

**Common causes**:
- Content too long (> 10,000 characters)
- Special characters in text
- TTS service unavailable

**Solutions**:
1. Review error message
2. Shorten content if too long
3. Remove special characters
4. Retry generation
5. Try different narrator voice

### Metrics Not Loading

**Solutions**:
1. Refresh page
2. Check internet connection
3. Clear browser cache
4. Try different browser
5. Contact support if persists

### Cannot Approve Story

**Possible reasons**:
- Story not in READY status
- Audio not generated for all languages
- Missing required fields

**Solutions**:
1. Check story status
2. Verify all languages have audio
3. Complete all required fields
4. Refresh page and retry

## Support

### Get Help

**In-app support**:
- Click "?" icon in top right
- Search help articles
- Submit support ticket

**Email**: admin-support@tamixa.com

**Slack**: #admin-support channel (for team members)

### Report Bugs

When reporting issues, include:
- What you were trying to do
- What happened instead
- Steps to reproduce
- Screenshots if applicable
- Browser and OS version

### Feature Requests

We welcome suggestions! Submit via:
- In-app feedback form
- Email to product@tamixa.com
- Monthly feedback survey

## Security

### Access Control

- Use strong, unique passwords
- Enable two-factor authentication
- Don't share credentials
- Log out when finished

### Data Protection

- All data encrypted in transit and at rest
- Regular security audits
- GDPR and COPPA compliant
- Minimal data retention

### Incident Response

If you suspect a security issue:
1. **Do not** investigate further
2. **Do not** share with others
3. **Immediately** contact security@tamixa.com
4. Document what you observed

---

**Version**: 2.0 (Premium UX Overhaul)  
**Last Updated**: January 2025  
**For**: Content Operations Team

For technical documentation, see [Admin Technical Guide](./ADMIN_TECHNICAL_GUIDE.md)
