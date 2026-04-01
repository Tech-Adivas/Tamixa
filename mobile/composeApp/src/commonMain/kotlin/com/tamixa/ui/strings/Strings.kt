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
        "ta" -> "வயது (1–12)"
        "hi" -> "उम्र (1–12)"
        else -> "Age (1–12)"
    }

    fun bedtimeStory(): String = when (languageCode) {
        "ta" -> "இரவு கதை (அமைதியான)"
        "hi" -> "रात की कहानी (शांत)"
        else -> "Bedtime story (calm)"
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
        "ta" -> "$days நாள் தொடர்!"
        "hi" -> "$days दिन का सिलसिला!"
        else -> "$days day streak!"
    }

    fun buildYourStreak(): String = when (languageCode) {
        "ta" -> "தினமும் கேட்டு தொடரை உருவாக்குங்கள்"
        "hi" -> "रोज़ सुनकर सिलसिला बनाएं"
        else -> "Listen daily to build your streak"
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
        "ta" -> "கதைசொல்லும் குரலை உருவாக்க குறுகிய குரல் மாதிரியை பதிவு செய்யுங்கள்"
        "hi" -> "कहानी सुनाने की आवाज़ बनाने के लिए एक छोटा ऑडियो रिकॉर्ड करें"
        else -> "Record a short voice sample to create a storytelling voice"
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
        "ta" -> "யாரோ ஒருவரின் புகைப்படம் பதிவேற்றுங்கள் (மூத்தவர், பெற்றோர்)"
        "hi" -> "किसी का फोटो अपलोड करें (दादा, माता-पिता)"
        else -> "Upload a photo of someone (grandparent, parent, etc.)"
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
        "ta" -> "உங்கள் குழந்தைக்கான தனிப்பட்ட கதைகளை உருவாக்குங்கள்"
        "hi" -> "अपने बच्चे के लिए व्यक्तिगत कहानियाँ बनाएँ"
        else -> "Create personalized stories for your child"
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
        else -> type
    }

    // --- Onboarding daily quote samples (Thought, Proverb, Tongue twister, Quote) ---
    fun onboardingQuoteThought(): String = when (languageCode) {
        "ta" -> "கதைகள் கற்பனையை வளர்க்கின்றன."
        "hi" -> "कहानियाँ कल्पना को बढ़ाती हैं।"
        else -> "Stories grow imagination."
    }

    fun onboardingQuoteProverb(): String = when (languageCode) {
        "ta" -> "கற்றது கைமண் அளவு."
        "hi" -> "ज्ञान सबसे बड़ा धन है।"
        else -> "A book is a dream you hold in your hands."
    }

    fun onboardingQuoteTongueTwister(): String = when (languageCode) {
        "ta" -> "செந்தமிழ் நாடெனும் போதினிலே."
        "hi" -> "चार कच्चे चाचा चार पक्के पापा।"
        else -> "She sells seashells by the seashore."
    }

    fun onboardingQuoteQuote(): String = when (languageCode) {
        "ta" -> "கதை சொல்லுதல் என்பது அன்பைப் பகிர்வது."
        "hi" -> "कहानी सुनाना प्यार बांटना है।"
        else -> "There is no greater power than a story."
    }
}
