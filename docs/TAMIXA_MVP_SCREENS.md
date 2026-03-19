# Tamixa MVP Screen Blueprint

A UX blueprint for the Tamixa MVP: screens, layout, and flows a product designer or mobile developer can implement directly. Last updated for 1‑month launch scope.

---

## Screen Map & Flow

```
Splash
  ↓
Hook Screen (Onboarding Step 1)
  ↓
Demo Story (Magic Moment)
  ↓
[Story Preferences - optional]
  ↓
[Voice Recording - optional]
  ↓
[Avatar Upload - optional]
  ↓
Home Screen
   ├── Story Library
   ├── Story Player
   ├── Voice Selection
   ├── Avatar Story Video
   ├── Create Story (optional)
   └── Profile
```

---

## 1. Splash Screen

**Purpose:** Brand introduction (1–2 seconds)

```
------------------------------------------------
|                                              |
|               TAMIXA LOGO                    |
|        "Stories told by voices you love"     |
|                                              |
|                 Loading...                   |
------------------------------------------------
```

**Elements:** Logo, tagline, soft animation (optional)

---

## 2. Emotional Hook Screen (Onboarding Step 1)

**Purpose:** Explain Tamixa magic before login

```
------------------------------------------------
|      Illustration: Grandpa telling story     |
|                                              |
|   Stories told by the voices you love.      |
|   Bring family voices into bedtime stories   |
|                                              |
|        [ Start Story Magic ]                 |
|                 Skip                         |
------------------------------------------------
```

**Goal:** Communicate value before login

---

## 3. Demo Story Screen (Magic Moment)

**Purpose:** Show Tamixa experience instantly (~20 seconds)

```
------------------------------------------------
|                Scene Background              |
|            Talking Character Avatar          |
|----------------------------------------------|
| Subtitle                                     |
| "ஒரு அமைதியான காட்டில் ஒரு சிறுவன்..."      |
|----------------------------------------------|
| ▶️ Play  ⏸ Pause                             |
------------------------------------------------
```

**Features:** Talking character, subtitle, audio narration. Length: 20 seconds.

---

## 4. Story Preference Screen (Optional)

**Purpose:** Personalize recommendations

```
------------------------------------------------
|        What kind of stories do you like?     |
|   [ 🦊 Animal Stories ]                      |
|   [ 🧭 Adventure Stories ]                   |
|   [ 🤝 Friendship Stories ]                  |
|   [ 🌾 Village Life ]                        |
|   [ 😄 Funny Stories ]                       |
|           Select 2–3 options                  |
|               Continue                        |
------------------------------------------------
```

Stores user preferences for recommendations.

---

## 5. Voice Recording Invitation (Optional)

**Purpose:** Introduce emotional feature

```
------------------------------------------------
|        Want stories in your family voice?    |
|   Record a short voice sample to create      |
|   a storytelling voice.                     |
|           🎤 Record Voice                    |
|                 Skip                         |
------------------------------------------------
```

Recording length: 10–15 seconds.

---

## 6. Avatar Upload Screen (Optional)

**Purpose:** Enable talking avatar storytelling

```
------------------------------------------------
|        Who should tell the story?            |
|   Upload a photo of someone                  |
|   (grandparent, parent, etc.)                 |
|           [ Upload Photo ]                    |
|                 Skip                          |
------------------------------------------------
```

Future result: Grandpa avatar → talking story.

---

## 7. Home Screen

**Purpose:** Primary screen after onboarding

```
------------------------------------------------
| Hello, Achappan 👋                           |
| Continue Adventure                           |
| -------------------------------------------  |
| Murugan and the Lantern ▶️                   |
| Recommended Stories                          |
| [Story Card] [Story Card] [Story Card]       |
| Popular Stories                              |
| [Story Card] [Story Card] [Story Card]       |
| Categories: Animals | Adventure | Funny       |
------------------------------------------------
| Home | Library | Create | Profile            |
------------------------------------------------
```

---

## 8. Story Library Screen

**Purpose:** Browse all stories

```
------------------------------------------------
|               Story Library                  |
| 🔍 Search Stories                            |
| Categories: Animals | Adventure | Friendship  |
| All Stories                                  |
| [Story Card] [Story Card] [Story Card]       |
------------------------------------------------
```

**Story card:** Thumbnail, title, duration, play button.

---

## 9. Story Player Screen

**Purpose:** Main storytelling experience

```
------------------------------------------------
|                Scene Background              |
|             Talking Character                |
|----------------------------------------------|
| Subtitle                                     |
| "முருகன் மெதுவாக காட்டுக்குள் நடந்தான்..." |
|----------------------------------------------|
| ◀️ Previous   ▶️ Play   ⏭ Next                |
| Progress Bar                                 |
| ❤️ Favorite   🔊 Change Voice   📤 Share       |
------------------------------------------------
```

---

## 10. Voice Selection Screen

```
------------------------------------------------
|             Choose Voice                     |
| Default Narrator                             |
| Your Voices: Mom | Grandpa | My Voice         |
|             Add New Voice                    |
------------------------------------------------
```

---

## 11. Talking Avatar Video Screen

```
------------------------------------------------
|            Talking Avatar Story              |
|             Grandpa Avatar                   |
|----------------------------------------------|
| Subtitle                                     |
| ▶️ Play   📤 Share Clip                       |
------------------------------------------------
```

---

## 12. Create Story Screen (Optional AI)

**Purpose:** Optional feature for later

```
------------------------------------------------
|            Tell me a story                   |
| What story would you like to hear?           |
| [ Voice Input 🎤 ]                            |
| Example: "Tell me a story about a fox..."    |
| Generate Story                               |
------------------------------------------------
```

---

## 13. Profile Screen

**Purpose:** Central hub for user content and settings

```
------------------------------------------------
|                Profile                       |
| Achappan                                     |
| My Voices                                    |
| My Avatars                                   |
| Favorite Stories                             |
| Listening History                            |
| Premium Subscription                         |
| Settings                                     |
------------------------------------------------
```

---

## 14. Share Story Screen

```
------------------------------------------------
|            Share Story Clip                  |
| 15 sec preview                               |
| Share to: WhatsApp | Instagram | TikTok       |
| Copy Link                                    |
------------------------------------------------
```

---

## Recommended MVP Screens (1‑Month Launch)

| # | Screen           | Status |
|---|------------------|--------|
| 1 | Splash           | Required |
| 2 | Hook screen      | Required |
| 3 | Demo story       | Required |
| 4 | Home             | Required |
| 5 | Story library    | Required |
| 6 | Story player     | Required |
| 7 | Profile          | Required |
| 8 | Voice upload     | Required |
| 9 | Avatar upload    | Required |

**Deferred:** Story Preferences (can be post‑MVP), Voice Selection (in‑player), Talking Avatar Video (when backend ready), Create Story (optional), Share Story.

---

## Bottom Navigation (Blueprint)

- **Home** – Dashboard with continue adventure, recommended, popular, categories
- **Library** – Story Library screen
- **Create** – Create Story (optional for MVP)
- **Profile** – Profile hub (My Voices, Avatars, Favorites, History, Subscription, Settings)
