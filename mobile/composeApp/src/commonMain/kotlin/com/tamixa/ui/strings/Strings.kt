package com.tamixa.ui.strings

object Strings {
    var languageCode: String = com.tamixa.util.TamixaConstants.DEFAULT_LANGUAGE
        private set

    fun setLanguage(code: String) {
        languageCode = code
    }

    fun appName(): String = "Tamixa"

    fun appTagline(): String = when (languageCode) {
        "ta" -> "கேள் · கற்றுக்கொள் · ஒளிர்"
        "hi" -> "सुनें · सीखें · चमकें"
        else -> "Listen · Learn · Shine"
    }

    fun bedtimeReminderSubline(): String = when (languageCode) {
        "ta" -> "இன்று இரவு கதை கேட்க உங்களுக்கு நினைவூட்டுவோம்."
        "hi" -> "आज रात कहानी सुनने की याद दिलाएंगे।"
        else -> "We'll remind you to listen to a story tonight."
    }

    fun login(): String = when (languageCode) {
        "ta" -> "உள்நுழை"
        "hi" -> "लॉग इन"
        else -> "Login"
    }

    fun register(): String = when (languageCode) {
        "ta" -> "பதிவு"
        "hi" -> "पंजीकरण"
        else -> "Register"
    }

    /** Login screen: alternative path when the user prefers email + password. */
    fun registerWithEmail(): String = when (languageCode) {
        "ta" -> "மின்னஞ்சலுடன் பதிவு செய்யவும்"
        "hi" -> "ईमेल से पंजीकरण करें"
        else -> "Register with email"
    }

    fun storyQuiz(): String = when (languageCode) {
        "ta" -> "கதை வினாடி வினா"
        "hi" -> "कहानी क्विज़"
        else -> "Story quiz"
    }

    fun storyQuizNeedsChildProfile(): String = when (languageCode) {
        "ta" -> "வினாடி வினாவுக்கு குழந்தை சுயவிவரம் தேவை. இந்தக் கதையில் குழந்தை இணைக்கப்படவில்லை."
        "hi" -> "क्विज़ के लिए बच्चे की प्रोफ़ाइल चाहिए. इस कहानी में बच्चा लिंक नहीं है."
        else -> "A child profile is needed for the quiz. This story is not linked to a child."
    }

    fun storyQuizNoQuizAvailable(): String = when (languageCode) {
        "ta" -> "இந்தக் கதைக்கு இன்னும் வினாடி வினா இல்லை."
        "hi" -> "इस कहानी के लिए अभी कोई क्विज़ नहीं है."
        else -> "No quiz is available for this story yet."
    }

    fun storyQuizYourAnswerPlaceholder(): String = when (languageCode) {
        "ta" -> "உங்கள் பதில்"
        "hi" -> "आपका उत्तर"
        else -> "Your answer"
    }

    fun storyQuizQuestionCount(count: Int): String = when (languageCode) {
        "ta" -> if (count == 1) "1 கேள்வி" else "$count கேள்விகள்"
        "hi" -> if (count == 1) "1 प्रश्न" else "$count प्रश्न"
        else -> if (count == 1) "1 question" else "$count questions"
    }

    fun storyQuizSubmit(): String = when (languageCode) {
        "ta" -> "சமர்ப்பி"
        "hi" -> "सबमिट करें"
        else -> "Submit quiz"
    }

    fun storyQuizDone(): String = when (languageCode) {
        "ta" -> "முடிந்தது"
        "hi" -> "हो गया"
        else -> "Done"
    }

    fun storyQuizScoreLine(score: Int, max: Int): String = when (languageCode) {
        "ta" -> "$score / $max சரியானவை"
        "hi" -> "$score / $max सही"
        else -> "$score / $max correct"
    }

    fun myClassrooms(): String = when (languageCode) {
        "ta" -> "எனது வகுப்பறைகள்"
        "hi" -> "मेरी कक्षाएँ"
        else -> "My classrooms"
    }

    fun classroomJoinCardTitle(): String = when (languageCode) {
        "ta" -> "வகுப்பறையில் சேரவும்"
        "hi" -> "कक्षा में शामिल हों"
        else -> "Join a classroom"
    }

    fun classroomCodeLabel(): String = when (languageCode) {
        "ta" -> "வகுப்பு குறியீடு"
        "hi" -> "कक्षा कोड"
        else -> "Classroom code"
    }

    fun classroomCodePlaceholder(): String = when (languageCode) {
        "ta" -> "எ.கா. ABC123"
        "hi" -> "जैसे ABC123"
        else -> "e.g. ABC123"
    }

    fun classroomJoinButton(): String = when (languageCode) {
        "ta" -> "சேர்"
        "hi" -> "जोड़ें"
        else -> "Join"
    }

    fun classroomsEmptyMessage(): String = when (languageCode) {
        "ta" -> "இன்னும் வகுப்பறைகள் இல்லை. ஆசிரியரிடம் குறியீட்டைக் கேளுங்கள்!"
        "hi" -> "अभी कोई कक्षा नहीं। अपने शिक्षक से कोड माँगें!"
        else -> "No classrooms yet. Ask your teacher for a code!"
    }

    fun classroomCodeDisplay(code: String): String = when (languageCode) {
        "ta" -> "குறியீடு: $code"
        "hi" -> "कोड: $code"
        else -> "Code: $code"
    }

    fun classroomGradeLabel(grade: String): String = when (languageCode) {
        "ta" -> "வகுப்பு $grade"
        "hi" -> "कक्षा $grade"
        else -> "Grade $grade"
    }

    fun dashboardDailySparkAtIndex(dayBucket: Int): String {
        val tips = dashboardDailySparkTips()
        if (tips.isEmpty()) return ""
        val n = tips.size
        val idx = ((dayBucket % n) + n) % n
        return tips[idx]
    }

    private fun dashboardDailySparkTips(): List<String> = when (languageCode) {
        "ta" -> listOf(
            "இன்றிரவு ஒரு புதிய குரலில் கேட்க முயலுங்கள் — சிறு மாற்றம் பெரிய நினைவுகள்.",
            "கதைக்குப் பிறகு: உங்களுக்குப் பிடித்த கதாபாத்திரம் யார் எனக் கேளுங்கள்.",
            "நாளைய கதையை யார் தேர்வு செய்வது — நீங்கள், குழந்தை, அல்லது ஆச்சரியம்?",
            "பிளே அழுத்தும் முன் தலைப்பை உரக்கப் படியுங்கள் — ஆர்வம் இங்கே தொடங்குகிறது.",
            "கதாநாயகனின் பெயரை ஒன்றாக மெதுவாகச் சொல்லுங்கள் — நண்பர்கள் போல் உணர்வார்கள்.",
            "தொடங்குவதற்கு இரண்டு நிமிடங்கள் முன் விளக்குகளை மங்கலாக்குங்கள் — இது அமைதி நேரம் என்று மூளைக்குத் தெரியும்.",
        )
        "hi" -> listOf(
            "आज रात नई आवाज़ में सुनने की कोशिश करें — छोटे बदलाव, बड़ी यादें।",
            "कहानी के बाद पूछें: आपको कौन सा पात्र सबसे अच्छा लगा?",
            "कल की कहानी कौन चुनेगा — आप, बच्चा, या सरप्राइज़?",
            "प्ले दबाने से पहले शीर्षक ज़ोर से पढ़ें — जिज्ञासा यहीं शुरू होती है।",
            "नायक का नाम साथ में धीरे से बोलें — पात्र दोस्त जैसे लगेंगे।",
            "शुरू करने से दो मिनट पहले लाइट कम करें — दिमाग को पता चले कि यह आराम का समय है।",
        )
        else -> listOf(
            "Tonight, try a story in a new voice — tiny rituals make big memories.",
            "After listening, ask which character they’d invite to dinner.",
            "Take turns picking tomorrow’s tale: parent, child, or surprise pick.",
            "Read the title out loud before you press play — curiosity starts there.",
            "Say the hero’s name together softly — characters feel like friends.",
            "Dim the lights two minutes early — brains learn it’s cozy time.",
        )
    }

    fun readingStreakTitle(): String = when (languageCode) {
        "ta" -> "படிப்புத் தொடர்"
        "hi" -> "पढ़ने की लकीर"
        else -> "Reading streak"
    }

    fun profileLearningProgressSection(): String = when (languageCode) {
        "ta" -> "கற்றல் மற்றும் முன்னேற்றம்"
        "hi" -> "सीखने और प्रगति"
        else -> "Learning & progress"
    }

    fun streakLengthSubtitle(days: Int): String = when (languageCode) {
        "ta" -> if (days == 1) "நாள் தொடர்" else "நாட்கள் தொடர்"
        "hi" -> if (days == 1) "दिन की लकीर" else "दिनों की लकीर"
        else -> if (days == 1) "day streak" else "days streak"
    }

    fun streakStatusActive(): String = when (languageCode) {
        "ta" -> "🔥 செயலில்"
        "hi" -> "🔥 सक्रिय"
        else -> "🔥 Active"
    }

    fun streakBestLabel(): String = when (languageCode) {
        "ta" -> "சிறந்த தொடர்"
        "hi" -> "सबसे लंबी लकीर"
        else -> "Best streak"
    }

    fun streakBestDays(count: Int): String = when (languageCode) {
        "ta" -> "$count நாட்கள்"
        "hi" -> "$count दिन"
        else -> if (count == 1) "1 day" else "$count days"
    }

    fun streakLastReadLabel(): String = when (languageCode) {
        "ta" -> "கடைசியாகக் கேட்டது"
        "hi" -> "आख़िरी बार सुना"
        else -> "Last listened"
    }

    fun streakNoDataYet(): String = when (languageCode) {
        "ta" -> "இன்னும் தொடர் தரவு இல்லை. கதை கேட்டுத் தொடங்குங்கள்!"
        "hi" -> "अभी कोई स्ट्रीक डेटा नहीं। सुनना शुरू करें!"
        else -> "No streak data yet. Start listening to build one!"
    }

    fun readingLevelTitle(): String = when (languageCode) {
        "ta" -> "படிப்பு நிலை"
        "hi" -> "पढ़ने का स्तर"
        else -> "Reading level"
    }

    fun readingLevelNumber(level: Int): String = when (languageCode) {
        "ta" -> "நிலை $level"
        "hi" -> "स्तर $level"
        else -> "Level $level"
    }

    fun readingLevelOutOf(max: Int): String = when (languageCode) {
        "ta" -> "$max இல்"
        "hi" -> "$max में से"
        else -> "out of $max"
    }

    fun readingLevelLastUpdated(): String = when (languageCode) {
        "ta" -> "கடைசி புதுப்பிப்பு"
        "hi" -> "आख़री अपडेट"
        else -> "Last updated"
    }

    fun readingLevelNoDataYet(): String = when (languageCode) {
        "ta" -> "இன்னும் படிப்பு நிலை தரவு இல்லை."
        "hi" -> "अभी पढ़ने के स्तर का डेटा नहीं है।"
        else -> "No reading level data yet."
    }

    fun vocabularyTitle(): String = when (languageCode) {
        "ta" -> "சொற்களஞ்சியம்"
        "hi" -> "शब्दावली"
        else -> "Vocabulary"
    }

    fun vocabularyProgress(): String = when (languageCode) {
        "ta" -> "முன்னேற்றம்"
        "hi" -> "प्रगति"
        else -> "Progress"
    }

    fun vocabularyWordsLearned(count: Int): String = when (languageCode) {
        "ta" -> "$count கற்றுக்கொண்டவை"
        "hi" -> "$count सीखे"
        else -> "$count learned"
    }

    fun vocabularyWordsMastered(count: Int): String = when (languageCode) {
        "ta" -> "$count தேர்ச்சி"
        "hi" -> "$count में निपुण"
        else -> "$count mastered"
    }

    fun vocabularyMyWords(): String = when (languageCode) {
        "ta" -> "என் சொற்கள்"
        "hi" -> "मेरे शब्द"
        else -> "My words"
    }

    fun vocabularySuggestedWords(): String = when (languageCode) {
        "ta" -> "பரிந்துரை சொற்கள்"
        "hi" -> "सुझाए गए शब्द"
        else -> "Suggested words"
    }

    fun vocabularyLearnAction(): String = when (languageCode) {
        "ta" -> "கற்க"
        "hi" -> "सीखें"
        else -> "Learn"
    }

    fun vocabularyMasteryLabel(level: Int): String = when (languageCode) {
        "ta" -> when (level) {
            3 -> "நிபுணர்"
            2 -> "தேர்ச்சி"
            1 -> "பழக்கம்"
            else -> "கற்றல்"
        }
        "hi" -> when (level) {
            3 -> "विशेषज्ञ"
            2 -> "निपुण"
            1 -> "परिचित"
            else -> "सीख रहे"
        }
        else -> when (level) {
            3 -> "Expert"
            2 -> "Proficient"
            1 -> "Familiar"
            else -> "Learning"
        }
    }

    fun email(): String = when (languageCode) {
        "ta" -> "மின்னஞ்சல்"
        "hi" -> "ईमेल"
        else -> "Email"
    }

    fun password(): String = when (languageCode) {
        "ta" -> "கடவுச்சொல்"
        "hi" -> "पासवर्ड"
        else -> "Password"
    }

    fun children(): String = when (languageCode) {
        "ta" -> "குழந்தைகள்"
        "hi" -> "बच्चे"
        else -> "Children"
    }

    fun addChild(): String = when (languageCode) {
        "ta" -> "குழந்தையைச் சேர்"
        "hi" -> "बच्चा जोड़ें"
        else -> "Add Child"
    }

    fun name(): String = when (languageCode) {
        "ta" -> "பெயர்"
        "hi" -> "नाम"
        else -> "Name"
    }

    fun dateOfBirth(): String = when (languageCode) {
        "ta" -> "பிறந்த தேதி"
        "hi" -> "जन्म तिथि"
        else -> "Date of Birth"
    }

    fun languagePreference(): String = when (languageCode) {
        "ta" -> "மொழி விருப்பம்"
        "hi" -> "भाषा पसंद"
        else -> "Language Preference"
    }

    fun stories(): String = when (languageCode) {
        "ta" -> "கதைகள்"
        "hi" -> "कहानियाँ"
        else -> "Stories"
    }

    fun generateStory(): String = when (languageCode) {
        "ta" -> "கதையை உருவாக்கு"
        "hi" -> "कहानी बनाएं"
        else -> "Generate Story"
    }

    fun theme(): String = when (languageCode) {
        "ta" -> "தீம்"
        "hi" -> "थीम"
        else -> "Theme"
    }

    fun emotionMode(): String = when (languageCode) {
        "ta" -> "நிலை"
        "hi" -> "मोड"
        else -> "Mode"
    }

    fun parentInstructions(): String = when (languageCode) {
        "ta" -> "பெற்றோரின் விருப்பம்"
        "hi" -> "अभिभावक निर्देश (वैकल्पिक)"
        else -> "Parent instructions (optional)"
    }

    fun interests(): String = when (languageCode) {
        "ta" -> "விருப்பங்கள்"
        "hi" -> "रुचियाँ"
        else -> "Interests"
    }

    fun childName(): String = when (languageCode) {
        "ta" -> "குழந்தையின் பெயர்"
        "hi" -> "बच्चे का नाम"
        else -> "Child Name"
    }

    fun age(): String = when (languageCode) {
        "ta" -> "வயது"
        "hi" -> "उम्र"
        else -> "Age"
    }

    fun language(): String = when (languageCode) {
        "ta" -> "மொழி"
        "hi" -> "भाषा"
        else -> "Language"
    }

    fun voiceUpload(): String = when (languageCode) {
        "ta" -> "குரல் பதிவேற்றம்"
        "hi" -> "आवाज़ अपलोड"
        else -> "Voice Upload"
    }

    fun subscription(): String = when (languageCode) {
        "ta" -> "சந்தா"
        "hi" -> "सदस्यता"
        else -> "Subscription"
    }

    fun settings(): String = when (languageCode) {
        "ta" -> "அமைப்புகள்"
        "hi" -> "सेटिंग्स"
        else -> "Settings"
    }

    fun darkMode(): String = when (languageCode) {
        "ta" -> "இருண்ட முறை"
        "hi" -> "डार्क मोड"
        else -> "Dark Mode"
    }

    fun useSystemTheme(): String = when (languageCode) {
        "ta" -> "அமைப்பு வண்ணம் (பகல்/இரவு)"
        "hi" -> "सिस्टम थीम (दिन/रात)"
        else -> "Use system theme (day/night)"
    }

    fun serverEnvironmentTitle(): String = when (languageCode) {
        "ta" -> "சேவையகம் (மேம்பட்டது)"
        "hi" -> "सर्वर (उन्नत)"
        else -> "Server (advanced)"
    }

    fun serverEnvironmentDescription(): String = when (languageCode) {
        "ta" -> "சோதனை / ஸ்டேஜிங். API மாற்றத்திற்கு பயன்பாட்டை மீண்டும் தொடங்கவும்."
        "hi" -> "टेस्ट / स्टेजिंग। API बदलने के लिए ऐप पूरी तरह बंद करके खोलें।"
        else -> "For testing or staging. Fully restart the app after changing the API URL."
    }

    fun apiBaseUrlHint(): String = when (languageCode) {
        "ta" -> "API அடிப்படை URL (https://…, பாதை இல்லாமல்)"
        "hi" -> "API बेस URL (https://…, बिना पथ)"
        else -> "API base URL (https://…, no path)"
    }

    fun subscriptionWebUrlHint(): String = when (languageCode) {
        "ta" -> "சந்தா / நிர்வாக URL (முழு https)"
        "hi" -> "सदस्यता प्रबंधन URL (पूरा https)"
        else -> "Subscription manage URL (full https)"
    }

    fun saveServerEnvironment(): String = when (languageCode) {
        "ta" -> "சேமி"
        "hi" -> "सहेजें"
        else -> "Save"
    }

    fun clearServerEnvironment(): String = when (languageCode) {
        "ta" -> "இயல்புநிலைக்கு மீட்டமை"
        "hi" -> "डिफ़ॉल्ट पर रीसेट"
        else -> "Reset to defaults"
    }

    fun serverEnvironmentInvalidUrl(): String = when (languageCode) {
        "ta" -> "http:// அல்லது https:// உடன் செல்லுபடியாகும் URL ஐ உள்ளிடவும்."
        "hi" -> "मान्य URL http:// या https:// से शुरू होना चाहिए।"
        else -> "Enter a valid URL starting with http:// or https://."
    }

    fun serverEnvironmentSavedHint(): String = when (languageCode) {
        "ta" -> "சேமிக்கப்பட்டது. சந்தா இணைப்பு உடனே புதுப்பிக்கப்படும். API க்கு பயன்பாட்டை முழுவதுமாக மீண்டும் தொடங்கவும்."
        "hi" -> "सहेजा गया। सदस्यता लिंक तुरंत अपडेट। API के लिए ऐप पूरी तरह रीस्टार्ट करें।"
        else -> "Saved. Subscription link updates now. Fully restart the app for the new API server."
    }

    fun serverEnvironmentClearedHint(): String = when (languageCode) {
        "ta" -> "மீட்டமைக்கப்பட்டது. மாற்றங்களுக்கு பயன்பாட்டை மீண்டும் தொடங்கவும்."
        "hi" -> "रीसेट। परिवर्तनों के लिए ऐप रीस्टार्ट करें।"
        else -> "Reset. Restart the app to apply."
    }

    fun selectLanguage(): String = when (languageCode) {
        "ta" -> "மொழியைத் தேர்ந்தெடு"
        "hi" -> "भाषा चुनें"
        else -> "Select Language"
    }

    fun tamil(): String = when (languageCode) {
        "ta" -> "தமிழ்"
        "hi" -> "तामिल"
        else -> "Tamil"
    }

    fun english(): String = when (languageCode) {
        "ta" -> "ஆங்கிலம்"
        "hi" -> "अंग्रेज़ी"
        else -> "English"
    }

    fun hindi(): String = when (languageCode) {
        "ta" -> "இந்தி"
        "hi" -> "हिन्दी"
        else -> "Hindi"
    }

    fun telugu(): String = when (languageCode) {
        "ta" -> "தெலுங்கு"
        "te" -> "తెలుగు"
        else -> "Telugu"
    }

    fun kannada(): String = when (languageCode) {
        "ta" -> "கன்னடம்"
        "kn" -> "ಕನ್ನಡ"
        else -> "Kannada"
    }

    fun malayalam(): String = when (languageCode) {
        "ta" -> "மலையாளம்"
        "ml" -> "മലയാളം"
        else -> "Malayalam"
    }

    fun loading(): String = when (languageCode) {
        "ta" -> "ஏற்றுகிறது..."
        "hi" -> "लोड हो रहा है..."
        else -> "Loading..."
    }

    fun generatingStory(): String = when (languageCode) {
        "ta" -> "கதை உருவாக்கப்படுகிறது..."
        "hi" -> "कहानी बन रही है..."
        else -> "Creating your story..."
    }

    fun retry(): String = when (languageCode) {
        "ta" -> "மீண்டும் முயற்சிக்கவும்"
        "hi" -> "पुनः प्रयास करें"
        else -> "Retry"
    }

    fun play(): String = when (languageCode) {
        "ta" -> "இயக்கு"
        "hi" -> "चलाएं"
        else -> "Play"
    }

    /** Hero / featured row: TalkBack hint when the whole row opens the story. */
    fun openStoryAction(): String = when (languageCode) {
        "ta" -> "திறக்க தட்டவும்"
        "hi" -> "खोलने के लिए टैप करें"
        else -> "Tap to open story"
    }

    /** Featured carousel: current slide index is 1-based for parents. */
    fun featuredStoriesPager(oneBasedIndex: Int, total: Int): String = when (languageCode) {
        "ta" -> "சிறப்பு கதைகள், $oneBasedIndex / $total"
        "hi" -> "विशेष कहानियाँ, $oneBasedIndex में से $total"
        else -> "Featured stories, $oneBasedIndex of $total"
    }

    fun pause(): String = when (languageCode) {
        "ta" -> "இடைநிறுத்து"
        "hi" -> "रोकें"
        else -> "Pause"
    }

    fun logout(): String = when (languageCode) {
        "ta" -> "வெளியேறு"
        "hi" -> "लॉग आउट"
        else -> "Logout"
    }

    fun logoutConfirmTitle(): String = when (languageCode) {
        "ta" -> "வெளியேறவா?"
        "hi" -> "लॉग आउट करें?"
        else -> "Log out?"
    }

    fun logoutConfirmMessage(): String = when (languageCode) {
        "ta" -> "நீங்கள் வெளியேறுவீர்கள். மீண்டும் உள்நுழைய நீங்கள் உள்நுழைய வேண்டும்."
        "hi" -> "आप लॉग आउट हो जाएंगे। दोबारा प्रवेश करने के लिए साइन इन करें।"
        else -> "You will be signed out. Sign in again to continue."
    }

    fun deleteAccount(): String = when (languageCode) {
        "ta" -> "கணக்கை நீக்கு"
        "hi" -> "खाता हटाएं"
        else -> "Delete account"
    }

    fun deleteAccountDescription(): String = when (languageCode) {
        "ta" -> "உங்கள் கணக்கு மற்றும் அனைத்து தரவையும் நிரந்தரமாக நீக்குங்கள். இதை மீளமைக்க முடியாது."
        "hi" -> "अपना खाता और सभी डेटा स्थायी रूप से हटाएं। इसे पूर्ववत नहीं किया जा सकता।"
        else -> "Permanently delete your account and all data. This cannot be undone."
    }

    fun deleteAccountConfirm(): String = when (languageCode) {
        "ta" -> "நீக்கு"
        "hi" -> "हटाएं"
        else -> "Delete"
    }

    fun newStory(): String = when (languageCode) {
        "ta" -> "புதியக் கதை"
        "hi" -> "नई कहानी"
        else -> "New Story"
    }

    fun goodMorning(): String = when (languageCode) {
        "ta" -> "காலை வணக்கம்"
        "hi" -> "शुभ प्रभात"
        else -> "Good morning"
    }

    fun goodAfternoon(): String = when (languageCode) {
        "ta" -> "மதிய வணக்கம்"
        "hi" -> "शुभ दोपहर"
        else -> "Good afternoon"
    }

    fun goodEvening(): String = when (languageCode) {
        "ta" -> "மாலை வணக்கம்"
        "hi" -> "शुभ संध्या"
        else -> "Good evening"
    }

    fun chooseLanguage(): String = when (languageCode) {
        "ta" -> "மொழியைத் தேர்ந்தெடுங்கள்"
        "hi" -> "भाषा चुनें"
        else -> "Choose your language"
    }

    fun passwordlessLogin(): String = when (languageCode) {
        "ta" -> "பாஸ்வர்ட் இல்லாமல் உள்நுழை"
        "hi" -> "बिना पासवर्ड लॉगिन"
        else -> "Passwordless login"
    }

    fun mobileNumber(): String = when (languageCode) {
        "ta" -> "மொபைல் எண்"
        "hi" -> "मोबाइल नंबर"
        else -> "Mobile number"
    }

    fun enterMobileNumber(): String = when (languageCode) {
        "ta" -> "மொபைல் எண்ணை பதிவிடவும்"
        "hi" -> "मोबाइल नंबर दर्ज करें"
        else -> "Enter mobile number"
    }

    fun sendOtp(): String = when (languageCode) {
        "ta" -> "OTPயை அனுப்பு"
        "hi" -> "OTP भेजें"
        else -> "Send OTP"
    }

    fun continueWith(): String = when (languageCode) {
        "ta" -> "தொடர்க..."
        "hi" -> "जारी रखें"
        else -> "Continue"
    }

    fun chooseSignIn(): String = when (languageCode) {
        "ta" -> "உள்நுழையத் தேர்ந்தெடு"
        "hi" -> "साइन इन चुनें"
        else -> "Choose how to sign in"
    }

    fun usageThisMonth(): String = when (languageCode) {
        "ta" -> "இந்த மாத பயன்பாடு"
        "hi" -> "इस महीने का उपयोग"
        else -> "Usage this month"
    }

    /** e.g. "3 / 5 stories this month" or "Unlimited" */
    fun usageStoriesSummary(used: Int, limit: Int?): String = when (languageCode) {
        "ta" -> if (limit != null) "இந்த மாதம் $used / $limit கதைகள்" else "வரம்பற்ற கதைகள்"
        "hi" -> if (limit != null) "इस महीने $used / $limit कहानियाँ" else "असीमित कहानियाँ"
        else -> if (limit != null) "$used / $limit stories this month" else "Unlimited stories"
    }

    fun storiesUsed(): String = when (languageCode) {
        "ta" -> "கதைகள் பயன்படுத்தப்பட்டது"
        "hi" -> "कहानियाँ उपयोग"
        else -> "Stories used"
    }

    fun voiceGenerations(): String = when (languageCode) {
        "ta" -> "குரல் உருவாக்கங்கள்"
        else -> "Voice generations"
    }

    fun cancelAtPeriodEnd(): String = when (languageCode) {
        "ta" -> "கால முடிவில் ரத்து செய்"
        else -> "Cancel at period end"
    }

    fun canceling(): String = when (languageCode) {
        "ta" -> "ரத்து செய்கிறது..."
        else -> "Canceling..."
    }

    fun manage(): String = when (languageCode) {
        "ta" -> "மேலாண்மை"
        else -> "Manage"
    }

    fun subscribe(): String = when (languageCode) {
        "ta" -> "சந்தா செய்"
        else -> "Subscribe"
    }

    fun listeningProgress(): String = when (languageCode) {
        "ta" -> "கேட்டுக்கொண்டிருக்கிறது..."
        else -> "Listening progress"
    }

    fun storiesStarted(): String = when (languageCode) {
        "ta" -> "கதைகள் தொடங்கப்பட்டது!"
        else -> "Stories started"
    }

    fun storiesCompleted(): String = when (languageCode) {
        "ta" -> "கதைகள் முடிந்தது!"
        else -> "Stories completed"
    }

    fun completionRate(): String = when (languageCode) {
        "ta" -> "முடிவு விகிதம்"
        else -> "Completion rate"
    }

    fun consentHistory(): String = when (languageCode) {
        "ta" -> "ஒப்புதல் வரலாறு"
        else -> "Consent history"
    }

    fun noConsentRecords(): String = when (languageCode) {
        "ta" -> "இன்னும் சம்மத பதிவுகள் இல்லை"
        else -> "No consent records yet"
    }

    fun dataExport(): String = when (languageCode) {
        "ta" -> "தரவு ஏற்றுமதி"
        else -> "Data export"
    }

    fun requestDataExport(): String = when (languageCode) {
        "ta" -> "தரவு ஏற்றுமதி கோரிக்கை"
        else -> "Request data export"
    }

    fun requesting(): String = when (languageCode) {
        "ta" -> "கோருகிறது..."
        else -> "Requesting…"
    }

    fun download(): String = when (languageCode) {
        "ta" -> "பதிவிறக்கு"
        else -> "Download"
    }

    fun back(): String = when (languageCode) {
        "ta" -> "பின்"
        else -> "Back"
    }

    fun next(): String = when (languageCode) {
        "ta" -> "அடுத்தது"
        "hi" -> "अगला"
        else -> "Next"
    }

    fun refresh(): String = when (languageCode) {
        "ta" -> "புதுப்பிப்பு"
        "hi" -> "रीफ़्रेश"
        else -> "Refresh"
    }

    fun cancel(): String = when (languageCode) {
        "ta" -> "ரத்து"
        "hi" -> "रद्द करें"
        else -> "Cancel"
    }

    fun close(): String = when (languageCode) {
        "ta" -> "மூடு"
        "hi" -> "बंद करें"
        else -> "Close"
    }

    fun activePlan(): String = when (languageCode) {
        "ta" -> "செயலில் உள்ள திட்டம்"
        else -> "Active plan"
    }

    fun noActivePlan(): String = when (languageCode) {
        "ta" -> "செயலில் உள்ள திட்டம் இல்லை"
        else -> "No active plan"
    }

    fun plan(): String = when (languageCode) {
        "ta" -> "திட்டம்"
        else -> "Plan"
    }

    fun endsAt(): String = when (languageCode) {
        "ta" -> "முடிவடைகிறது"
        else -> "Ends"
    }

    fun cancelsAtPeriodEnd(): String = when (languageCode) {
        "ta" -> "கால முடிவில் ரத்து செய்யப்படும்"
        else -> "Cancels at period end"
    }

    fun maxChildren(): String = when (languageCode) {
        "ta" -> "அதிகபட்ச குழந்தைகள்"
        else -> "Max children"
    }

    fun subscriptionFullAccess(): String = when (languageCode) {
        "ta" -> "கதைகள் மற்றும் ஒலிகளை முழுமையாக அணுகும் அனுமதி உங்களுக்கு உள்ளது."
        else -> "You have full access to stories and audio."
    }

    fun subscribeToUnlock(): String = when (languageCode) {
        "ta" -> "அனைத்து அம்சங்களையும் பயன்படுத்த சந்தா செலுத்துங்கள்."
        else -> "Subscribe to unlock all features."
    }

    fun premiumBenefits(): String = when (languageCode) {
        "ta" -> "பிரீமியம் நன்மைகள்"
        "hi" -> "प्रीमियम लाभ"
        else -> "Premium benefits"
    }

    fun benefitRecordYourVoice(): String = when (languageCode) {
        "ta" -> "கதைகளுக்காக உங்கள் சொந்த குரலை பதிவு செய்யுங்கள்."
        "hi" -> "अपनी आवाज़ में कहानियाँ रिकॉर्ड करें"
        else -> "Record your own voice for stories"
    }

    fun benefitUnlimitedStories(): String = when (languageCode) {
        "ta" -> "வரம்பற்ற கதைகள்"
        "hi" -> "असीमित कहानियाँ"
        else -> "Unlimited stories"
    }

    fun benefitAdFree(): String = when (languageCode) {
        "ta" -> "விளம்பரமற்ற"
        "hi" -> "विज्ञापन मुक्त"
        else -> "Ad-free experience"
    }

    fun voiceUploadPremiumRequired(): String = when (languageCode) {
        "ta" -> "கதைகளில் உங்கள் குரலை பதிவேற்றவும் மற்றும் பயன்படுத்தவும் சந்தா செய்யுங்கள்."
        "hi" -> "कहानियों में अपनी आवाज़ अपलोड और इस्तेमाल करने के लिए सब्सक्राइब करें."
        else -> "Subscribe to upload and use your own voice in stories."
    }

    /** In-player dialog when user has no cloned voice: ask to upload voice (Premium). */
    fun uploadVoicePremiumPrompt(): String = when (languageCode) {
        "ta" -> "கதைகளை உங்கள் குரலில் கேட்க குரல் பதிவேற்றுங்கள். பிரீமியம்."
        "hi" -> "कहानियाँ अपनी आवाज़ में सुनने के लिए आवाज़ अपलोड करें. प्रीमियम."
        else -> "Upload your voice to tell stories in your voice. Premium."
    }

    /** Avatar requires voice + image (Super premium). */
    fun avatarSuperPremiumPrompt(): String = when (languageCode) {
        "ta" -> "கதை சொல்லும் அவதாரத்திற்கு குரல் மற்றும் படம் பதிவேற்றுங்கள். சூப்பர் பிரீமியம்."
        "hi" -> "कहानी अवतार के लिए आवाज़ और फोटो अपलोड करें. सुपर प्रीमियम."
        else -> "Upload your voice and photo for storytelling avatar. Super premium."
    }

    fun pricePerMonthInr(): String = when (languageCode) {
        "ta" -> "மாதம்"
        "hi" -> "महीना"
        else -> "per month"
    }

    fun cancelAtPeriodEndMessage(): String = when (languageCode) {
        "ta" -> "நடப்பு காலம் முடியும் வரை நீங்கள் அணுகலைத் தொடர்வீர்கள்."
        else -> "You'll keep access until the end of the current period."
    }

    fun childDetails(): String = when (languageCode) {
        "ta" -> "குழந்தையின் விவரங்கள்"
        else -> "Child details"
    }

    fun childConsentMessage(): String = when (languageCode) {
        "ta" -> "தனிப்பட்ட கதைசொல்லலுக்கு என் குழந்தையின் தரவு சேகரிப்பிற்கு நான் சம்மதிக்கிறேன்"
        else -> "I consent to the collection of my child's data for personalized storytelling"
    }

    fun dataExportDescription(): String = when (languageCode) {
        "ta" -> "உங்கள் தரவின் நகலைக் கோருங்கள் (GDPR/DPDP)"
        else -> "Request a copy of your data (GDPR/DPDP)"
    }

    fun sendCode(): String = when (languageCode) {
        "ta" -> "குறியீடு அனுப்பவும்"
        else -> "Send code"
    }

    fun enterCode(): String = when (languageCode) {
        "ta" -> "குறியீடு உள்ளிடவும்"
        else -> "Enter code"
    }

    fun code(): String = when (languageCode) {
        "ta" -> "குறியீடு"
        else -> "Code"
    }

    fun invalidEmail(): String = when (languageCode) {
        "ta" -> "செல்லுபடியான மின்னஞ்சலை உள்ளிடவும்"
        "hi" -> "मान्य ईमेल दर्ज करें"
        else -> "Please enter a valid email address"
    }

    fun invalidCode(): String = when (languageCode) {
        "ta" -> "குறியீடு 4-8 இலக்கங்களாக இருக்க வேண்டும்"
        "hi" -> "कोड 4-8 अंकों का होना चाहिए"
        else -> "Code must be 4–8 digits"
    }

    fun agreeTerms(): String = when (languageCode) {
        "ta" -> "சேவை விதிமுறைகளுக்கு நான் ஒப்புக்கொள்கிறேன்"
        else -> "I agree to Terms of Service"
    }

    fun agreePrivacy(): String = when (languageCode) {
        "ta" -> "தனியுரிமைக் கொள்கைக்கு நான் ஒப்புக்கொள்கிறேன்"
        else -> "I agree to Privacy Policy"
    }

    fun requiredForNewAccounts(): String = when (languageCode) {
        "ta" -> "புதிய கணக்குகளுக்கு தேவை"
        else -> "Required for new accounts"
    }

    fun acceptTerms(): String = when (languageCode) {
        "ta" -> "சேவை விதிமுறைகளை ஏற்கிறேன்"
        else -> "I accept the Terms of Service"
    }

    fun acceptPrivacy(): String = when (languageCode) {
        "ta" -> "தனியுரிமைக் கொள்கையை ஏற்கிறேன்"
        else -> "I accept the Privacy Policy"
    }

    fun parentalAttestation(): String = when (languageCode) {
        "ta" -> "நான் பிரதான பாதுகாவலர்/பெற்றோர் மற்றும் 18 வயதுக்கு மேற்பட்டவர்"
        "hi" -> "मैं अभिभावक/संरक्षक हूं और 18 वर्ष से अधिक का हूं"
        else -> "I am the parent or guardian and am at least 18 years old"
    }

    fun enterOtp(): String = when (languageCode) {
        "ta" -> "OTP உள்ளிடவும்"
        else -> "Enter OTP"
    }

    fun noChildrenYet(): String = when (languageCode) {
        "ta" -> "இன்னும் குழந்தைகள் இல்லை"
        else -> "No children yet"
    }

    fun addFirstChildPrompt(): String = when (languageCode) {
        "ta" -> "தொடங்குவதற்கு உங்கள் முதல் குழந்தையைச் சேர்க்கவும்."
        else -> "Add your first child to get started"
    }

    fun tapToUploadAudio(): String = when (languageCode) {
        "ta" -> "ஆடியோ பதிவேற்ற கிளிக் செய்யுங்கள்"
        else -> "Tap to upload audio"
    }

    fun useVoiceForStories(): String = when (languageCode) {
        "ta" -> "தனிப்பட்ட கதைகளுக்கு உங்கள் குரலைப் பயன்படுத்துங்கள்"
        else -> "Use your voice for personalized stories"
    }

    fun voiceProfileCreated(): String = when (languageCode) {
        "ta" -> "குரல் சுயவிவரம் உருவாக்கப்பட்டது!"
        else -> "Voice profile created!"
    }

    fun noVoiceProfiles(): String = when (languageCode) {
        "ta" -> "இன்னும் குரல் சுயவிவரங்கள் இல்லை"
        else -> "No voice profiles yet"
    }

    fun uploadVoiceToGetStarted(): String = when (languageCode) {
        "ta" -> "தொடங்க குரல் பதிவேற்றம் செய்யுங்கள்"
        else -> "Upload your voice to get started"
    }

    fun minCharacters(): String = when (languageCode) {
        "ta" -> "குறைந்தபட்சம் 8 எழுத்துக்கள்"
        else -> "Min 8 characters"
    }

    fun createAccountSafe(): String = when (languageCode) {
        "ta" -> "கணக்கு உருவாக்குங்கள் • குடும்பங்களுக்கு பாதுகாப்பானது"
        else -> "Create an account • Safe for families"
    }

    fun storiesSparkImagination(): String = when (languageCode) {
        "ta" -> "கற்பனையைத் தூண்டும் கதைகள் • குழந்தைகளுக்கு பாதுகாப்பானது"
        else -> "Stories that spark imagination • Safe for kids"
    }

    fun sleepTimer(): String = when (languageCode) {
        "ta" -> "தூக்க நேர அமைப்பி"
        else -> "Sleep timer"
    }

    fun sleepTimerHint(): String = when (languageCode) {
        "ta" -> "எத்தனை நிமிடங்களுக்குப் பிறகு நிறுத்தவும்"
        else -> "Stop playback after"
    }

    fun cancelTimer(): String = when (languageCode) {
        "ta" -> "நதூக்க நேர அமைப்பியை ரத்து செய்"
        else -> "Cancel timer"
    }

    fun premiumVoiceTitle(): String = when (languageCode) {
        "ta" -> "பிரீமியம் குரல்"
        "hi" -> "प्रीमियम आवाज़"
        else -> "Premium Voice"
    }

    fun recordYourVoiceNow(): String = when (languageCode) {
        "ta" -> "உங்கள் குரலை இப்போது பதிவு செய்யுங்கள்"
        "hi" -> "अभी अपनी आवाज़ रिकॉर्ड करें"
        else -> "Record your voice now"
    }

    /** In-player option: record voice for this story (then clone). */
    fun recordVoiceForThisStory(): String = when (languageCode) {
        "ta" -> "இந்த கதைக்கு குரல் பதிவு செய்"
        "hi" -> "इस कहानी के लिए आवाज़ रिकॉर्ड करें"
        else -> "Record voice for this story"
    }

    /** In-player option: upload an audio file as family voice for this story. */
    fun uploadAudioForThisStory(): String = when (languageCode) {
        "ta" -> "இந்த கதைக்கு ஆடியோ பதிவேற்று"
        "hi" -> "इस कहानी के लिए ऑडियो अपलोड करें"
        else -> "Upload audio for this story"
    }

    /** Shown on story screen when user has no cloned voice; opens voice upload to create a profile. */
    fun uploadVoiceToCreateMyVoice(): String = when (languageCode) {
        "ta" -> "குரல் பதிவேற்றி என் குரலை உருவாக்கு"
        "hi" -> "आवाज़ अपलोड करके अपनी आवाज़ बनाएं"
        else -> "Upload voice to create my voice"
    }

    /** CTA after voice clone success: add avatar if they have credits. */
    fun addAvatarAfterVoice(): String = when (languageCode) {
        "ta" -> "உங்கள் படத்தைச் சேர்த்து கதைகளை உயிர்ப்பிக்கவும்"
        "hi" -> "अपनी फोटो जोड़ें और कहानियों को जीवंत करें"
        else -> "Add your photo — bring stories to life"
    }

    fun tapToStartRecording(): String = when (languageCode) {
        "ta" -> "பதிவைத் தொடங்க கிளிக் செய்யுங்கள்"
        "hi" -> "रिकॉर्डिंग शुरू करने के लिए टैप करें"
        else -> "Tap to start recording"
    }

    fun tapToStopRecording(): String = when (languageCode) {
        "ta" -> "நிறுத்த கிளிக் செய்யுங்கள்"
        "hi" -> "रोकने के लिए टैप करें"
        else -> "Tap to stop recording"
    }

    fun uploading(): String = when (languageCode) {
        "ta" -> "பதிவேற்றுகிறது..."
        "hi" -> "अपलोड हो रहा है..."
        else -> "Uploading..."
    }

    fun useThisRecording(): String = when (languageCode) {
        "ta" -> "இந்த பதிவைப் பயன்படுத்து"
        "hi" -> "इस रिकॉर्डिंग का उपयोग करें"
        else -> "Use this recording"
    }

    fun useThisVoiceForStory(): String = when (languageCode) {
        "ta" -> "இந்த கதைக்கு இந்த குரலைப் பயன்படுத்தவா?"
        "hi" -> "क्या इस कहानी के लिए यह आवाज़ उपयोग करें?"
        else -> "Use this voice for this story?"
    }

    fun useThisVoice(): String = when (languageCode) {
        "ta" -> "இந்த குரலைப் பயன்படுத்து"
        "hi" -> "यह आवाज़ उपयोग करें"
        else -> "Use this voice"
    }

    fun uploadYourVoiceForStory(): String = when (languageCode) {
        "ta" -> "இந்தக் கதைக்கு உங்கள் குரலைப் பதிவேற்றுங்கள்"
        "hi" -> "इस कहानी के लिए अपनी आवाज़ अपलोड करें"
        else -> "Upload your voice for this story"
    }

    fun familyVoiceUploaded(): String = when (languageCode) {
        "ta" -> "உங்கள் குரல் சேமிக்கப்பட்டது!"
        "hi" -> "आपकी आवाज़ सेव हो गई!"
        else -> "Your voice has been saved!"
    }

    fun preferredNarrationVoice(): String = when (languageCode) {
        "ta" -> "விரும்பிய கதை குரல்"
        "hi" -> "पसंदीदा कहानी आवाज़"
        else -> "Preferred story voice"
    }

    fun useVoiceGloballyHint(): String = when (languageCode) {
        "ta" -> "அனைத்து கதைகளுக்கும் பயன்படுத்தப்படும்"
        "hi" -> "सभी कहानियों के लिए लागू होगा"
        else -> "Applied to all stories"
    }

    /**
     * User-facing label for voice options (story screen, settings).
     * Naming: "Default" = system; "My voice" = user's own (family or cloned); premium = name + ★.
     */
    fun voiceLabel(voiceProfile: String, isPremium: Boolean = false): String {
        val base = when (voiceProfile.lowercase()) {
            "default" -> when (languageCode) {
                "ta" -> "இயல்புநிலை"
                "hi" -> "डिफ़ॉल्ट"
                else -> "Default"
            }
            "family" -> myVoiceLabel()
            else -> when {
                voiceProfile.startsWith("cloned:") -> myVoiceLabel()
                else -> voiceProfile.replaceFirstChar { it.uppercase() }
            }
        }
        return base + if (isPremium) " ★" else ""
    }

    /** Single label for user's own voice (family or cloned); aligns with "My voice & Avatar" tab. */
    fun myVoiceLabel(): String = when (languageCode) {
        "ta" -> "என் குரல்"
        "hi" -> "मेरी आवाज़"
        else -> "My voice"
    }

    /** Story play screen: single menu option – default narrator voice. */
    fun playScreenDefaultVoice(): String = when (languageCode) {
        "ta" -> "இயல்பு குரல்"
        "hi" -> "डिफ़ॉल्ट आवाज़"
        else -> "Default Voice"
    }

    /** Story play screen: single menu option – play in my voice; if missing, navigates to record/upload. */
    fun playScreenMyVoice(): String = when (languageCode) {
        "ta" -> "என் குரல்"
        "hi" -> "मेरी आवाज़"
        else -> "My Voice"
    }

    /** Story play screen: single menu option – play with my avatar; if missing, navigates to voice + photo setup. */
    fun playScreenAvatar(): String = when (languageCode) {
        "ta" -> "அவதாரம்"
        "hi" -> "अवतार"
        else -> "Avatar"
    }

    /** My voice menu: add first cloned voice when none exist. */
    fun playScreenUploadRecordVoice(): String = when (languageCode) {
        "ta" -> "பதிவு / வாக்கைப் பதிவு செய்யுங்கள்"
        "hi" -> "रिकॉर्ड / अपनी आवाज़ अपलोड करें"
        else -> "Upload / Record voice"
    }

    /** My voice menu: add another cloned voice when under max (5). */
    fun playScreenUploadRecordAnotherVoice(): String = when (languageCode) {
        "ta" -> "மற்றொரு குரலைப் பதிவு செய்யுங்கள்"
        "hi" -> "दूसरी आवाज़ रिकॉर्ड / अपलोड करें"
        else -> "Upload / Record another voice"
    }

    /** Bottom sheet section header – list of cloned voices. */
    fun playScreenYourVoices(): String = when (languageCode) {
        "ta" -> "உங்கள் குரல்கள்"
        "hi" -> "आपकी आवाज़ें"
        else -> "Your voices"
    }

    /** Bottom sheet section header – list of avatar images. */
    fun playScreenYourAvatars(): String = when (languageCode) {
        "ta" -> "உங்கள் அவதாரங்கள்"
        "hi" -> "आपके अवतार"
        else -> "Your avatars"
    }

    /** Bottom sheet: upload new avatar when under max (5). */
    fun playScreenUploadAvatar(): String = when (languageCode) {
        "ta" -> "அவதாரம் பதிவேற்றம்"
        "hi" -> "अवतार अपलोड करें"
        else -> "Upload Avatar"
    }

    /** Audio is being prepared/generated; shown while stream URL is fetched or backend generates narration. */
    fun preparingAudio(): String = when (languageCode) {
        "ta" -> "ஒலி தயார் செய்யப்படுகிறது..."
        "hi" -> "ऑडियो तैयार हो रहा है..."
        else -> "Preparing audio..."
    }

    /** Audio failed to load; shown with Retry when stream URL or playback fails. */
    fun audioLoadFailed(): String = when (languageCode) {
        "ta" -> "ஒலி ஏற்றப்படவில்லை"
        "hi" -> "ऑडियो लोड नहीं हो पाया"
        else -> "Audio couldn't load"
    }

    /** Shown briefly when narrative scene advances on Learn stories (optional family pause). */
    fun playerReflectionCue(): String = when (languageCode) {
        "ta" -> "புதிய காட்சி — விரும்பினால் ஒரு நிமிடம் இடைநிறுத்தி பகிர்ந்து கொள்ளுங்கள்."
        "hi" -> "नया दृश्य — चाहें तो रोककर एक साथ बात करें।"
        else -> "New scene — pause if you’d like to chat together for a moment."
    }

    /** Avatar video is being generated; shown while backend prepares talking-head video. */
    fun playScreenAvatarGenerating(): String = when (languageCode) {
        "ta" -> "அவதாரம் (உருவாகுகிறது...)"
        "hi" -> "अवतार (बन रहा है...)"
        else -> "Avatar (generating…)"
    }

    /** Avatar video generation failed; fallback to voice only. */
    fun playScreenAvatarTrouble(): String = when (languageCode) {
        "ta" -> "அவதாரம் தடை"
        "hi" -> "अवतार समस्या"
        else -> "Avatar (having trouble)"
    }

    fun premiumVoiceMessage(): String = when (languageCode) {
        "ta" -> "பிரீமியம் குரலைப் பயன்படுத்த சந்தாவை மேம்படுத்தவும்."
        "hi" -> "प्रीमियम आवाज़ के लिए अपना सब्सक्रिप्शन अपग्रेड करें।"
        else -> "Upgrade your subscription to use premium voices."
    }

    fun avatarUpload(): String = when (languageCode) {
        "ta" -> "கதை சொல்லும் அவதாரம்"
        "hi" -> "कहानी सुनाने वाला अवतार"
        else -> "Storytelling Avatar"
    }

    /** Short tab label for bottom nav — one word to avoid overlap */
    fun tabStoryTellingAvatar(): String = when (languageCode) {
        "ta" -> "அவதாரம்"
        "hi" -> "अवतार"
        else -> "Avatar"
    }

    /** Short tab label for bottom nav */
    fun tabVoiceUpload(): String = when (languageCode) {
        "ta" -> "குரல்"
        "hi" -> "आवाज़"
        else -> "Voice"
    }

    /** Combined tab label: My voice & Avatar (replaces separate Voice / Avatar tabs). */
    fun tabMyVoiceAndAvatar(): String = when (languageCode) {
        "ta" -> "குரல் & அவதாரம்"
        "hi" -> "आवाज़ और अवतार"
        else -> "My voice & Avatar"
    }

    fun avatarScreenHeadline(): String = when (languageCode) {
        "ta" -> "உங்கள் முகம் கதைசொல்லியாக மாறும்"
        "hi" -> "आपका चेहरा कहानी सुनाएगा"
        else -> "Your face becomes the storyteller"
    }

    fun avatarScreenSubline(): String = when (languageCode) {
        "ta" -> "படம் பதிவேற்றுங்கள் — கதைகள் உங்கள் குரலில், உங்கள் தோற்றத்தில்"
        "hi" -> "फोटो अपलोड करें — कहानियाँ आपकी आवाज़ और चेहरे में"
        else -> "Upload your photo — stories come alive in your voice and your face"
    }

    fun avatarSectionStoriesWithAvatar(): String = when (languageCode) {
        "ta" -> "உங்கள் அவதாரத்துடன் கதைகள்"
        "hi" -> "आपके अवतार वाली कहानियाँ"
        else -> "Stories with your avatar"
    }

    fun avatarSectionNoStoriesYet(): String = when (languageCode) {
        "ta" -> "கதை உருவாக்கி, உங்கள் அவதாரத்தை சேர்த்து பார்க்கவும்"
        "hi" -> "कहानी बनाएं और अपना अवतार जोड़ें"
        else -> "Create a story and add your avatar to see it here"
    }

    fun avatarCtaAddPhoto(): String = when (languageCode) {
        "ta" -> "என் படத்தை சேர் — நான் கதை சொல்லுவேன்"
        "hi" -> "अपनी फोटो जोड़ें — मैं कहानी सुनाऊंगा"
        else -> "Add my photo — I'll tell the story"
    }

    /** Shown on Avatar card when user has no cloned voice yet. */
    fun avatarCreateVoiceFirst(): String = when (languageCode) {
        "ta" -> "முதலில் உங்கள் குரலை உருவாக்குங்கள்"
        "hi" -> "पहले अपनी आवाज़ बनाएं"
        else -> "Create your voice first"
    }

    fun voiceScreenHeadline(): String = when (languageCode) {
        "ta" -> "கதைகள் உங்கள் குரலில்"
        "hi" -> "कहानियाँ आपकी आवाज़ में"
        else -> "Stories in your voice"
    }

    fun voiceScreenSubline(): String = when (languageCode) {
        "ta" -> "குரலை குளோன் செய்யுங்கள் — குழந்தைகள் உங்கள் குரலில் கதைகள் கேட்பார்கள்"
        "hi" -> "अपनी आवाज़ क्लोन करें — बच्चे आपकी आवाज़ में कहानियाँ सुनेंगे"
        else -> "Clone your voice — kids hear stories in the voice they love"
    }

    fun voiceSectionStoriesWithYourVoice(): String = when (languageCode) {
        "ta" -> "உங்கள் குரலில் சொல்லப்பட்ட கதைகள்"
        "hi" -> "आपकी आवाज़ में कहानियाँ"
        else -> "Stories with your voice"
    }

    fun voiceSectionNoStoriesYet(): String = when (languageCode) {
        "ta" -> "குரலை சேர்க்கவும், பின்னர் கதைகள் இங்கே தோன்றும்"
        "hi" -> "आवाज़ जोड़ें — यहाँ आपकी आवाज़ वाली कहानियाँ दिखेंगी"
        else -> "Add your voice — stories you narrate will appear here"
    }

    fun voiceCtaCloneYourVoice(): String = when (languageCode) {
        "ta" -> "என் குரலை பதிவு செய் — கதைகள் என் குரலில்"
        "hi" -> "अपनी आवाज़ रिकॉर्ड करें — कहानियाँ मेरी आवाज़ में"
        else -> "Record my voice — stories in my voice"
    }

    /** Shown when cloned voice was requested but default audio is served (TTS temporarily unavailable). */
    fun voiceFallbackMessage(): String = when (languageCode) {
        "ta" -> "உங்கள் குரல் தற்காலிகமாக கிடைக்கவில்லை — இயல்புநிலை குரலில் இயக்கப்படுகிறது"
        "hi" -> "आपकी आवाज़ अस्थायी रूप से उपलब्ध नहीं — डिफ़ॉल्ट आवाज़ में चल रहा है"
        else -> "Your voice is temporarily unavailable — playing with default voice"
    }

    fun tapToUploadAvatar(): String = when (languageCode) {
        "ta" -> "படத்தைப் பதிவேற்ற பட்டனை அழுத்துங்கள்"
        "hi" -> "अपना अवतार अपलोड करने के लिए टैप करें"
        else -> "Tap to upload your photo"
    }

    fun avatarTellsStoriesHint(): String = when (languageCode) {
        "ta" -> "உங்கள் முகம் கதைகளைச் சொல்லும்"
        "hi" -> "आपकी तस्वीर कहानियाँ सुनाएगी"
        else -> "Your face will tell the stories"
    }

    fun avatarUploadSuccess(): String = when (languageCode) {
        "ta" -> "அவதாரம் சேமிக்கப்பட்டது!"
        "hi" -> "अवतार सेव हो गया!"
        else -> "Avatar saved!"
    }

    fun removeAvatar(): String = when (languageCode) {
        "ta" -> "அவதாரத்தை அகற்று"
        "hi" -> "अवतार हटाएं"
        else -> "Remove avatar"
    }

    fun yourAvatar(): String = when (languageCode) {
        "ta" -> "உங்கள் அவதாரம்"
        "hi" -> "आपका अवतार"
        else -> "Your avatar"
    }

    /** Label above avatar in story player: this story is narrated with your avatar. */
    fun storyWithYourAvatar(): String = when (languageCode) {
        "ta" -> "உங்கள் அவதாரத்துடன் கதை"
        "hi" -> "आपके अवतार के साथ कहानी"
        else -> "Story with your avatar"
    }

    /** Optional host / brand clip (muted) shown with story audio. */
    fun hostStoryClipSectionLabel(): String = when (languageCode) {
        "ta" -> "சிறப்பு காட்சி"
        "hi" -> "विशेष दृश्य"
        else -> "Story moment"
    }

    /** Avatar menu label on story screen. */
    fun avatarMenuLabel(): String = when (languageCode) {
        "ta" -> "அவதாரம்"
        "hi" -> "अवतार"
        else -> "Avatar"
    }

    /** Avatar menu: upload photo for storytelling. */
    fun avatarUploadPhoto(): String = when (languageCode) {
        "ta" -> "படம் பதிவேற்று"
        "hi" -> "फोटो अपलोड करें"
        else -> "Upload photo"
    }

    fun avatarPremiumRequired(): String = when (languageCode) {
        "ta" -> "கதை சொல்லும் அவதாரத்திற்கு பிரீமியம் தேவை"
        "hi" -> "कहानी अवतार के लिए प्रीमियम चाहिए"
        else -> "Premium required for storytelling avatar"
    }

    /** Avatar card when not entitled: voice + image = Super premium. */
    fun avatarUploadVoiceAndImageSuperPremium(): String = when (languageCode) {
        "ta" -> "குரல் மற்றும் படம் பதிவேற்றுங்கள் (சூப்பர் பிரீமியம்)"
        "hi" -> "आवाज़ और फोटो अपलोड करें (सुपर प्रीमियम)"
        else -> "Upload voice and photo (Super premium)"
    }

    fun avatarTellsStoriesDescription(): String = when (languageCode) {
        "ta" -> "பிரீமியம் உறுப்பினர்கள் தங்கள் படத்தைப் பதிவேற்றலாம். அந்த முகம் கதைகளைச் சொல்லும் வீடியோவாக மாறும்."
        "hi" -> "प्रीमियम सदस्य अपनी फोटो अपलोड कर सकते हैं। वह चेहरा कहानियाँ सुनाने वाला वीडियो बन जाएगा।"
        else -> "Premium members can upload their photo. That face will become the storyteller in videos."
    }

    fun upgrade(): String = when (languageCode) {
        "ta" -> "மேம்படுத்து"
        "hi" -> "अपग्रेड करें"
        else -> "Upgrade"
    }

    fun referralCodePlaceholder(): String = when (languageCode) {
        "ta" -> "பரிந்துரை குறியீடு (எ.கா AMAZ5)"
        "hi" -> "रेफरल कोड (जैसे AMAZ5)"
        else -> "Referral code (e.g. AMAZ5)"
    }

    /** Short hint inside the referral code field (codes are typically Latin). */
    fun referralCodeShortHint(): String = "AMAZ5"

    fun applyCode(): String = when (languageCode) {
        "ta" -> "பயன்படுத்து"
        "hi" -> "लागू करें"
        else -> "Apply"
    }

    fun referralDiscount(percent: Int, shopName: String): String = when (languageCode) {
        "ta" -> "$percent% தள்ளுபடி — $shopName"
        "hi" -> "$percent% छूट — $shopName"
        else -> "$percent% off — $shopName"
    }

    fun invalidReferralCode(): String = when (languageCode) {
        "ta" -> "குறியீடு செல்லுபடியாகவில்லை அல்லது காலாவதியானது"
        "hi" -> "कोड अमान्य या समाप्त हो गया"
        else -> "Invalid or expired code"
    }

    fun minutesShort(min: Int): String = when (languageCode) {
        "ta" -> "$min நிமி"
        "hi" -> "$min मिनट"
        else -> "$min min"
    }

    fun getOtp(): String = when (languageCode) {
        "ta" -> "OTP பெறு"
        "hi" -> "OTP प्राप्त करें"
        else -> "Get OTP"
    }

    fun verifyOtp(): String = when (languageCode) {
        "ta" -> "OTP சரிபார்க்க"
        "hi" -> "OTP सत्यापित करें"
        else -> "Verify OTP"
    }

    fun enterOtpCode(): String = when (languageCode) {
        "ta" -> "OTP குறியீட்டை உள்ளிடவும்"
        "hi" -> "OTP कोड दर्ज करें"
        else -> "Enter the OTP Code"
    }

    fun otpSentTo(): String = when (languageCode) {
        "ta" -> "6 இலக்க குறியீட்டை நாங்கள் அனுப்பியுள்ளோம். அதை உள்ளிடவும்."
        "hi" -> "हमने भेजा 6 अंकों का कोड। इसे दर्ज करें।"
        else -> "Please enter the 6-digit code we sent."
    }

    fun resendOtpIn(seconds: Int): String = when (languageCode) {
        "ta" -> "${seconds}s இல் OTP மீண்டும் அனுப்பு"
        "hi" -> "${seconds}s में OTP फिर से भेजें"
        else -> "Resend OTP in ${seconds}s"
    }

    fun haventReceivedCode(): String = when (languageCode) {
        "ta" -> "குறியீடு வரவில்லையா?"
        "hi" -> "कोड नहीं मिला?"
        else -> "Haven't received the code?"
    }

    /**
     * OTP send returned success=false (invalid number, SMS provider failure, or server could not deliver).
     * Prefer this over implying the phone format is always wrong.
     */
    fun otpSendFailed(): String = when (languageCode) {
        "ta" -> "உள்நுழைவு குறியீட்டை அனுப்ப முடியவில்லை. மொபைல் எண்ணைச் சரிபார்த்து மீண்டும் முயலவும்."
        "hi" -> "लॉगिन कोड नहीं भेजा जा सका। अपना मोबाइल नंबर जाँचें और फिर कोशिश करें।"
        else -> "We couldn't send your login code. Check your mobile number and try again."
    }

    fun termsAndPrivacyDisclaimer(): String = when (languageCode) {
        "ta" -> "உள்ளிடுவதன் மூலம், எங்கள் விதிமுறைகள் மற்றும் தனியுரிமைக் கொள்கைக்கு நீங்கள் சம்மதிக்கிறீர்கள்."
        "hi" -> "दर्ज करके, आप हमारी शर्तों और गोपनीयता नीति से सहमत हैं।"
        else -> "By entering, you agree to our Terms and Privacy Policy."
    }

    fun welcomeToTamixa(): String = when (languageCode) {
        "ta" -> "Tamixa-க்கு வரவேற்கிறோம்"
        "hi" -> "Tamixa में आपका स्वागत है"
        else -> "Welcome to Tamixa"
    }

    fun loginWelcomeTagline(): String = when (languageCode) {
        "ta" -> "உங்கள் வழியில் கேளுங்கள்—உங்களுக்கான தனிப்பட்ட அனுபவம்."
        "hi" -> "अपने तरीके से सुनें—आपके लिए व्यक्तिगत।"
        else -> "Listen your way—personalized for you."
    }

    fun recommended(): String = when (languageCode) {
        "ta" -> "பரிந்துரைக்கப்பட்டது"
        "hi" -> "अनुशंसित"
        else -> "Recommended"
    }

    fun recommendedForYou(): String = when (languageCode) {
        "ta" -> "உங்களுக்கு பரிந்துரைக்கப்பட்டது"
        "hi" -> "आपके लिए अनुशंसित"
        else -> "Recommended for you"
    }

    fun becauseYouListenedTo(title: String): String = when (languageCode) {
        "ta" -> "\"$title\" கேட்டதால்"
        "hi" -> "\"$title\" सुनने के कारण"
        else -> "Because you listened to \"$title\""
    }

    fun allAges(): String = when (languageCode) {
        "ta" -> "அனைத்து வயது"
        "hi" -> "सभी उम्र"
        else -> "All ages"
    }

    fun ageRange(min: Int, max: Int): String = when (languageCode) {
        "ta" -> "$min-$max வயது"
        "hi" -> "$min-$max साल"
        else -> "Ages $min-$max"
    }

    fun freePlanLimit(used: Int, limit: Int): String = when (languageCode) {
        "ta" -> "இலவச திட்டம்: $used / $limit கதைகள்"
        "hi" -> "फ्री प्लान: $used / $limit कहानियाँ"
        else -> "Free plan: $used / $limit stories"
    }

    fun storyLimitReachedUpgradeMessage(): String = when (languageCode) {
        "ta" -> "இந்த மாதம் உங்கள் இலவச கதைகள் முடிந்துவிட்டன. வரம்பற்ற கதைகள் மற்றும் உங்கள் குரலுக்கு மேம்படுத்துங்கள்."
        "hi" -> "इस महीने आपकी मुफ्त कहानियाँ खत्म हो गईं। अनलिमिटेड कहानियाँ और अपनी आवाज़ के लिए अपग्रेड करें।"
        else -> "You've used your free stories this month. Upgrade for unlimited stories and your voice."
    }

    fun trialDaysLeft(days: Int): String = when (languageCode) {
        "ta" -> "$days நாட்கள் சோதனை மீதம்"
        "hi" -> "$days दिन ट्रायल बचा"
        else -> "$days days trial left"
    }

    fun editStory(): String = when (languageCode) {
        "ta" -> "கதையைத் திருத்து"
        "hi" -> "कहानी संपादित करें"
        else -> "Edit story"
    }

    fun remixInstruction(): String = when (languageCode) {
        "ta" -> "மாற்றத்தை விவரிக்கவும் (எ.கா. டிராகனை நட்பாக மாற்று)"
        "hi" -> "बदलाव बताएं (जैसे: ड्रैगन को दोस्ताना बनाएं)"
        else -> "Describe the change (e.g. make the dragon friendly)"
    }

    fun featured(): String = when (languageCode) {
        "ta" -> "சிறப்பு கதைகள்"
        "hi" -> "विशेष कहानियाँ"
        else -> "Featured"
    }

    fun allStories(): String = when (languageCode) {
        "ta" -> "அனைத்து கதைகள்"
        "hi" -> "सभी कहानियाँ"
        else -> "All Stories"
    }

    fun upNext(): String = when (languageCode) {
        "ta" -> "பரிந்துரைக்கப்பட்ட கதைகள்"
        "hi" -> "अनुशंसित कहानियाँ"
        else -> "Recommended Stories"
    }

    fun seeAll(): String = when (languageCode) {
        "ta" -> "அனைத்தையும் காண்க"
        "hi" -> "सभी देखें"
        else -> "See all"
    }

    fun moralOfStory(): String = when (languageCode) {
        "ta" -> "கதையின் நீதி"
        "hi" -> "कहानी का सबक"
        else -> "Moral of the Story"
    }

    fun readAlong(): String = when (languageCode) {
        "ta" -> "படித்துப் பயிற்சி"
        "hi" -> "पढ़कर अभ्यास करें"
        else -> "Read along"
    }

    fun readAlongUnavailable(): String = when (languageCode) {
        "ta" -> "கதை உரை இல்லை"
        "hi" -> "कहानी का पाठ उपलब्ध नहीं"
        else -> "Story text not available"
    }

    /** Label for the audio transcript (spoken text) in the player. */
    fun transcript(): String = when (languageCode) {
        "ta" -> "பேசும் உரை"
        "hi" -> "बोला गया पाठ"
        else -> "Transcript"
    }

    fun transcriptUnavailable(): String = when (languageCode) {
        "ta" -> "உரை இல்லை"
        "hi" -> "पाठ उपलब्ध नहीं"
        else -> "Transcript not available"
    }

    fun confirmLanguage(): String = when (languageCode) {
        "ta" -> "மொழியை உறுதிப்படுத்து"
        "hi" -> "भाषा की पुष्टि करें"
        else -> "Confirm Language"
    }

    fun storiesInPreferredLanguage(): String = when (languageCode) {
        "ta" -> "உங்கள் விரும்பிய மொழியில் கதைகள் கேளுங்கள்"
        "hi" -> "अपनी पसंदीदा भाषा में कहानियाँ सुनें"
        else -> "Listen to stories in your preferred language."
    }

    /** First-run language screen — explains scope (library, narration, learn). */
    fun languageSelectionSubtitle(): String = when (languageCode) {
        "ta" -> "நூலகம், விளக்கம் மற்றும் செயல்பாடுகள் இந்த மொழியில். அமைப்புகளில் எப்போதும் மாற்றலாம்."
        "hi" -> "लाइब्रेरी, बोलकर सुनाना और गतिविधियाँ इसी भाषा में। सेटिंग्स में कभी भी बदलें।"
        "te" -> "లైబ్రరీ, కథనం (నేరేషన్) మరియు కార్యకలాపాలు ఈ భాషలో ఉంటాయి. సెట్టింగ్‌లలో ఎప్పుడైనా మార్చుకోవచ్చు."
        "kn" -> "ಲೈಬ್ರರಿ, ನಿರೂಪಣೆ ಮತ್ತು ಚಟುವಟಿಕೆಗಳು ಈ ಭಾಷೆಯಲ್ಲಿರುತ್ತವೆ. ಸೆಟ್ಟಿಂಗ್‌ಗಳಲ್ಲಿ ಯಾವಾಗ ಬೇಕಾದರೂ ಬದಲಿಸಬಹುದು."
        "ml" -> "ലൈബ്രറി, വിവരണവും പ്രവർത്തനങ്ങളും ഈ ഭാഷയിലായിരിക്കും. ക്രമീകരണങ്ങളിൽ എപ്പോൾ വേണമെങ്കിലും മാറ്റാം."
        else -> "Library, narration, and activities follow this language. Change anytime in Settings."
    }

    fun generateNewStory(): String = when (languageCode) {
        "ta" -> "புதிய கதை உருவாக்கு"
        "hi" -> "नई कहानी बनाएं"
        else -> "Generate a new story"
    }

    fun childFirstName(): String = when (languageCode) {
        "ta" -> "குழந்தையின் முதல் பெயர்"
        "hi" -> "बच्चे का पहला नाम"
        else -> "Child's first Name"
    }

    fun selectStoryTheme(): String = when (languageCode) {
        "ta" -> "கதை தீம் தேர்ந்தெடு"
        "hi" -> "कहानी का विषय चुनें"
        else -> "Select Story Theme"
    }

    fun createStory(): String = when (languageCode) {
        "ta" -> "கதை உருவாக்கு"
        "hi" -> "कहानी बनाएं"
        else -> "Create Story"
    }

    fun chooseThemeAndGenerate(): String = when (languageCode) {
        "ta" -> "தீம் தேர்ந்தெடுத்து கதையை உருவாக்குங்கள்"
        "hi" -> "थीम चुनें और कहानी बनाएं"
        else -> "Choose a theme and create your story"
    }

    fun createStoryForChild(name: String): String = when (languageCode) {
        "ta" -> "$name க்கான கதை"
        "hi" -> "$name के लिए कहानी"
        else -> "Story for $name"
    }

    fun addChildToCreateStories(): String = when (languageCode) {
        "ta" -> "கதை உருவாக்க குழந்தையைச் சேர்க்கவும் (செட்‌டிங்ஸ்)"
        "hi" -> "कहानी बनाने के लिए सेटिंग्स में बच्चा जोड़ें"
        else -> "Add a child in Settings to create stories"
    }

    /** Default character name when no child profile is selected. */
    fun listener(): String = when (languageCode) {
        "ta" -> "கேட்பவர்"
        "hi" -> "श्रोता"
        else -> "Listener"
    }

    fun listenerNameOptional(): String = when (languageCode) {
        "ta" -> "கதை கேட்பவர் பெயர் (விரும்பினால்)"
        "hi" -> "कहानी सुनने वाले का नाम (वैकल्पिक)"
        else -> "Listener name (optional)"
    }

    fun ageForStory(): String = when (languageCode) {
        "ta" -> "வயது (1–99)"
        "hi" -> "उम्र (1–99)"
        else -> "Age (1–99)"
    }

    fun bedtimeStory(): String = when (languageCode) {
        "ta" -> "இரவு கதை (அமைதியான)"
        "hi" -> "रात की कहानी (शांत)"
        else -> "Bedtime story (calm)"
    }

    fun learningFocusOptional(): String = when (languageCode) {
        "ta" -> "கற்றல் நோக்கம் (விரும்பினால்)"
        "hi" -> "सीखने पर ध्यान (वैकल्पिक)"
        else -> "Learning focus (optional)"
    }

    fun learningFocusNone(): String = when (languageCode) {
        "ta" -> "இல்லை"
        "hi" -> "कोई नहीं"
        else -> "None"
    }

    fun learningFocusPublicSpeaking(): String = when (languageCode) {
        "ta" -> "பேச்சு திறன்"
        "hi" -> "बोलने का आत्मविश्वास"
        else -> "Public speaking"
    }

    fun learningFocusMoneyLiteracy(): String = when (languageCode) {
        "ta" -> "பண அறிவு"
        "hi" -> "पैसे की समझ"
        else -> "Money smarts"
    }

    fun learningFocusResearchSkills(): String = when (languageCode) {
        "ta" -> "ஆராய்ச்சி & உண்மைகள்"
        "hi" -> "जाँच और सच्चाई"
        else -> "Research & facts"
    }

    fun learningFocusEmpathy(): String = when (languageCode) {
        "ta" -> "புரிதல்"
        "hi" -> "सहानुभूति"
        else -> "Empathy"
    }

    fun learningFocusProblemSolving(): String = when (languageCode) {
        "ta" -> "சிக்கல் தீர்ப்பு"
        "hi" -> "समस्या सुलझाना"
        else -> "Problem solving"
    }

    fun learningFocusVocabulary(): String = when (languageCode) {
        "ta" -> "சொற்கள்"
        "hi" -> "शब्दावली"
        else -> "Vocabulary"
    }

    fun learningFocusCuriosity(): String = when (languageCode) {
        "ta" -> "ஆர்வம்"
        "hi" -> "जिज्ञासा"
        else -> "Curiosity"
    }

    fun libraryBrowseTab(): String = when (languageCode) {
        "ta" -> "நூலகம்"
        "hi" -> "लाइब्रेरी"
        else -> "Browse"
    }

    /** Library second lane: lighter classics (category Fun stories / Funny Stories). */
    fun libraryFunCornerTab(): String = when (languageCode) {
        "ta" -> "சிரிப்பு மூலை"
        "hi" -> "मज़ेदार कोना"
        else -> "Fun corner"
    }

    /** Library third lane: Learn-prefixed and Digital Safety stories. */
    fun libraryLearnSafetyTab(): String = when (languageCode) {
        "ta" -> "கற்றல் & பாதுகாப்பு"
        "hi" -> "सीखें और सुरक्षा"
        else -> "Learn & safety"
    }

    /** Library fourth lane: Learn · Simulator and interactive graph stories. */
    fun librarySimulatorTab(): String = when (languageCode) {
        "ta" -> "பயிற்சி"
        "hi" -> "अभ्यास"
        else -> "Practice"
    }

    fun spotlightEmptyHint(): String = when (languageCode) {
        "ta" -> "நூலகக் கதைகள் தயாரானதும் இங்கே தோன்றும் — இழுத்து புதுப்பிக்கவும்."
        "hi" -> "लाइब्रेरी की कहानियाँ तैयार होते ही यहाँ दिखेंगी—खींचकर रीफ़्रेश करें।"
        else -> "Approved library tales will show up here—pull to refresh."
    }

    fun funCornerEmptyHint(): String = when (languageCode) {
        "ta" -> "இங்கே 'Fun stories' அல்லது 'Funny Stories' வகையில் கதைகள் தோன்றும் — பழைய, இலகுவான கதைகளை நிர்வாகத்தில் அந்த வகைக்கு நகர்த்தலாம்."
        "hi" -> "यहाँ 'Fun stories' या 'Funny Stories' श्रेणी वाली कहानियाँ दिखेंगी—हल्की पुरानी कहानियों को एडमिन में उस श्रेणी में ले जाएँ।"
        else -> "Stories tagged Fun stories or Funny Stories show up here—move lighter classics to those categories in admin."
    }

    fun learnSafetyEmptyHint(): String = when (languageCode) {
        "ta" -> "இங்கே 'Learn · …' அல்லது 'Digital Safety' தலைப்புடன் கூடிய நூலகக் கதைகள் தோன்றும்."
        "hi" -> "यहाँ 'Learn · …' या 'Digital Safety' वाली लाइब्रेरी कहानियाँ दिखेंगी।"
        else -> "Educational and digital-safety library stories appear here (theme/category starts with Learn or mentions Digital Safety)."
    }

    fun simulatorHubEmptyHint(): String = when (languageCode) {
        "ta" -> "இங்கே 'Learn · Simulator' தலைப்பு அல்லது தேர்வுகளுடன் கூடிய ஊடாடும் நூலகக் கதைகள் தோன்றும்."
        "hi" -> "यहाँ 'Learn · Simulator' शीर्षक या चुनाव वाली इंटरैक्टिव लाइब्रेरी कहानियाँ दिखेंगी।"
        else -> "Stories labeled Learn · Simulator or with interactive choices appear here—assign metadata in admin if missing."
    }

    /** Small chip on poster tiles in the fun lane. */
    fun storyFunCornerBadge(): String = when (languageCode) {
        "ta" -> "சிரிப்பு"
        "hi" -> "मज़ा"
        else -> "Just for fun"
    }

    /** Library poster: branching Edu / simulator tales (Learn · Simulator or interactive graph). */
    fun libraryInteractivePosterBadge(): String = when (languageCode) {
        "ta" -> "நீங்கள் தேர்வு"
        "hi" -> "आप चुनें"
        else -> "You choose"
    }

    /** Home dashboard: featured library listens (every tale is built for growth). */
    fun dashboardSpotlightTitle(): String = when (languageCode) {
        "ta" -> "கேட்டு வளருங்கள்"
        "hi" -> "सुनकर बढ़ें"
        else -> "Grow with every listen"
    }

    fun dashboardSpotlightSubtitle(): String = when (languageCode) {
        "ta" -> "ஒவ்வொரு கதையிலும் மொழி, உணர்வு, ஆர்வம் — எதையும் தட்டி கேட்கத் தொடங்குங்கள்."
        "hi" -> "हर कहानी में भाषा, भावना, जिज्ञासा—किसी भी कवर को टैप कर शुरू करें।"
        else -> "Heart, vocabulary, and curiosity are woven into every Tamixa tale—tap any cover to start."
    }

    /** Home: explains Library hubs (Browse, Fun, Learn · Simulator). */
    fun dashboardLibraryMapTitle(): String = when (languageCode) {
        "ta" -> "நூலக வரைபடம்"
        "hi" -> "लाइब्रेरी मैप"
        else -> "Your Library map"
    }

    fun dashboardLibraryMapSubtitle(): String = when (languageCode) {
        "ta" -> "இன்று தேவையானது எது? — சிரிப்பு, கற்றல், அல்லது தேர்வுகளுடன் பயிற்சி."
        "hi" -> "आज क्या चाहिए?—मज़ा, सीख या चुनाव वाला अभ्यास।"
        else -> "Pick the lane for today—fun listens, life skills, or practice with choices."
    }

    fun dashboardLibraryBrowseBlurb(): String = when (languageCode) {
        "ta" -> "முழு பட்டியல்; நூலகத்தில் வடிகட்டிகளைத் தட்டவும்."
        "hi" -> "पूरी सूची—लाइब्रेरी में फ़िल्टर टैप करें।"
        else -> "Full catalog—use filters inside Library."
    }

    fun dashboardLibraryFunLaneTitle(): String = when (languageCode) {
        "ta" -> "சிரிப்பு மூலை"
        "hi" -> "मज़ेदार कोना"
        else -> "Fun corner"
    }

    fun dashboardLibraryFunLaneBlurb(): String = when (languageCode) {
        "ta" -> "இலகுவான, சிரிப்புத் தரும் கதைகள்."
        "hi" -> "हल्की, मज़ेदार कहानियाँ।"
        else -> "Lighthearted listens—stories just for fun."
    }

    fun dashboardLibraryLearnLaneBlurb(): String = when (languageCode) {
        "ta" -> "உயிர்க்கல்வி, மின்னணு பாதுகாப்பு, முழு கற்றல் வரிசை."
        "hi" -> "जीवन कौशल, डिजिटल सुरक्षा, पूरी लर्न सूची।"
        else -> "Life skills, digital safety, and the full Learn catalog."
    }

    fun dashboardLibraryPracticeLaneTitle(): String = when (languageCode) {
        "ta" -> "பயிற்சி (Learn · Simulator)"
        "hi" -> "अभ्यास (Learn · Simulator)"
        else -> "Practice (Learn · Simulator)"
    }

    fun dashboardLibraryPracticeLaneBlurb(): String = when (languageCode) {
        "ta" -> "தேர்வுகளுடன் ஊடாடும் நூலகக் கதைகள் — தனி மையம்."
        "hi" -> "चुनाव वाली इंटरैक्टिव लाइब्रेरी कहानियाँ—अलग हब।"
        else -> "Interactive library stories with choices—in their own hub."
    }

    /** Library story with interactive graph: parent-facing player cue. */
    fun playerInteractivePracticeChip(): String = when (languageCode) {
        "ta" -> "பயிற்சிக் கதை · தேர்வுகள்"
        "hi" -> "अभ्यास कहानी · विकल्प"
        else -> "Practice story · choices"
    }

    fun openLibraryForMore(): String = when (languageCode) {
        "ta" -> "நூலகத்தில் மேலும்"
        "hi" -> "लाइब्रेरी में और"
        else -> "More in Library"
    }

    fun openFunCorner(): String = when (languageCode) {
        "ta" -> "சிரிப்பு மூலை"
        "hi" -> "मज़ेदार कोना"
        else -> "Open fun corner"
    }

    fun openLearnSafety(): String = when (languageCode) {
        "ta" -> "கற்றல் & பாதுகாப்பு"
        "hi" -> "सीखें और सुरक्षा"
        else -> "Learn & safety"
    }

    /** Dashboard spotlight: library Practice hub (Learn · Simulator). */
    fun openPracticeHub(): String = when (languageCode) {
        "ta" -> "பயிற்சி (சிமுலேட்டர்)"
        "hi" -> "अभ्यास (सिम्युलेटर)"
        else -> "Practice (simulator)"
    }

    fun interactiveStoryChoose(): String = when (languageCode) {
        "ta" -> "அடுத்து என்ன செய்வது?"
        "hi" -> "आगे क्या करें?"
        else -> "What happens next?"
    }

    fun familyMissionTitle(): String = when (languageCode) {
        "ta" -> "இன்றைய குடும்பப் பணி"
        "hi" -> "आज का परिवार मिशन"
        else -> "Today’s family mission"
    }

    /** Mission tone: optional challenge, not homework or a grade. */
    fun missionCardChallengeHint(): String = when (languageCode) {
        "ta" -> "ஒரு லேசான சவால்—பாடப்பணி அல்ல."
        "hi" -> "एक हल्की चुनौती है—यह होमवर्क नहीं है।"
        else -> "A light real-world challenge—not homework."
    }

    /** Guarded life-skill summary (interactive Edu stories only). */
    fun lifeSkillPracticeTitle(): String = when (languageCode) {
        "ta" -> "கதை தேர்வுகள் — பயிற்சி குறிப்புகள்"
        "hi" -> "कहानी विकल्प — अभ्यास संकेत"
        else -> "Story choices — practice signals"
    }

    fun lifeSkillPracticeDisclaimer(): String = when (languageCode) {
        "ta" -> "இவை மதிபெண்கள் அல்ல; குழந்தையின் கதை தேர்வுகளிலிருந்து தனிப்பட்ட சுருக்கமே."
        "hi" -> "ये अंक नहीं हैं—केवल कहानी विकल्पों से निजी संकेत।"
        else -> "Not scores or grades—private hints from story choices only."
    }

    fun lifeSkillPracticeChooseChild(): String = when (languageCode) {
        "ta" -> "குழந்தையைத் தேர்ந்தெடு"
        "hi" -> "बच्चा चुनें"
        else -> "Choose child"
    }

    /** When only stories-linked child id is known (no name from profile yet). */
    fun lifeSkillPracticeUnnamedChild(): String = when (languageCode) {
        "ta" -> "குழந்தை சுயவிவரம்"
        "hi" -> "बच्चे की प्रोफ़ाइल"
        else -> "Child profile"
    }

    fun lifeSkillPillarWisdom(): String = when (languageCode) {
        "ta" -> "ஞானம் / டிஜிட்டல் நுண்ணறிவு"
        "hi" -> "बुद्धिमत्ता / डिजिटल समझ"
        else -> "Wisdom / digital judgment"
    }

    fun lifeSkillPillarSocial(): String = when (languageCode) {
        "ta" -> "சமூகம்"
        "hi" -> "सामाजिक"
        else -> "Social"
    }

    fun lifeSkillPillarMoney(): String = when (languageCode) {
        "ta" -> "பணம் / நிதி பழக்கம்"
        "hi" -> "पैसा / वित्तीय आदतें"
        else -> "Money habits"
    }

    fun lifeSkillPillarBalance(): String = when (languageCode) {
        "ta" -> "சமநிலை / கவனம்"
        "hi" -> "संतुलन / ध्यान"
        else -> "Balance / focus"
    }

    fun openLinkedResource(): String = when (languageCode) {
        "ta" -> "இணைப்பைத் திற"
        "hi" -> "लिंक खोलें"
        else -> "Open link"
    }

    fun missionCardDone(): String = when (languageCode) {
        "ta" -> "முடிந்தது"
        "hi" -> "हो गया"
        else -> "Done"
    }

    fun curatedTopicsLabel(): String = when (languageCode) {
        "ta" -> "தேர்ந்தெடுக்கப்பட்ட தலைப்புகள்"
        "hi" -> "चुने हुए विषय"
        else -> "Curated topics"
    }

    fun curatedTopicCustom(): String = when (languageCode) {
        "ta" -> "தனிப்பயன் தீம்"
        "hi" -> "अपनी थीम"
        else -> "Custom theme"
    }

    fun themeOverrideHint(): String = when (languageCode) {
        "ta" -> "தலைப்புக்கு கூடுதல் விவரம் (விரும்பினால்)"
        "hi" -> "विषय पर अतिरिक्त विवरण (वैकल्पिक)"
        else -> "Extra detail for the topic (optional)"
    }

    fun forParentsSectionTitle(): String = when (languageCode) {
        "ta" -> "பெற்றோருக்கு"
        "hi" -> "अभिभावकों के लिए"
        else -> "For parents"
    }

    fun parentContentNoteLabel(): String = when (languageCode) {
        "ta" -> "உள்ளடக்கக் குறிப்பு"
        "hi" -> "सामग्री नोट"
        else -> "Content note"
    }

    fun speakAlongLabel(): String = when (languageCode) {
        "ta" -> "ஒன்றாகப் பேசுங்கள்"
        "hi" -> "साथ में बोलें"
        else -> "Speak-along"
    }

    fun discussionPromptsLabel(): String = when (languageCode) {
        "ta" -> "உரையாடல் கேள்விகள்"
        "hi" -> "चर्चा के प्रश्न"
        else -> "Discussion prompts"
    }

    fun storyFromOurFamilyHint(): String = when (languageCode) {
        "ta" -> "உங்கள் குடும்பத்தை கதையில் சேர்க்கவும் (விரும்பினால்)"
        "hi" -> "कहानी में अपना विवरण जोड़ें (वैकल्पिक)"
        else -> "Add your family to the story (optional)"
    }

    fun storyFromOurFamilyPlaceholder(): String = when (languageCode) {
        "ta" -> "எ.கா. ஒரு நாய் மேக்ஸ், கோயம்புத்தூரில் அமைக்கவும்"
        "hi" -> "जैसे: एक कुत्ता मैक्स, कोयंबटूर में सेट करें"
        else -> "e.g. Include a dog named Max, or set it in Coimbatore"
    }

    fun exploreStories(): String = when (languageCode) {
        "ta" -> "கதைகளை ஆராயுங்கள்"
        "hi" -> "कहानियाँ देखें"
        else -> "Explore Stories"
    }

    fun trending(): String = when (languageCode) {
        "ta" -> "பிரபலம்"
        "hi" -> "ट्रेंडिंग"
        else -> "Trending"
    }

    fun popular(): String = when (languageCode) {
        "ta" -> "பிரபலமானவை"
        "hi" -> "लोकप्रिय"
        else -> "Popular"
    }

    fun continueListening(): String = when (languageCode) {
        "ta" -> "கேட்டது தொடர்"
        "hi" -> "सुनना जारी रखें"
        else -> "Continue Listening"
    }

    fun playerCategoryDuration(category: String, minutes: Int): String = when (languageCode) {
        "ta" -> "வகை: $category | நேரம்: $minutes நிமி"
        "hi" -> "श्रेणी: $category | अवधि: $minutes मिनट"
        else -> "Category: $category | Duration: $minutes min"
    }

    fun playerAddToList(): String = when (languageCode) {
        "ta" -> "பட்டியலில் சேர்"
        "hi" -> "सूची में जोड़ें"
        else -> "Add to list"
    }

    /** Audio player quick action when story is already in favorites / list. */
    fun playerInList(): String = when (languageCode) {
        "ta" -> "பட்டியலில் உள்ளது"
        "hi" -> "सूची में है"
        else -> "In your list"
    }

    fun playerAddedToListSnackbar(): String = when (languageCode) {
        "ta" -> "பட்டியலில் சேர்க்கப்பட்டது"
        "hi" -> "सूची में जोड़ दिया गया"
        else -> "Saved to your list"
    }

    fun playerAlreadyInListSnackbar(): String = when (languageCode) {
        "ta" -> "இது ஏற்கனவே உங்கள் பட்டியலில் உள்ளது"
        "hi" -> "यह पहले से आपकी सूची में है"
        else -> "Already in your list"
    }

    fun playerLike(): String = when (languageCode) {
        "ta" -> "விருப்பம்"
        "hi" -> "पसंद"
        else -> "Like"
    }

    /** Short trust label on player hero art (generated / story illustration). */
    fun illustrationGeneratedArt(): String = when (languageCode) {
        "ta" -> "சித்திரம்"
        "hi" -> "चित्रण"
        else -> "Illustration"
    }

    fun storyCoverContentDescription(storyTitle: String?, theme: String): String {
        val label = storyTitle?.takeIf { it.isNotBlank() } ?: theme
        return when (languageCode) {
            "ta" -> "கதை அட்டை: $label"
            "hi" -> "कहानी कवर: $label"
            else -> "Story cover: $label"
        }
    }

    /** Settings: title for optional future story-art personalization consent. */
    fun storyArtPersonalizationSettingTitle(): String = when (languageCode) {
        "ta" -> "கதை கலை தனிப்பயனாக்கம்"
        "hi" -> "कहानी कला वैयक्तिकरण"
        else -> "Story art personalization"
    }

    fun storyArtPersonalizationSettingSummary(): String = when (languageCode) {
        "ta" -> "எதிர்காலத்தில், உங்கள் ஒப்புதலுடன் கதைப் படங்கள் மேலும் தனிப்பயனாக அமையலாம். புகைப்படங்கள் சேமிப்பு கொள்கைக்கு உட்பட்டவை."
        "hi" -> "भविष्य में, आपकी सहमति से कहानी की तस्वीरें और अनुकूलित हो सकती हैं। फ़ोटो हमारी गोपनीयता नीति के अधीन हैं।"
        else -> "With your consent, we may personalize story visuals further in the future. Photos follow our privacy policy; you can turn this off anytime."
    }

    fun storyArtPersonalizationPlayerNote(): String = when (languageCode) {
        "ta" -> "தனிப்பயன் கதைக் கலை முன்னுரிமைகள் இயக்கத்தில் உள்ளன."
        "hi" -> "वैयक्तिकृत कला वरीयताएँ चालू हैं।"
        else -> "Personalized story-art preferences are on (see Settings)."
    }

    fun playerStartReading(): String = when (languageCode) {
        "ta" -> "வாசிப்பைத் தொடங்கு"
        "hi" -> "पढ़ना शुरू करें"
        else -> "Start reading"
    }

    fun playerShowLess(): String = when (languageCode) {
        "ta" -> "குறைவாகக் காட்டு"
        "hi" -> "कम दिखाएँ"
        else -> "Show less"
    }

    /** Story player: synced read-along (streamed narration). */
    fun playerListenAlongTitle(): String = when (languageCode) {
        "ta" -> "கேட்டுக்கொண்டே வாசி"
        "hi" -> "सुनते हुए पढ़ें"
        else -> "Listen & read along"
    }

    /** Shown when playback uses on-device read-aloud (no server word timings). */
    fun playerDeviceReadAloudHint(): String = when (languageCode) {
        "ta" -> "சாதன வாசிப்பு — உரை ஒலியுடன் ஒத்திசைக்கப்படாது; முழு கதையை கீழே படிக்கலாம்."
        "hi" -> "डिवाइस रीड-अलाउड — टेक्स्ट आवाज़ से मेल नहीं खा सकता; नीचे पूरी कहानी पढ़ें।"
        else -> "Device read-aloud — text won’t track the voice; scroll to read the full story below."
    }

    fun playerVoiceAndPlayMode(): String = when (languageCode) {
        "ta" -> "குரல் மற்றும் பயன்முறை"
        "hi" -> "आवाज़ और मोड"
        else -> "Voice & play mode"
    }

    /** Overflow ⋮ on audio player hero (opens voice & related actions). */
    fun playerMoreActions(): String = when (languageCode) {
        "ta" -> "மேலும் செயல்கள்"
        "hi" -> "और विकल्प"
        else -> "More actions"
    }

    fun playerSkipSeconds(sec: Int): String = when (languageCode) {
        "ta" -> "${sec}s"
        "hi" -> "${sec}s"
        else -> "${sec}s"
    }

    /** Shown under Continue Listening on the home dashboard. */
    fun latestListeningSubtitle(): String = when (languageCode) {
        "ta" -> "சமீபத்திய கேட்ட கதைகள் — புதியவை முதலில்"
        "hi" -> "आपकी हाल की सुनी कहानियाँ — नई पहले"
        else -> "Your latest listens — newest first"
    }

    /** Home dashboard listening snapshot (matches web stat window). */
    fun dashboardListeningWindow(days: Int): String = when (languageCode) {
        "ta" -> "கடைசி $days நாட்கள்"
        "hi" -> "पिछले $days दिन"
        else -> "Last $days days"
    }

    /** Overflow menu on home header (search stays visible; other actions grouped). */
    fun dashboardMenu(): String = when (languageCode) {
        "ta" -> "மேலும்"
        "hi" -> "और विकल्प"
        else -> "Menu"
    }

    /** Section title for generated / saved tales (friendlier than “all stories”). */
    fun dashboardYourStoriesTitle(): String = when (languageCode) {
        "ta" -> "உங்கள் கதைகள்"
        "hi" -> "आपकी कहानियाँ"
        else -> "Your stories"
    }

    /** One line under “Your stories” — tap any card to play. */
    fun dashboardYourStoriesSubtitle(): String = when (languageCode) {
        "ta" -> "உருவாக்கிய மற்றும் சேமித்த கதைகள் — ஒரு அட்டையைத் தட்டி கேளுங்கள்."
        "hi" -> "बनाई और सहेजी कहानियाँ — चलाने के लिए किसी कार्ड पर टैप करें।"
        else -> "Stories you’ve created and saved — tap a card to listen."
    }

    fun playerShare(): String = when (languageCode) {
        "ta" -> "பகிர்"
        "hi" -> "शेयर करें"
        else -> "Share"
    }

    fun noStoriesYet(): String = when (languageCode) {
        "ta" -> "இன்னும் கதைகள் இல்லை"
        "hi" -> "अभी तक कोई कहानी नहीं"
        else -> "No stories yet"
    }

    fun storyOfTheDay(): String = when (languageCode) {
        "ta" -> "இன்றைய கதை"
        "hi" -> "आज की कहानी"
        else -> "Story of the day"
    }

    fun listeningStreak(days: Int): String = when (languageCode) {
        "ta" -> "$days நாள் கேட்டல் தொடர்!"
        "hi" -> "$days दिन की सुनने की लकीर!"
        else -> "$days day listening streak!"
    }

    fun buildYourStreak(): String = when (languageCode) {
        "ta" -> "தினமும் கேட்டு கேட்டல் தொடரை உருவாக்குங்கள்"
        "hi" -> "रोज़ सुनकर सुनने की लकीर बनाएं"
        else -> "Listen daily to build your listening streak"
    }

    fun noFavoritesYet(): String = when (languageCode) {
        "ta" -> "விருப்பங்கள் இல்லை"
        "hi" -> "अभी कोई पसंदीदा नहीं"
        else -> "No favorites yet"
    }

    fun noFavoritesHint(): String = when (languageCode) {
        "ta" -> "கதைகளில் இதயத்தை தட்டுங்கள்"
        "hi" -> "कहानियों पर दिल दबाएं"
        else -> "Tap the heart on stories to add them here"
    }

    fun noListeningHistoryYet(): String = when (languageCode) {
        "ta" -> "இன்னும் கேட்கும் வரலாறு இல்லை"
        "hi" -> "अभी सुनने का इतिहास नहीं"
        else -> "No listening history yet"
    }

    fun noListeningHistoryHint(): String = when (languageCode) {
        "ta" -> "கதைகளை இயக்கவும், உங்கள் முன்னேற்றம் இங்கே காண்பிக்கப்படும்"
        "hi" -> "कहानियाँ बजाएं, आपकी प्रगति यहाँ दिखेगी"
        else -> "Play stories and your progress will show here"
    }

    fun continueFrom(min: Int, sec: Int): String = when (languageCode) {
        "ta" -> "தொடர் ${min}நிமி ${sec}வி"
        "hi" -> "जारी रखें ${min}मि ${sec}से"
        else -> "Continue from ${min}m ${sec}s"
    }

    fun noSearchResults(): String = when (languageCode) {
        "ta" -> "முடிவுகள் இல்லை"
        "hi" -> "कोई परिणाम नहीं"
        else -> "No results found"
    }

    fun noSearchResultsHint(): String = when (languageCode) {
        "ta" -> "வேறு சொற்களை முயற்சிக்கவும்"
        "hi" -> "दूसरे शब्द आज़माएं"
        else -> "Try different keywords"
    }

    /** Before the user has typed enough characters for search (see NavHost / repository). */
    fun searchMinCharactersHint(): String = when (languageCode) {
        "ta" -> "கதைகளைத் தேட இரண்டு எழுத்துகள் அல்லது அதற்கு மேல் தட்டச்சு செய்யவும்"
        "hi" -> "कहानियाँ खोजने के लिए कम से दो अक्षर टाइप करें"
        else -> "Type at least 2 characters to search stories"
    }

    /** Shown under [searchMinCharactersHint] on the search screen. */
    fun searchWhatYouCanFindHint(): String = when (languageCode) {
        "ta" -> "தலைப்பு, தீம் அல்லது விசைச்சொல்லாக தேடலாம்"
        "hi" -> "नाम, थीम या कीवर्ड से खोजें"
        else -> "You can search by title, theme, or keyword"
    }

    /** TalkBack when a full-screen error state is shown. */
    fun accessibilityErrorState(): String = when (languageCode) {
        "ta" -> "பிழை"
        "hi" -> "त्रुटि"
        else -> "Error"
    }

    fun upgradeToCreateMore(): String = when (languageCode) {
        "ta" -> "மேலும் கதைகள் உருவாக்க சந்தா மேம்படுத்துங்கள்"
        "hi" -> "और कहानियाँ बनाने के लिए अपग्रेड करें"
        else -> "Upgrade to create more stories"
    }

    fun storiesLeftThisMonth(remaining: Int): String = when (languageCode) {
        "ta" -> "இந்த மாதம் $remaining கதைகள் மீதம்"
        "hi" -> "इस महीने $remaining कहानियाँ बचीं"
        else -> "$remaining stories left this month"
    }

    fun tapNewStoryToCreate(): String = when (languageCode) {
        "ta" -> "முதல் சாகசத்தை உருவாக்க \"புதிய கதை\" தட்டுங்கள்"
        "hi" -> "अपना पहला रोमांच बनाने के लिए \"नई कहानी\" पर टैप करें"
        else -> "Tap \"New Story\" to create your first adventure!"
    }

    fun browseLibraryForStories(): String = when (languageCode) {
        "ta" -> "கதைகளைப் பார்க்க நூலகத்தைப் பார்வையிடுங்கள்"
        "hi" -> "कहानियाँ देखने के लिए लाइब्रेरी में जाएं"
        else -> "Browse the Library for stories"
    }

    fun noStoriesInCategory(): String = when (languageCode) {
        "ta" -> "இந்த வகையில் கதைகள் இல்லை"
        "hi" -> "इस श्रेणी में कोई कहानी नहीं"
        else -> "No stories in this category"
    }

    fun tryAnotherCategory(): String = when (languageCode) {
        "ta" -> "வேறு வகையைத் தேர்ந்தெடுக்கவும் அல்லது அனைத்தையும் பாருங்கள்"
        "hi" -> "दूसरी श्रेणी चुनें या सभी देखें"
        else -> "Try another category or select All"
    }

    fun sendStory(): String = when (languageCode) {
        "ta" -> "கதை அனுப்பு"
        "hi" -> "कहानी भेजें"
        else -> "Send Story"
    }

    fun sendStoryInTextFormat(): String = when (languageCode) {
        "ta" -> "உங்கள் கதையை உரை வடிவத்தில் அனுப்புங்கள்"
        "hi" -> "अपनी कहानी टेक्स्ट फॉर्मैट में भेजें"
        else -> "Send your story in text format"
    }

    fun sendStoryHint(): String = when (languageCode) {
        "ta" -> "உங்கள் கதையை தட்டச்சு செய்யுங்கள் அல்லது ஒட்டவும். நாங்கள் அதை ஆடியோ கதையாக மாற்றுவோம்."
        "hi" -> "अपनी कहानी टाइप करें या पेस्ट करें। हम इसे ऑडियो कहानी में बदल देंगे।"
        else -> "Type or paste your story below. We'll turn it into an audio story for your child."
    }

    fun yourStoryText(): String = when (languageCode) {
        "ta" -> "உங்கள் கதை"
        "hi" -> "आपकी कहानी"
        else -> "Your story"
    }

    fun storyTextPlaceholder(): String = when (languageCode) {
        "ta" -> "உங்கள் கதையை இங்கே தட்டச்சு செய்யவும் அல்லது ஒட்டவும்..."
        "hi" -> "अपनी कहानी यहाँ टाइप करें या पेस्ट करें..."
        else -> "Type or paste your story here..."
    }

    fun categories(): String = when (languageCode) {
        "ta" -> "வகைகள்"
        "hi" -> "श्रेणियाँ"
        else -> "Categories"
    }

    fun search(): String = when (languageCode) {
        "ta" -> "தேடு"
        "hi" -> "खोजें"
        else -> "Search"
    }

    fun searchStories(): String = when (languageCode) {
        "ta" -> "கதைகளைத் தேடு"
        "hi" -> "कहानियाँ खोजें"
        else -> "Search stories"
    }

    fun tryDifferentSearch(): String = when (languageCode) {
        "ta" -> "வேறு சொல்லைத் தேட முயற்சிக்கவும்"
        "hi" -> "कोई और शब्द खोजें"
        else -> "Try a different search"
    }

    fun tapToAddFavorites(): String = when (languageCode) {
        "ta" -> "கதைகளை இயக்கி இதயத்தில் தட்டுங்கள்"
        "hi" -> "कहानियाँ सुनकर दिल पर टैप करें"
        else -> "Play stories and tap the heart to add to favorites"
    }

    fun removeFromFavorites(): String = when (languageCode) {
        "ta" -> "பிடித்தவைகளிலிருந்து நீக்கு"
        "hi" -> "पसंदीदा से हटाएं"
        else -> "Remove from favorites"
    }

    fun favorites(): String = when (languageCode) {
        "ta" -> "பிடித்தவை"
        "hi" -> "पसंदीदा"
        else -> "Favorites"
    }

    fun home(): String = when (languageCode) {
        "ta" -> "முகப்பு"
        "hi" -> "होम"
        else -> "Home"
    }

    fun profile(): String = when (languageCode) {
        "ta" -> "சுயவிவரம்"
        "hi" -> "प्रोफ़ाइल"
        else -> "Profile"
    }

    fun nickname(): String = when (languageCode) {
        "ta" -> "புனைப்பெயர்"
        "hi" -> "उपनाम"
        else -> "Nickname"
    }

    fun nicknameHint(): String = when (languageCode) {
        "ta" -> "உங்கள் பெயர் அல்லது புனைப்பெயர் (மொபைல் எண்ணுக்கு பதிலாக காட்டப்படும்)"
        "hi" -> "आपका नाम या उपनाम (मोबाइल नंबर के बजाय दिखाया जाएगा)"
        else -> "Your name or nickname (shown instead of mobile number)"
    }

    fun personalDetails(): String = when (languageCode) {
        "ta" -> "தனிப்பட்ட விவரங்கள்"
        "hi" -> "व्यक्तिगत विवरण"
        else -> "Personal details"
    }

    fun edit(): String = when (languageCode) {
        "ta" -> "திருத்து"
        "hi" -> "संपादित करें"
        else -> "Edit"
    }

    fun notSet(): String = when (languageCode) {
        "ta" -> "அமைக்கப்படவில்லை"
        "hi" -> "सेट नहीं"
        else -> "Not set"
    }

    fun profileUpdated(): String = when (languageCode) {
        "ta" -> "சுயவிவரம் புதுப்பிக்கப்பட்டது"
        "hi" -> "प्रोफ़ाइल अपडेट हो गई"
        else -> "Profile updated"
    }

    fun save(): String = when (languageCode) {
        "ta" -> "சேமி"
        "hi" -> "सहेजें"
        else -> "Save"
    }

    fun library(): String = when (languageCode) {
        "ta" -> "நூலகம்"
        "hi" -> "लाइब्रेरी"
        else -> "Library"
    }

    fun storyLibrary(): String = when (languageCode) {
        "ta" -> "கதை நூலகம்"
        "hi" -> "कहानी लाइब्रेरी"
        else -> "Story Library"
    }

    fun myVoices(): String = when (languageCode) {
        "ta" -> "என் குரல்கள்"
        "hi" -> "मेरी आवाज़ें"
        else -> "My Voices"
    }

    fun myAvatars(): String = when (languageCode) {
        "ta" -> "என் அவதாரங்கள்"
        "hi" -> "मेरे अवतार"
        else -> "My Avatars"
    }

    fun listeningHistory(): String = when (languageCode) {
        "ta" -> "கேட்கும் வரலாறு"
        "hi" -> "सुनने का इतिहास"
        else -> "Listening History"
    }

    fun achievements(): String = when (languageCode) {
        "ta" -> "லட்சியங்கள்"
        "hi" -> "उपलब्धियाँ"
        else -> "Goals"
    }

    fun achievementsSubtitle(): String = when (languageCode) {
        "ta" -> "கதைகளைக் கேட்டு பதக்கங்களைப் பெறுங்கள்"
        "hi" -> "कहानियाँ सुनकर बैज कमाएँ"
        else -> "Listen to stories to earn badges"
    }

    fun premiumSubscription(): String = when (languageCode) {
        "ta" -> "பிரீமியம் சந்தா"
        "hi" -> "प्रीमियम सदस्यता"
        else -> "Premium Subscription"
    }

    fun create(): String = when (languageCode) {
        "ta" -> "உருவாக்கு"
        "hi" -> "बनाएं"
        else -> "Create"
    }

    fun onboardingVoiceHeadline(): String = when (languageCode) {
        "ta" -> "உங்கள் குடும்ப குரலில் கதைகள் விரும்புகிறீர்களா?"
        "hi" -> "क्या आप अपनी पारिवारिक आवाज़ में कहानियाँ चाहते हैं?"
        else -> "Want stories in your family voice?"
    }

    fun onboardingVoiceSubline(): String = when (languageCode) {
        "ta" -> "குறுகிய மாதிரி — பின்னர் கதைகள் உங்கள் தொனியில்; விருப்பமாக அவதார் வீடியோவும்."
        "hi" -> "छोटा सैंपल — फिर कहानियाँ आपकी आवाज़ में; चाहें तो अवतार वीडियो भी।"
        else -> "One short sample — then stories in your tone, with optional avatar video in the player."
    }

    fun onboardingVoicePrivacyNote(): String = when (languageCode) {
        "ta" -> "உங்கள் குடும்பத்திற்கு — நீங்கள் கட்டுப்படுத்தலாம்"
        "hi" -> "सिर्फ आपके परिवार के लिए — आप नियंत्रित करते हैं"
        else -> "For your family — you stay in control"
    }

    /** Onboarding step 3: primary CTA to continue to avatar/voice setup (not "Record voice"). */
    fun onboardingAddMyVoice(): String = when (languageCode) {
        "ta" -> "என் குரலைச் சேர்ப்பேன்"
        "hi" -> "मैं अपनी आवाज़ जोड़ूंगा"
        else -> "Add my voice"
    }

    fun onboardingAvatarHeadline(): String = when (languageCode) {
        "ta" -> "கதை யாரிடம் சொல்ல வேண்டும்?"
        "hi" -> "कहानी कौन सुनाए?"
        else -> "Who should tell the story?"
    }

    fun onboardingAvatarSubline(): String = when (languageCode) {
        "ta" -> "புகைப்படம் — 'என் குரல் + அவதார்' பயன்முறையில் சிறு வீடியோ கதைசொல்லி."
        "hi" -> "फोटो — 'मेरी आवाज़ + अवतार' मोड में छोटा वीडियो स्टोरीटेलर।"
        else -> "Add a photo — with My voice + avatar mode, kids see a friendly video storyteller."
    }

    /** Voice invitation card label (below hero image) — describes the value, not the CTA */
    fun onboardingVoiceCardLabel(): String = when (languageCode) {
        "ta" -> "உங்கள் குரலில் கதைகள்"
        "hi" -> "आपकी आवाज़ में कहानियाँ"
        else -> "Stories in your voice"
    }

    /** Avatar invitation card label (below hero image) — describes the value, not the CTA */
    fun onboardingAvatarCardLabel(): String = when (languageCode) {
        "ta" -> "உங்கள் கதைசொல்லி"
        "hi" -> "आपका कहानी सुनाने वाला"
        else -> "Your personal storyteller"
    }

    fun recordVoice(): String = when (languageCode) {
        "ta" -> "குரலை பதிவு செய்"
        "hi" -> "आवाज़ रिकॉर्ड करें"
        else -> "Record Voice"
    }

    fun uploadPhoto(): String = when (languageCode) {
        "ta" -> "புகைப்படம் பதிவேற்று"
        "hi" -> "फोटो अपलोड करें"
        else -> "Upload Photo"
    }

    fun onboardingDemoHeadline(): String = when (languageCode) {
        "ta" -> "உங்கள் முதல் கதையைத் தொடங்குங்கள்"
        "hi" -> "अपनी पहली कहानी शुरू करें"
        else -> "Let's start your first story"
    }

    fun onboardingDemoSubline(): String = when (languageCode) {
        "ta" -> "இயக்கு — முழு பிளேயரில் ஒலிக்கும் வரிகள் ஒளிரும்; இங்கே சிறு டெமோ மட்டும்."
        "hi" -> "प्ले दबाएँ — असली प्लेयर में पंक्तियाँ आवाज़ के साथ चमकती हैं; यहाँ छोटा डेमो है।"
        else -> "Tap play — the full player highlights lines with the voice; this is just a tiny preview."
    }

    fun onboardingDemoListenAlongHint(): String = when (languageCode) {
        "ta" -> "முழு பிளேயரில்: ஒலி + உரை ஒருங்கே"
        "hi" -> "पूरे प्लेयर में: आवाज़ + टेक्स्ट साथ"
        else -> "Full player: voice + text stay in sync"
    }

    /** One-line teaser inside the onboarding demo story card (above play). */
    fun onboardingDemoCardTeaser(): String = when (languageCode) {
        "ta" -> "ஒரு அமைதியான கிராமத்தில், ஒரு ஆர்வமுள்ள நரி வாழ்ந்தது…"
        "hi" -> "एक शांत गाँव में एक जिज्ञासु लोमड़ी रहती थी…"
        else -> "In a quiet village, there lived a curious fox…"
    }

    fun onboardingDemoBenefitHear(): String = when (languageCode) {
        "ta" -> "ஸ்ட்ரீம் கதைகளில் உரை ஒலியுடன் ஒத்திசைகிறது"
        "hi" -> "स्ट्रीम कहानियों में टेक्स्ट आवाज़ के साथ चलता है"
        else -> "Streamed stories keep on-screen text aligned with narration"
    }

    fun onboardingDemoBenefitControl(): String = when (languageCode) {
        "ta" -> "இடைநிறுத்தம், மீண்டும் — உங்கள் நேரத்தில்"
        "hi" -> "रोकें, फिर से चलाएँ — आपकी गति पर"
        else -> "Pause & replay — you set the rhythm"
    }

    fun onboardingDemoBenefitYoung(): String = when (languageCode) {
        "ta" -> "சிறுவர்களுக்கு ஏற்ற மென்மையான கதைசொல்லல்"
        "hi" -> "छोटे बच्चों के लिए कोमल, साफ़ कहानी"
        else -> "Gentle pacing made for little listeners"
    }

    fun onboardingDemoBenefitInteractive(): String = when (languageCode) {
        "ta" -> "கற்றல் நூலகத்தில் — தேர்வுகள், பின்னர் குடும்ப பணிகள்"
        "hi" -> "लर्न लाइब्रेरी में — विकल्प, फिर परिवार के छोटे मिशन"
        else -> "In Learn — choices, then optional family missions together"
    }

    fun onboardingDemoPillFun(): String = when (languageCode) {
        "ta" -> "வேடிக்கை கதைகள்"
        "hi" -> "मज़ेदार कहानियाँ"
        else -> "Fun stories"
    }

    fun onboardingDemoPillLearn(): String = when (languageCode) {
        "ta" -> "கற்றல் · பாதுகாப்பு"
        "hi" -> "सीखें · सुरक्षा"
        else -> "Learn · Safety"
    }

    fun onboardingVoiceBenefitKeepsake(): String = when (languageCode) {
        "ta" -> "பாட்டி / அப்பாவின் தொனி ஒவ்வொரு அத்தியாயத்திலும்"
        "hi" -> "दादी या पापा की आवाज़ हर अध्याय में"
        else -> "Keeps a grandparent or parent’s tone in every chapter"
    }

    fun onboardingVoiceBenefitQuick(): String = when (languageCode) {
        "ta" -> "குறுகிய மாதிரி — நாங்கள் தொழில்நுட்பத்தை கவனிக்கிறோம்"
        "hi" -> "छोटा सैंपल — तकनीक हम संभालते हैं"
        else -> "One short sample — we handle the techy bits"
    }

    fun onboardingVoiceBenefitRoutine(): String = when (languageCode) {
        "ta" -> "படுக்கை நேர வழக்கத்துடன் நன்கு பொருந்தும்"
        "hi" -> "सोने से पहले की दिनचर्या से मेल खाता है"
        else -> "Slides into cozy bedtime routines"
    }

    fun onboardingAvatarBenefitFace(): String = when (languageCode) {
        "ta" -> "அறிமுக முகம் கதைசொல்லிக்கு — குழந்தைகள் மகிழ்வர்"
        "hi" -> "जाना-पहचाना चेहरा कहानी सुनाने वाले के रूप में"
        else -> "A familiar face as the storyteller — kids light up"
    }

    fun onboardingAvatarBenefitTrust(): String = when (languageCode) {
        "ta" -> "குழந்தைகளுக்கு வேடிக்கை — பெற்றோருக்கு நம்பிக்கை"
        "hi" -> "बच्चों के लिए मज़ा — माता-पिता के लिए भरोसा"
        else -> "Playful for kids, reassuring for you"
    }

    fun onboardingAvatarBenefitOptional(): String = when (languageCode) {
        "ta" -> "விருப்பமே — தவிர்த்து கேட்டுக்கொள்ளலாம்"
        "hi" -> "पूरी तरह वैकल्पिक — कभी भी छोड़ सकते हैं"
        else -> "Totally optional — skip and keep listening anytime"
    }

    /** Short section title above value bullets (step 1–4 onboarding). */
    fun onboardingStripTitle(step: Int): String = when (step) {
        1 -> when (languageCode) {
            "ta" -> "உங்கள் குடும்பத்திற்கு ஏற்றது"
            "hi" -> "आपके परिवार के लिए बना"
            else -> "Built for your family"
        }
        2 -> when (languageCode) {
            "ta" -> "இந்த சிறு டெமோவில்"
            "hi" -> "इस छोटे डेमो में"
            else -> "In this mini demo"
        }
        3 -> when (languageCode) {
            "ta" -> "குடும்பக் குரல்"
            "hi" -> "परिवार की आवाज़"
            else -> "Layer in family voice"
        }
        4 -> when (languageCode) {
            "ta" -> "விருப்பமான முகம்"
            "hi" -> "वैकल्पिक चेहरा"
            else -> "Optional storyteller face"
        }
        else -> when (languageCode) {
            "ta" -> "சிறப்பம்சங்கள்"
            "hi" -> "खास बातें"
            else -> "Highlights"
        }
    }

    fun preparingDemo(): String = when (languageCode) {
        "ta" -> "உதாரணம் தயாரிக்கிறது..."
        "hi" -> "डेमो तैयार हो रहा है..."
        else -> "Preparing demo..."
    }

    fun parent(): String = when (languageCode) {
        "ta" -> "பெற்றோர்"
        "hi" -> "अभिभावक"
        else -> "Parent"
    }

    fun comingSoon(): String = when (languageCode) {
        "ta" -> "விரைவில்"
        "hi" -> "जल्द आ रहा है"
        else -> "Coming soon"
    }

    fun success(): String = when (languageCode) {
        "ta" -> "வெற்றி!"
        "hi" -> "सफलता!"
        else -> "Success!"
    }

    fun notPremiumMember(): String = when (languageCode) {
        "ta" -> "நீங்கள் பிரீமியம் உறுப்பினர் அல்ல!"
        "hi" -> "आप प्रीमियम सदस्य नहीं हैं!"
        else -> "You're not a premium member!"
    }

    fun proceed(): String = when (languageCode) {
        "ta" -> "தொடர்"
        "hi" -> "आगे बढ़ें"
        else -> "Proceed"
    }

    fun option(): String = when (languageCode) {
        "ta" -> "விருப்பம்"
        "hi" -> "विकल्प"
        else -> "Option"
    }

    fun oneTime(): String = when (languageCode) {
        "ta" -> "ஒரே முறை"
        "hi" -> "एक बार"
        else -> "One-time"
    }

    fun unlimitedBedtimeStories(): String = when (languageCode) {
        "ta" -> "வரம்பற்ற வரதட்சணை கதைகள்"
        "hi" -> "असीमित बिस्तर की कहानियाँ"
        else -> "Unlimited Bedtime Stories"
    }

    fun createStoriesPromotedBy(): String = when (languageCode) {
        "ta" -> "தனித்துவமான கதைகளை உருவாக்கி டிரினர்பே மூலம் ஊக்குவிக்கப்படுங்கள்"
        "hi" -> "अद्वितीय कहानियाँ बनाएं और त्रिनरपे द्वारा प्रचारित हो जाएं"
        else -> "Create unique stories and get promoted by Trinerpay"
    }

    // Error messages (used by AppMessageNotifier)
    fun sessionExpired(): String = when (languageCode) {
        "ta" -> "அமர்வு முடிந்துவிட்டது. மீண்டும் உள்நுழையுங்கள்."
        "hi" -> "सत्र समाप्त। कृपया फिर लॉगिन करें।"
        else -> "Session expired. Please log in again."
    }

    fun rateLimitExceeded(): String = when (languageCode) {
        "ta" -> "பல கோரிக்கைகள். சிறிது நேரம் கழித்து முயற்சிக்கவும்."
        "hi" -> "बहुत सारे अनुरोध। कृपया कुछ देर बाद कोशिश करें।"
        else -> "Too many requests. Please try again in a moment."
    }

    fun noPermission(): String = when (languageCode) {
        "ta" -> "உங்களுக்கு அனுமதி இல்லை."
        "hi" -> "आपको अनुमति नहीं है।"
        else -> "You don't have permission for this."
    }

    fun notFound(): String = when (languageCode) {
        "ta" -> "கண்டுபிடிக்கப்படவில்லை."
        "hi" -> "नहीं मिला।"
        else -> "Not found."
    }

    fun serverError(): String = when (languageCode) {
        "ta" -> "சர்வர் பிழை. பிறகு முயற்சிக்கவும்."
        "hi" -> "सर्वर त्रुटि। बाद में कोशिश करें।"
        else -> "Server error. Please try again later."
    }

    fun requestTimedOut(): String = when (languageCode) {
        "ta" -> "கோரிக்கை நேரம் முடிந்தது. மீண்டும் முயற்சிக்கவும்."
        "hi" -> "अनुरोध का समय समाप्त हो गया। कृपया पुनः प्रयास करें।"
        else -> "Request timed out. Please try again."
    }

    fun noInternet(): String = when (languageCode) {
        "ta" -> "இணைய இணைப்பு இல்லை. உங்கள் வலையமைப்பை சரிபார்க்கவும்."
        "hi" -> "इंटरनेट कनेक्शन नहीं। अपना नेटवर्क जांचें।"
        else -> "No internet connection. Check your network."
    }

    /** Same intent as [noInternet]; used by shared API error mapping. */
    fun noInternetConnection(): String = when (languageCode) {
        "ta" -> "இணைய இணைப்பு இல்லை. உங்கள் இணைப்பை சரிபார்க்கவும்."
        "hi" -> "इंटरनेट कनेक्शन नहीं है। कृपया अपना कनेक्शन जांचें।"
        else -> "No internet connection. Please check your connection."
    }

    fun connectionProblem(): String = when (languageCode) {
        "ta" -> "இணைப்பு சிக்கல். உங்கள் வலையமைப்பை சரிபார்க்கவும்."
        "hi" -> "कनेक्शन समस्या। अपना नेटवर्क जांचें।"
        else -> "Connection problem. Please check your network."
    }

    /** Shown when API/server is unreachable (e.g. connection refused, server down). */
    fun serverUnavailable(): String = when (languageCode) {
        "ta" -> "சர்வர் கிடைக்கவில்லை. பிறகு முயற்சிக்கவும்."
        "hi" -> "सर्वर उपलब्ध नहीं है। बाद में कोशिश करें।"
        else -> "Server is unavailable. Please try again later."
    }

    fun somethingWentWrong(): String = when (languageCode) {
        "ta" -> "ஏதோ தவறியது. மீண்டும் முயற்சிக்கவும்."
        "hi" -> "कुछ गलत हो गया। फिर कोशिश करें।"
        else -> "Something went wrong. Please try again."
    }

    /** Fallback when moderation rejects content and the API returns no message body. */
    fun storyContentNotAllowed(): String = when (languageCode) {
        "ta" -> "இந்த உள்ளடக்கத்தை உருவாக்க முடியவில்லை. வேறு தீம் அல்லது வார்த்தைகளை முயற்சிக்கவும்."
        "hi" -> "यह सामग्री नहीं बनाई जा सकी। कोई दूसरा विषय या शब्द आज़माएँ।"
        else -> "We couldn’t create that content. Try a different theme or wording."
    }

    /** When the optional structured-output guardrails sidecar is unavailable (HTTP 503). */
    fun storyValidationTemporarilyUnavailable(): String = when (languageCode) {
        "ta" -> "சரிபார்ப்பு சேவை தற்காலிகமாக கிடைக்கவில்லை. சிறிது நேரம் கழித்து முயற்சிக்கவும்."
        "hi" -> "सत्यापन सेवा अस्थायी रूप से उपलब्ध नहीं है। कृपया थोड़ी देर बाद पुनः प्रयास करें।"
        else -> "We couldn’t verify that story right now. Please try again in a moment."
    }

    /** POST /stories/generate — UNKNOWN_GENERATION_TOPIC. */
    fun storyGenerateUnknownTopic(): String = when (languageCode) {
        "ta" -> "அந்த தலைப்பு தற்போது கிடைக்கவில்லை. வேறு தலைப்பைத் தேர்ந்தெடுக்கவும் அல்லது ஒரு தீமை உள்ளிடவும்."
        "hi" -> "वह विषय अभी उपलब्ध नहीं है। दूसरा विषय चुनें या अपना विषय लिखें।"
        else -> "That curated topic isn’t available. Pick another topic or enter a theme."
    }

    /** POST /stories/generate — THEME_OR_TOPIC_REQUIRED. */
    fun storyGenerateThemeOrTopicRequired(): String = when (languageCode) {
        "ta" -> "ஒரு தலைப்பைத் தேர்ந்தெடுக்கவும் அல்லது தீமை உள்ளிடவும்."
        "hi" -> "कोई विषय चुनें या विषय दर्ज करें।"
        else -> "Choose a curated topic or enter a theme."
    }

    /** POST /stories/generate — GENERATION_LANGUAGE_NOT_SUPPORTED. */
    fun storyGenerateTamilOnly(): String = when (languageCode) {
        "ta" -> "தற்போது கதை உருவாக்கம் தமிழில் மட்டுமே."
        "hi" -> "अभी कहानी जनरेशन केवल तमिल में उपलब्ध है।"
        else -> "Story generation is available in Tamil only for now."
    }

    fun noMoralAvailable(): String = when (languageCode) {
        "ta" -> "நீதி கிடைக்கவில்லை."
        "hi" -> "सबक उपलब्ध नहीं।"
        else -> "No moral available."
    }

    fun moralDialogIntro(): String = when (languageCode) {
        "ta" -> "இந்த கதையின் நீதி:"
        "hi" -> "इस कहानी का सबक:"
        else -> "Here’s what this story teaches:"
    }

    fun talkAboutIt(): String = when (languageCode) {
        "ta" -> "இதைப் பற்றி பேசுங்கள்"
        "hi" -> "इस बारे में बात करें"
        else -> "Talk about it"
    }

    fun discussionPromptWhatLearned(): String = when (languageCode) {
        "ta" -> "இந்த கதையிலிருந்து நீங்கள் என்ன கற்றுக்கொண்டீர்கள்?"
        "hi" -> "इस कहानी से आपने क्या सीखा?"
        else -> "What did you learn from this story?"
    }

    fun discussionPromptFavoritePart(): String = when (languageCode) {
        "ta" -> "உங்களுக்கு பிடித்த பகுதி எது?"
        "hi" -> "आपको सबसे अच्छा कौन सा हिस्सा लगा?"
        else -> "What was your favorite part?"
    }

    fun voiceRecordingComingSoonIos(): String = when (languageCode) {
        "ta" -> "iOS-இல் குரல் பதிவு விரைவில் வரும். இதுவரை அட்ராய்டில் உங்கள் குரலைப் பதிவு செய்யுங்கள்."
        "hi" -> "iOS पर वॉयस रिकॉर्डिंग जल्द आ रही है। तब तक Android पर अपनी आवाज़ रिकॉर्ड करें।"
        else -> "Voice recording is coming soon on iOS. Use an Android device to record your voice until then."
    }

    fun selectChild(): String = when (languageCode) {
        "ta" -> "குழந்தையைத் தேர்ந்தெடு"
        "hi" -> "बच्चा चुनें"
        else -> "Select child"
    }

    fun selected(): String = when (languageCode) {
        "ta" -> "தேர்ந்தெடுக்கப்பட்டது"
        "hi" -> "चयनित"
        else -> "Selected"
    }

    fun select(): String = when (languageCode) {
        "ta" -> "தேர்ந்தெடு"
        "hi" -> "चुनें"
        else -> "Select"
    }

    fun included(): String = when (languageCode) {
        "ta" -> "சேர்க்கப்பட்டது"
        "hi" -> "शामिल"
        else -> "Included"
    }

    fun chooseYourPlan(): String = when (languageCode) {
        "ta" -> "உங்கள் திட்டத்தைத் தேர்ந்தெடு"
        "hi" -> "अपनी योजना चुनें"
        else -> "Choose your plan"
    }

    fun codeSentTo(email: String): String = when (languageCode) {
        "ta" -> "குறியீடு அனுப்பப்பட்டது: $email"
        "hi" -> "कोड भेजा गया: $email"
        else -> "Code sent to $email"
    }

    fun enterPhoneNumber(): String = when (languageCode) {
        "ta" -> "மொபைல் எண் உள்ளிடவும்"
        "hi" -> "फ़ोन नंबर दर्ज करें"
        else -> "Enter phone number"
    }

    fun yourVoiceProfiles(): String = when (languageCode) {
        "ta" -> "உங்கள் குரல் சுயவிவரங்கள்"
        "hi" -> "आपकी आवाज़ प्रोफ़ाइल"
        else -> "Your voice profiles"
    }

    fun noProfilesYet(): String = when (languageCode) {
        "ta" -> "இன்னும் சுயவிவரங்கள் இல்லை"
        "hi" -> "अभी तक कोई प्रोफ़ाइल नहीं"
        else -> "No profiles yet"
    }

    fun uploadAudioForFirstProfile(): String = when (languageCode) {
        "ta" -> "முதல் குரல் சுயவிவரத்தை உருவாக்க ஆடியோ பதிவேற்றம் செய்யுங்கள்"
        "hi" -> "पहली आवाज़ प्रोफ़ाइल बनाने के लिए ऑडियो अपलोड करें"
        else -> "Upload audio to create your first voice profile"
    }

    fun profileNumber(id: Long): String = when (languageCode) {
        "ta" -> "சுயவிவரம் #$id"
        "hi" -> "प्रोफ़ाइल #$id"
        else -> "Profile #$id"
    }

    fun generateFirstStoryPrompt(): String = when (languageCode) {
        "ta" -> "முதல் AI கதையை உருவாக்கி சாகசத்தைத் தொடங்குங்கள்!"
        "hi" -> "अपनी पहली AI कहानी बनाएं और रोमांच शुरू करें!"
        else -> "Generate your first AI story and start an adventure!"
    }

    fun child(): String = when (languageCode) {
        "ta" -> "குழந்தை"
        "hi" -> "बच्चा"
        else -> "Child"
    }

    // --- Onboarding ---

    fun onboardingHookHeadline(): String = when (languageCode) {
        "ta" -> "நீங்கள் விரும்பும் குரலில் கதைகள்"
        "hi" -> "जिन आवाजों से प्यार करते हैं, उनमें कहानियाँ"
        else -> "Stories told by the voices you love"
    }

    fun onboardingHookSubline(): String = when (languageCode) {
        "ta" -> "வேடிக்கை, கற்றல் மற்றும் பாதுகாப்பு கதைகள் — ஒரே இடத்தில். ஸ்ட்ரீம் கதைகளில் உரை ஒலியுடன் ஒளிரும்; விருப்பமாக உங்கள் குரல் + அவதார் வீடியோ."
        "hi" -> "मज़ेदार, सीखने और सुरक्षा वाली कहानियाँ — एक जगह। स्ट्रीम पर टेक्स्ट आवाज़ के साथ हाइलाइट; वैकल्पिक रूप से आपकी आवाज़ + अवतार वीडियो।"
        else -> "Fun, learning, and safety tales in one place. Streamed stories highlight text with the voice — add your voice and optional avatar video when you’re ready."
    }

    /** Hook screen: value bullets (store-style clarity, Tamixa-specific copy). */
    fun onboardingHookBenefitVoice(): String = when (languageCode) {
        "ta" -> "அறிமுக குரல்களில் — ரோபோ குரல் அல்ல"
        "hi" -> "जानी-पहचानी आवाज़ें — रोबोटिक नहीं"
        else -> "Warm, familiar voices — not robotic narrators"
    }

    fun onboardingHookBenefitPersonal(): String = when (languageCode) {
        "ta" -> "உங்கள் குழந்தைக்கே ஏற்ற கதைகள் மற்றும் தீம்கள்"
        "hi" -> "आपके बच्चे के हिसाब से कहानियाँ और थीम"
        else -> "Stories and themes shaped around your child"
    }

    fun onboardingHookBenefitCalm(): String = when (languageCode) {
        "ta" -> "அமைதியான நேரத்திற்கு — பாதுகாப்பான, மென்மையான"
        "hi" -> "शांत समय के लिए — सुरक्षित, कोमल अनुभव"
        else -> "Made for wind-down time — safe, gentle listening"
    }

    /** Hook: Learn library + optional interactive practice (parent-facing, no grades). */
    fun onboardingHookBenefitLearn(): String = when (languageCode) {
        "ta" -> "கற்றல் நூலகம் — தேர்வுகளுடன் பயிற்சி அறிகுறிகள் (மதிப்பெண்கள் அல்ல)"
        "hi" -> "लर्न लाइब्रेरी — चुनावों के साथ हल्के सिग्नल (नंबर नहीं)"
        else -> "Learn library — gentle practice signals from choices, not scores"
    }

    /** Short chip: Learn & safety lane (matches admin category naming intent). */
    fun onboardingHookPillLearnSafety(): String = when (languageCode) {
        "ta" -> "கற்றல் · பாதுகாப்பு"
        "hi" -> "सीखें · सुरक्षा"
        else -> "Learn · Safety"
    }

    /** Ribbon under onboarding progress — positions Tamixa as edu / learn-lane stories. */
    fun onboardingEduStoryBadgeLabel(): String = when (languageCode) {
        "ta" -> "கற்றல் கதைகள் · பாதுகாப்பான கேட்டல்"
        "hi" -> "सीखने वाली कहानियाँ · सुरक्षित सुनना"
        else -> "Learn stories · Safe listening"
    }

    fun onboardingProgressShort(step: Int, totalSteps: Int): String = when (languageCode) {
        "ta" -> "படி $step / $totalSteps"
        "hi" -> "चरण $step / $totalSteps"
        else -> "Step $step of $totalSteps"
    }

    fun startStoryMagic(): String = when (languageCode) {
        "ta" -> "கதை மந்திரத்தைத் தொடங்குங்கள்"
        "hi" -> "कहानी जादू शुरू करें"
        else -> "Start Story Magic"
    }

    fun skip(): String = when (languageCode) {
        "ta" -> "தவிர்க்க"
        "hi" -> "छोड़ें"
        else -> "Skip"
    }

    fun whatStoriesDoYouLike(): String = when (languageCode) {
        "ta" -> "எந்த வகை கதைகள் பிடிக்கும்?"
        "hi" -> "आपको किस तरह की कहानियाँ पसंद हैं?"
        else -> "What kind of stories do you like?"
    }

    fun select2to3Themes(): String = when (languageCode) {
        "ta" -> "2–3 தேர்வு செய்யுங்கள்"
        "hi" -> "2–3 चुनें"
        else -> "Select 2–3"
    }

    fun themeAnimals(): String = when (languageCode) {
        "ta" -> "விலங்குகள்"
        "hi" -> "जानवर"
        else -> "Animals"
    }

    fun themeAdventure(): String = when (languageCode) {
        "ta" -> "சாகசம்"
        "hi" -> "रोमांच"
        else -> "Adventure"
    }

    fun themeFriendship(): String = when (languageCode) {
        "ta" -> "நட்பு"
        "hi" -> "दोस्ती"
        else -> "Friendship"
    }

    fun themeVillageLife(): String = when (languageCode) {
        "ta" -> "கிராம வாழ்க்கை"
        "hi" -> "गाँव जीवन"
        else -> "Village Life"
    }

    fun themeFunny(): String = when (languageCode) {
        "ta" -> "சிரிப்பான கதைகள்"
        "hi" -> "मजेदार कहानियाँ"
        else -> "Funny Stories"
    }

    fun continueLabel(): String = when (languageCode) {
        "ta" -> "தொடர்"
        "hi" -> "जारी रखें"
        else -> "Continue"
    }

    fun remindMeAtBedtime(): String = when (languageCode) {
        "ta" -> "படுக்கை நேரத்தில் நினைவூட்டு"
        "hi" -> "सोते समय याद दिलाएं"
        else -> "Remind me at bedtime"
    }

    fun enableReminder(): String = when (languageCode) {
        "ta" -> "நினைவூட்டினை இயக்கு"
        "hi" -> "अलार्म चालू करें"
        else -> "Enable reminder"
    }

    fun saveProgressCloneVoiceUploadAvatar(): String = when (languageCode) {
        "ta" -> "முன்னேற்றத்தை சேமிக்கவும் • குரலை குளோன் செய்யவும் • அவதாரம் பதிவேற்றவும்"
        "hi" -> "प्रगति सहेजें • आवाज़ क्लोन करें • अवतार अपलोड करें"
        else -> "Save progress • Clone voice • Upload avatar"
    }

    fun yourHomeScreen(): String = when (languageCode) {
        "ta" -> "உங்கள் முகப்பு திரை"
        "hi" -> "आपका होम स्क्रीन"
        else -> "Your home screen"
    }

    // --- Short Content (Fun & Learn) ---

    fun funAndLearn(): String = when (languageCode) {
        "ta" -> "வேடிக்கை மற்றும் கற்றல்"
        "hi" -> "मज़ा और सीखना"
        else -> "Fun & Learn"
    }

    fun thoughtForTheDay(): String = when (languageCode) {
        "ta" -> "இன்றைய சிந்தனை"
        "hi" -> "आज का विचार"
        else -> "Thought for the Day"
    }

    fun riddle(): String = when (languageCode) {
        "ta" -> "விளையாட்டுப் புதிர்"
        "hi" -> "पहेली"
        else -> "Riddle"
    }

    fun joke(): String = when (languageCode) {
        "ta" -> "நகைச்சுவை"
        "hi" -> "चुटकुला"
        else -> "Joke"
    }

    fun tapToRevealAnswer(): String = when (languageCode) {
        "ta" -> "பதிலைக் காட்ட தட்டவும்"
        "hi" -> "जवाब देखने के लिए टैप करें"
        else -> "Tap to reveal answer"
    }

    fun answer(): String = when (languageCode) {
        "ta" -> "பதில்"
        "hi" -> "जवाब"
        else -> "Answer"
    }

    fun seeMore(): String = when (languageCode) {
        "ta" -> "மேலும் பார்"
        "hi" -> "और देखें"
        else -> "See more"
    }

    fun noShortContentYet(): String = when (languageCode) {
        "ta" -> "இன்னும் உள்ளடக்கம் இல்லை"
        "hi" -> "अभी तक कोई सामग्री नहीं"
        else -> "No content yet"
    }

    fun noShortContentHint(): String = when (languageCode) {
        "ta" -> "மேலே வேறு வகையைத் தேர்ந்தெடுக்கவும் அல்லது புதுப்பிக்க இழுக்கவும்"
        "hi" -> "ऊपर दूसरी श्रेणी चुनें या रीफ़्रेश करने के लिए खींचें"
        else -> "Try another category above, or pull down to refresh"
    }

    /** Fallback label when API returns a new short-content type we do not map yet. */
    fun shortContentTypeOther(): String = when (languageCode) {
        "ta" -> "மேலும்"
        "hi" -> "अन्य"
        else -> "More"
    }

    fun shortContentTypeLabel(type: String): String = when (type.uppercase()) {
        "RIDDLE" -> riddle()
        "JOKE" -> joke()
        "THOUGHT_FOR_THE_DAY" -> thoughtForTheDay()
        "PROVERB" -> when (languageCode) {
            "ta" -> "பழமொழி"
            "hi" -> "कहावत"
            else -> "Proverb"
        }
        "FUN_FACT" -> when (languageCode) {
            "ta" -> "சுவாரஸ்யமான உண்மை"
            "hi" -> "मज़ेदार तथ्य"
            else -> "Fun Fact"
        }
        "TONGUE_TWISTER" -> when (languageCode) {
            "ta" -> "நாக்குழற்சி"
            "hi" -> "जीभ मुड़ना"
            else -> "Tongue Twister"
        }
        "WORD_OF_THE_DAY" -> when (languageCode) {
            "ta" -> "இன்றைய வார்த்தை"
            "hi" -> "आज का शब्द"
            else -> "Word of the Day"
        }
        "BRAIN_TEASER" -> when (languageCode) {
            "ta" -> "மூளையிடுக்கி"
            "hi" -> "पहेली"
            else -> "Brain Teaser"
        }
        "AFFIRMATION" -> when (languageCode) {
            "ta" -> "உறுதிமொழி"
            "hi" -> "पुष्टि"
            else -> "Affirmation"
        }
        "QUOTE" -> when (languageCode) {
            "ta" -> "மேற்கோள்"
            "hi" -> "उद्धरण"
            else -> "Quote"
        }
        "DID_YOU_KNOW" -> when (languageCode) {
            "ta" -> "தெரியுமா?"
            "hi" -> "क्या आप जानते हैं?"
            else -> "Did you know?"
        }
        "RHYME" -> when (languageCode) {
            "ta" -> "பாடல்"
            "hi" -> "कविता"
            else -> "Rhyme"
        }
        "TRIVIA" -> when (languageCode) {
            "ta" -> "பொது அறிவு"
            "hi" -> "सामान्य ज्ञान"
            else -> "Trivia"
        }
        else -> when (languageCode) {
            "ta", "hi" -> shortContentTypeOther()
            else -> type.replace('_', ' ').split(' ')
                .joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { c -> c.titlecase() }
                }
        }
    }

    // --- Life readiness (family radar; synced soft counters per child) ---

    fun lifeReadinessScreenTitle(): String = when (languageCode) {
        "ta" -> "வாழ்க்கைத் தயார்நிலை"
        "hi" -> "जीवन तैयारी"
        else -> "Life readiness"
    }

    fun lifeReadinessHeroEyebrow(): String = when (languageCode) {
        "ta" -> "குடும்பப் பார்வை"
        "hi" -> "परिवार के लिए"
        else -> "Family view"
    }

    fun lifeReadinessSubtitle(): String = when (languageCode) {
        "ta" ->
            "ஐந்து மென்மையான பரிமாணங்கள் — பள்ளி மதிப்பெண்கள் அல்ல. கதை தேர்வுகளிலிருந்து உங்கள் கணக்கில் ஒத்திசைகிறது."
        "hi" ->
            "पाँच कोमल आयाम — स्कूल के अंक नहीं। कहानी विकल्पों से आपके खाते में समन्वयित।"
        else ->
            "Five gentle dimensions — not school marks. Synced from interactive story choices on your Tamixa account."
    }

    fun lifeReadinessLoading(): String = when (languageCode) {
        "ta" -> "ஏற்றுகிறது…"
        "hi" -> "लोड हो रहा है…"
        else -> "Loading your snapshot…"
    }

    fun lifeReadinessSectionHowFeels(): String = when (languageCode) {
        "ta" -> "இப்போதைய உணர்வு"
        "hi" -> "अभी कैसा लगता है"
        else -> "How things feel right now"
    }

    fun lifeReadinessSectionBadges(): String = when (languageCode) {
        "ta" -> "திறந்த பேட்ஜ்கள்"
        "hi" -> "अनलॉक बैज"
        else -> "Badges your family unlocked"
    }

    fun lifeReadinessSectionNext(): String = when (languageCode) {
        "ta" -> "அடுத்த கேட்பு யோசனை"
        "hi" -> "अगली सुनने की सलाह"
        else -> "Suggested next listen"
    }

    fun lifeReadinessPickStory(): String = when (languageCode) {
        "ta" -> "கதையைத் தேர்ந்தெடு"
        "hi" -> "कहानी चुनें"
        else -> "Pick a story"
    }

    fun lifeReadinessOpenPracticeHub(): String = when (languageCode) {
        "ta" -> "பயிற்சி மையத்தைத் திற"
        "hi" -> "अभ्यास हब खोलें"
        else -> "Open practice hub"
    }

    fun lifeReadinessEmptyTitle(): String = when (languageCode) {
        "ta" -> "குறுகிய பயிற்சிக் கதையுடன் தொடங்கவும்"
        "hi" -> "एक छोटी अभ्यास कहानी से शुरू करें"
        else -> "Start with a short practice tale"
    }

    fun lifeReadinessEmptyBody(): String = when (languageCode) {
        "ta" ->
            "இன்டராக்டிவ் கற்றல் கதைகளை முடிக்கும்போது, இங்கே மென்மையான சுருக்கம் தோன்றும். குழந்தையைத் தேர்ந்தெடுத்து நூலகத்தில் பயிற்சி மையத்தை முயற்சிக்கவும்."
        "hi" ->
            "इंटरैक्टिव सीख वाली कहानियाँ पूरी करने पर यहाँ कोमल सार दिखेगा। बच्चा चुनकर लाइब्रेरी में अभ्यास हब आज़माएँ।"
        else ->
            "When you finish interactive Learn tales, a soft summary appears here. Choose a child below and try the library practice hub."
    }

    fun lifeReadinessPillLabel(): String = when (languageCode) {
        "ta" -> "வாழ்க்கைத் தயார்நிலை"
        "hi" -> "जीवन तैयारी"
        else -> "Life readiness"
    }

    fun lifeReadinessMenuItem(): String = when (languageCode) {
        "ta" -> "வாழ்க்கைத் தயார்நிலை (விளக்கப்படம்)"
        "hi" -> "जीवन तैयारी (चार्ट)"
        else -> "Life readiness snapshot"
    }

    fun lifeReadinessAxisTech(): String = when (languageCode) {
        "ta" -> "தொழில்நுட்பம்"
        "hi" -> "टेक"
        else -> "Tech"
    }

    fun lifeReadinessAxisBusiness(): String = when (languageCode) {
        "ta" -> "வணிகம் / பணம்"
        "hi" -> "व्यवसाय / पैसा"
        else -> "Business"
    }

    fun lifeReadinessAxisLeadership(): String = when (languageCode) {
        "ta" -> "தலைமை"
        "hi" -> "नेतृत्व"
        else -> "Leadership"
    }

    fun lifeReadinessAxisEthics(): String = when (languageCode) {
        "ta" -> "நெறிமுறைகள்"
        "hi" -> "नैतिकता"
        else -> "Ethics"
    }

    fun lifeReadinessAxisCommunication(): String = when (languageCode) {
        "ta" -> "தொடர்பு"
        "hi" -> "संवाद"
        else -> "Communication"
    }

    fun lifeReadinessAxisNoteTech(): String = when (languageCode) {
        "ta" -> "பாதுகாப்பான டிஜிட்டல் பழக்கங்கள்"
        "hi" -> "सुरक्षित डिजिटल आदतें"
        else -> "Safe, smart technology habits"
    }

    fun lifeReadinessAxisNoteBusiness(): String = when (languageCode) {
        "ta" -> "பண உணர்வு மற்றும் திட்டமிடல்"
        "hi" -> "पैसे की समझ और योजना"
        else -> "Money sense and planning"
    }

    fun lifeReadinessAxisNoteLeadership(): String = when (languageCode) {
        "ta" -> "மற்றவர்களுடன் வழிநடத்துதல்"
        "hi" -> "दूसरों के साथ नेतृत्व"
        else -> "Guiding and working with others"
    }

    fun lifeReadinessAxisNoteEthics(): String = when (languageCode) {
        "ta" -> "நேர்மை மற்றும் யோசித்தல்"
        "hi" -> "ईमानदारी और सोच"
        else -> "Honesty and careful thinking"
    }

    fun lifeReadinessAxisNoteCommunication(): String = when (languageCode) {
        "ta" -> "அமைதியான, தெளிவான பேச்சு"
        "hi" -> "शांत, स्पष्ट बातचीत"
        else -> "Calm, clear expression"
    }

    fun lifeReadinessStatusLabel(tier: Int): String = when (languageCode) {
        "ta" -> when (tier) {
            0 -> "தொடக்கம்"
            1 -> "வளர்ச்சி"
            2 -> "நிலையானது"
            3 -> "நெகிழ்வு"
            4 -> "வலிமை"
            else -> "முழுமை"
        }
        "hi" -> when (tier) {
            0 -> "शुरुआत"
            1 -> "बढ़त"
            2 -> "स्थिर"
            3 -> "लचीला"
            4 -> "मज़बूत"
            else -> "शीर्ष"
        }
        else -> when (tier) {
            0 -> "Starting out"
            1 -> "Growing"
            2 -> "Building steady"
            3 -> "Resilient"
            4 -> "Strong"
            else -> "Master"
        }
    }

    fun lifeReadinessBadgeTitle(badgeId: String): String = when (languageCode) {
        "ta" -> when (badgeId) {
            "scam-proof-senior" -> "மோசடி-எதிர் மூத்தோர்"
            "kirana-king" -> "கிராணா ராஜா"
            "team-captain" -> "குழு கேப்டன்"
            "truth-seeker" -> "உண்மை தேடுபவர்"
            "clear-voice" -> "தெளிவான குரல்"
            else -> badgeId
        }
        "hi" -> when (badgeId) {
            "scam-proof-senior" -> "घोटाला-सुरक्षित"
            "kirana-king" -> "किराना किंग"
            "team-captain" -> "टीम कप्तान"
            "truth-seeker" -> "सच के खोजी"
            "clear-voice" -> "स्पष्ट आवाज़"
            else -> badgeId
        }
        else -> when (badgeId) {
            "scam-proof-senior" -> "Scam-Proof Senior"
            "kirana-king" -> "Kirana King"
            "team-captain" -> "Team Captain"
            "truth-seeker" -> "Truth Seeker"
            "clear-voice" -> "Clear Voice"
            else -> badgeId
        }
    }

    fun lifeReadinessBadgeLine(badgeId: String): String = when (languageCode) {
        "ta" -> when (badgeId) {
            "scam-proof-senior" -> "டிஜிட்டல் பாதுகாப்பில் கூர்மையான உணர்வு வளர்கிறது."
            "kirana-king" -> "மதிப்பு, சேமிப்பு, சிறு வணிக நுண்ணறிவு."
            "team-captain" -> "நியாயத்துடனும் தொடர்ச்சியுடனும் முன்வருதல்."
            "truth-seeker" -> "நேர்மையைத் தேர்வு செய்து சரிபார்க்கிறது."
            "clear-voice" -> "அமைதியாகவும் தெளிவாகவும் பேசவும் கேட்கவும்."
            else -> ""
        }
        "hi" -> when (badgeId) {
            "scam-proof-senior" -> "डिजिटल सुरक्षा में तेज़ समझ बन रही है।"
            "kirana-king" -> "कीमत, बचत और छोटे व्यवसाय की समझ।"
            "team-captain" -> "निष्पक्षता और जिम्मेदारी के साथ आगे आना।"
            "truth-seeker" -> "ईमानदारी चुनना और जाँच करना।"
            "clear-voice" -> "शांति और स्पष्टता से बोलना और सुनना।"
            else -> ""
        }
        else -> when (badgeId) {
            "scam-proof-senior" -> "Your child is building sharp instincts for digital safety."
            "kirana-king" -> "Great nose for value, saving, and small-business smarts."
            "team-captain" -> "Stepping up with fairness and follow-through."
            "truth-seeker" -> "Chooses honesty and checks facts before acting."
            "clear-voice" -> "Growing calm, clear ways to speak up and listen."
            else -> ""
        }
    }

    fun lifeReadinessRecHeadline(axis: com.tamixa.domain.LifeReadinessAxisId): String = when (languageCode) {
        "ta" -> when (axis) {
            com.tamixa.domain.LifeReadinessAxisId.TECH ->
                "ஒன்றாக டிஜிட்டல் நியாயத்தை பயிற்சி செய்யுங்கள்"
            com.tamixa.domain.LifeReadinessAxisId.BUSINESS ->
                "பணக் கதைகளை ஒன்றாக கேளுங்கள்"
            com.tamixa.domain.LifeReadinessAxisId.LEADERSHIP ->
                "குழு மற்றும் நியாயம் பற்றிய கதைகள்"
            com.tamixa.domain.LifeReadinessAxisId.ETHICS ->
                "நேர்மை மற்றும் மெதுவான யோசனை கதைகள்"
            com.tamixa.domain.LifeReadinessAxisId.COMMUNICATION ->
                "அமைதியான உரையாடல் பயிற்சி"
        }
        "hi" -> when (axis) {
            com.tamixa.domain.LifeReadinessAxisId.TECH ->
                "साथ में डिजिटल समझ अभ्यास करें"
            com.tamixa.domain.LifeReadinessAxisId.BUSINESS ->
                "पैसे वाली कहानियाँ साथ सुनें"
            com.tamixa.domain.LifeReadinessAxisId.LEADERSHIP ->
                "टीमवर्क और निष्पक्षता की कहानियाँ"
            com.tamixa.domain.LifeReadinessAxisId.ETHICS ->
                "ईमानदारी और धीमी सोच वाली कहानियाँ"
            com.tamixa.domain.LifeReadinessAxisId.COMMUNICATION ->
                "शांत बातचीत का अभ्यास"
        }
        else -> when (axis) {
            com.tamixa.domain.LifeReadinessAxisId.TECH ->
                "Lean into digital judgment together"
            com.tamixa.domain.LifeReadinessAxisId.BUSINESS ->
                "Practice money stories side by side"
            com.tamixa.domain.LifeReadinessAxisId.LEADERSHIP ->
                "Stories about teamwork and fairness"
            com.tamixa.domain.LifeReadinessAxisId.ETHICS ->
                "Honesty and \"slow thinking\" tales"
            com.tamixa.domain.LifeReadinessAxisId.COMMUNICATION ->
                "Calm conversation practice"
        }
    }

    fun lifeReadinessRecMessage(axis: com.tamixa.domain.LifeReadinessAxisId): String = when (languageCode) {
        "ta" -> when (axis) {
            com.tamixa.domain.LifeReadinessAxisId.TECH ->
                "நூலகத்தில் Learn · Simulator அல்லது டிஜிட்டல் பாதுகாப்புக் கதைகளைத் தேர்ந்தெடுங்கள்."
            com.tamixa.domain.LifeReadinessAxisId.BUSINESS ->
                "சேமிப்பு அல்லது குடும்ப பட்ஜெட் பற்றிய கதைகளைத் தேடுங்கள்."
            com.tamixa.domain.LifeReadinessAxisId.LEADERSHIP ->
                "கருணையுடன் மோதலைத் தீர்க்கும் கதைகளைத் தேர்ந்தெடுங்கள்."
            com.tamixa.domain.LifeReadinessAxisId.ETHICS ->
                "உண்மை மற்றும் விளைவுகள் பற்றிய கதைகளை விரும்புங்கள்."
            com.tamixa.domain.LifeReadinessAxisId.COMMUNICATION ->
                "உணர்வுகளை எளிய சொற்களில் பகிர்ந்து கொள்ளுங்கள்."
        }
        "hi" -> when (axis) {
            com.tamixa.domain.LifeReadinessAxisId.TECH ->
                "लाइब्रेरी में Learn · Simulator या डिजिटल सुरक्षा कहानियाँ चुनें।"
            com.tamixa.domain.LifeReadinessAxisId.BUSINESS ->
                "बचत या परिवार बजट वाली कहानियाँ खोजें।"
            com.tamixa.domain.LifeReadinessAxisId.LEADERSHIP ->
                "दया से विवाद सुलझाने वाली कहानियाँ चुनें।"
            com.tamixa.domain.LifeReadinessAxisId.ETHICS ->
                "सच्चाई और परिणामों वाली कहानियाँ पसंद करें।"
            com.tamixa.domain.LifeReadinessAxisId.COMMUNICATION ->
                "भावनाओं को सरल शब्दों में साझा करें।"
        }
        else -> when (axis) {
            com.tamixa.domain.LifeReadinessAxisId.TECH ->
                "Pick a Learn · Simulator or digital safety tale next — short episodes with choices help spot scams and kind boundaries online."
            com.tamixa.domain.LifeReadinessAxisId.BUSINESS ->
                "Look for library tales about saving, earning, or family budgets — chat about what you’d do in each scene."
            com.tamixa.domain.LifeReadinessAxisId.LEADERSHIP ->
                "Choose adventures where characters resolve conflict or lead with empathy — pause and ask what they’d try next."
            com.tamixa.domain.LifeReadinessAxisId.ETHICS ->
                "Favor stories about telling the truth, research, and consequences — celebrate small honest wins at home."
            com.tamixa.domain.LifeReadinessAxisId.COMMUNICATION ->
                "Use stories with dialogue prompts; take turns naming feelings in plain words — no pressure to perform."
        }
    }
}
