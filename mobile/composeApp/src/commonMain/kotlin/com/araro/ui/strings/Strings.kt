package com.araro.ui.strings

object Strings {
    var languageCode: String = com.araro.util.AraroConstants.DEFAULT_LANGUAGE
        private set

    fun setLanguage(code: String) {
        languageCode = code
    }

    fun appName(): String = when (languageCode) {
        "ta" -> "ஆராரோ"
        "hi" -> "अरारो"
        else -> "Araro"
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
        "ta" -> "குழந்தை சேர்"
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
        "ta" -> "கதை உருவாக்கு"
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
        "ta" -> "பெற்றோர் விருப்பம்"
        "hi" -> "अभिभावक निर्देश (वैकल्पिक)"
        else -> "Parent instructions (optional)"
    }

    fun interests(): String = when (languageCode) {
        "ta" -> "விருப்பங்கள்"
        "hi" -> "रुचियाँ"
        else -> "Interests"
    }

    fun childName(): String = when (languageCode) {
        "ta" -> "குழந்தை பெயர்"
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

    fun selectLanguage(): String = when (languageCode) {
        "ta" -> "மொழியை தேர்ந்தெடு"
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

    fun retry(): String = when (languageCode) {
        "ta" -> "மீண்டும் முயற்சி"
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

    fun newStory(): String = when (languageCode) {
        "ta" -> "புதிய கதை"
        "hi" -> "नई कहानी"
        else -> "New Story"
    }

    fun goodEvening(): String = when (languageCode) {
        "ta" -> "மாலை வணக்கம்"
        "hi" -> "शुभ संध्या"
        else -> "Good evening"
    }

    fun chooseLanguage(): String = when (languageCode) {
        "ta" -> "மொழியை தேர்ந்தெடுங்கள்"
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
        "ta" -> "மொபைல் எண் உள்ளிடவும்"
        "hi" -> "मोबाइल नंबर दर्ज करें"
        else -> "Enter mobile number"
    }

    fun sendOtp(): String = when (languageCode) {
        "ta" -> "OTP அனுப்பு"
        "hi" -> "OTP भेजें"
        else -> "Send OTP"
    }

    fun continueWith(): String = when (languageCode) {
        "ta" -> "தொடர்க"
        "hi" -> "जारी रखें"
        else -> "Continue"
    }

    fun chooseSignIn(): String = when (languageCode) {
        "ta" -> "உள்நுழைய தேர்ந்தெடு"
        "hi" -> "साइन इन चुनें"
        else -> "Choose how to sign in"
    }

    fun usageThisMonth(): String = when (languageCode) {
        "ta" -> "இந்த மாத பயன்பாடு"
        "hi" -> "इस महीने का उपयोग"
        else -> "Usage this month"
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
        "ta" -> "கேட்கும் முன்னேற்றம்"
        else -> "Listening progress"
    }

    fun storiesStarted(): String = when (languageCode) {
        "ta" -> "கதைகள் தொடங்கப்பட்டது"
        else -> "Stories started"
    }

    fun storiesCompleted(): String = when (languageCode) {
        "ta" -> "கதைகள் முடிக்கப்பட்டது"
        else -> "Stories completed"
    }

    fun completionRate(): String = when (languageCode) {
        "ta" -> "முடிப்பு விகிதம்"
        else -> "Completion rate"
    }

    fun consentHistory(): String = when (languageCode) {
        "ta" -> "சம்மத வரலாறு"
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
        "ta" -> "கதைகள் மற்றும் ஆடியோவுக்கு முழு அணுகல் உள்ளது."
        else -> "You have full access to stories and audio."
    }

    fun subscribeToUnlock(): String = when (languageCode) {
        "ta" -> "அனைத்து அம்சங்களையும் அன்லாக் செய்ய சந்தா செய்யுங்கள்."
        else -> "Subscribe to unlock all features."
    }

    fun cancelAtPeriodEndMessage(): String = when (languageCode) {
        "ta" -> "தற்போதைய காலத்தின் முடிவு வரை அணுகலை வைத்திருப்பீர்கள்."
        else -> "You'll keep access until the end of the current period."
    }

    fun childDetails(): String = when (languageCode) {
        "ta" -> "குழந்தை விவரங்கள்"
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
        "ta" -> "குறியீடு அனுப்பு"
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
        "ta" -> "தொடங்க உங்கள் முதல் குழந்தையைச் சேர்க்கவும்"
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
        "ta" -> "தூக்கம் நேரமிடி"
        else -> "Sleep timer"
    }

    fun sleepTimerHint(): String = when (languageCode) {
        "ta" -> "எத்தனை நிமிடங்களுக்குப் பிறகு நிறுத்தவும்"
        else -> "Stop playback after"
    }

    fun cancelTimer(): String = when (languageCode) {
        "ta" -> "நேரமிடியை ரத்து செய்"
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

    fun voiceLabel(voiceProfile: String, isPremium: Boolean = false): String {
        val base = when (voiceProfile.lowercase()) {
            "default" -> when (languageCode) {
                "ta" -> "இயல்புநிலை"
                "hi" -> "डिफ़ॉल्ट"
                else -> "Default"
            }
            "family" -> when (languageCode) {
                "ta" -> "உங்கள் குரல்"
                "hi" -> "आपकी आवाज़"
                else -> "Your Voice"
            }
            else -> when {
                voiceProfile.startsWith("cloned:") -> when (languageCode) {
                    "ta" -> "உங்கள் குரல் (க்ளோன்)"
                    "hi" -> "क्लोन की गई आवाज़"
                    else -> "Cloned Voice"
                }
                else -> voiceProfile.replaceFirstChar { it.uppercase() }
            }
        }
        return base + if (isPremium) " ★" else ""
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

    fun avatarPremiumRequired(): String = when (languageCode) {
        "ta" -> "கதை சொல்லும் அவதாரத்திற்கு பிரீமியம் தேவை"
        "hi" -> "कहानी अवतार के लिए प्रीमियम चाहिए"
        else -> "Premium required for storytelling avatar"
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

    fun welcomeToAraro(): String = when (languageCode) {
        "ta" -> "ஆராரோவிற்கு வரவேற்கிறோம்"
        "hi" -> "अरारो में आपका स्वागत है"
        else -> "Welcome to Araro"
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

    fun noStoriesYet(): String = when (languageCode) {
        "ta" -> "இன்னும் கதைகள் இல்லை"
        "hi" -> "अभी तक कोई कहानी नहीं"
        else -> "No stories yet"
    }

    fun tapNewStoryToCreate(): String = when (languageCode) {
        "ta" -> "முதல் சாகசத்தை உருவாக்க \"புதிய கதை\" தட்டுங்கள்"
        "hi" -> "अपना पहला रोमांच बनाने के लिए \"नई कहानी\" पर टैप करें"
        else -> "Tap \"New Story\" to create your first adventure!"
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

    fun noFavoritesYet(): String = when (languageCode) {
        "ta" -> "இன்னும் பிடித்தவை இல்லை"
        "hi" -> "अभी तक कोई पसंदीदा नहीं"
        else -> "No favorites yet"
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

    fun noInternet(): String = when (languageCode) {
        "ta" -> "இணைய இணைப்பு இல்லை. உங்கள் வலையமைப்பை சரிபார்க்கவும்."
        "hi" -> "इंटरनेट कनेक्शन नहीं। अपना नेटवर्क जांचें।"
        else -> "No internet connection. Check your network."
    }

    fun connectionProblem(): String = when (languageCode) {
        "ta" -> "இணைப்பு சிக்கல். உங்கள் வலையமைப்பை சரிபார்க்கவும்."
        "hi" -> "कनेक्शन समस्या। अपना नेटवर्क जांचें।"
        else -> "Connection problem. Please check your network."
    }

    fun somethingWentWrong(): String = when (languageCode) {
        "ta" -> "ஏதோ தவறியது. மீண்டும் முயற்சிக்கவும்."
        "hi" -> "कुछ गलत हो गया। फिर कोशिश करें।"
        else -> "Something went wrong. Please try again."
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
}
