# Alternate options to support all Indian languages

Your current setup uses **Coqui XTTS v2**, which natively supports only **Hindi** among Indian languages; Tamil, Telugu, etc. are mapped to `hi` so synthesis runs but quality is a compromise. Below are alternatives that support multiple Indian languages (and often voice cloning).

---

## 1. **Cloud APIs (minimal infra, pay-per-use)**

| Provider | Indian languages | Voice cloning | Notes |
|----------|------------------|---------------|--------|
| **Sarvam AI (Bulbul)** | 11: Hindi, Bengali, Tamil, Telugu, Kannada, Malayalam, Marathi, Gujarati, Punjabi, Odia, English | Check docs | India-focused; sub-200ms latency; 30+ voices; [sarvam.ai](https://www.sarvam.ai/apis/text-to-speech) |
| **Bhashini** | 22+ Indian languages + Indian English | Check docs | Govt-backed; DNN TTS; [bhashini.gov.in](https://www.bhashini.gov.in) / bhashiniservices.com |
| **ElevenLabs** | Tamil, Hindi, and 28+ others in multilingual models | ✅ Yes (you already use it as fallback) | `Eleven Multilingual v2` supports Tamil; clone from reference; [elevenlabs.io/india](https://elevenlabs.io/india) |
| **Google Cloud TTS** | Hindi, and others (check [supported voices](https://cloud.google.com/text-to-speech/docs/voices)) | No (fixed voices) | WaveNet/Neural2; may have limited Tamil/Telugu; good for non-cloned TTS |

**Integration idea:** Add a **language-aware strategy**: for `ta`, `te`, `ml`, etc., call Sarvam/Bhashini/ElevenLabs instead of XTTS; keep XTTS for `hi` or for non-Indian languages if you prefer.

---

## 2. **Open-source / self-hosted (Indic-focused)**

| Project | Indian languages | Voice cloning | Notes |
|---------|------------------|---------------|--------|
| **AI4Bharat Indic-TTS** | 13 (Tamil, Telugu, Hindi, Bengali, etc.) | No (fixed voices) | FastPitch + HiFi-GAN; [github.com/AI4Bharat/Indic-TTS](https://github.com/AI4Bharat/Indic-TTS); Bhashini platform |
| **IndicF5** | 11 (Tamil, Telugu, Hindi, Bengali, etc.) | ✅ Yes (reference prompt) | 1.4k+ hrs data; on Hugging Face; [github.com/AI4Bharat/indicf5](https://github.com/AI4Bharat/indicf5) |
| **VoiceCloner** | 22 official Indian languages | Basic cloning | Tacotron 2 + WaveGlow; [github.com/thekartikeyamishra/VoiceCloner](https://github.com/thekartikeyamishra/VoiceCloner) |
| **KokoClone** | Hindi + multilingual | ✅ Zero-shot cloning | Kokoro-ONNX; [github.com/Ashish-Patnaik/kokoclone](https://github.com/Ashish-Patnaik/kokoclone) |

**Integration idea:** Run IndicF5 or another model as a **second service** (like XTTS). Backend chooses: if language is in `{ta, te, ml, kn, ...}` and Indic service is configured → call Indic service; else XTTS (or ElevenLabs).

---

## 3. **Recommended path for “all Indian languages”**

1. **Short term (no new service)**  
   - Use **ElevenLabs** for Tamil (and other supported Indian languages) when user selects them.  
   - You already have `ClonedVoiceStrategy` and ElevenLabs fallback; ensure `language` is passed through and that ElevenLabs is used for `ta`, `te`, etc. (or add a simple “use ElevenLabs for these codes” branch).  
   - No new infra; quality for Tamil will be better than XTTS-with-`hi`.

2. **Medium term (best quality per language)**  
   - Add **Sarvam AI (Bulbul)** or **Bhashini** as an optional TTS provider.  
   - Implement a small **Indic TTS adapter** (e.g. `SarvamTtsAdapter` / `BhashiniTtsAdapter`) and, for Indian language codes, route cloned or default narration through it.  
   - Gives native Tamil, Telugu, Malayalam, etc. with dedicated voices.

3. **Long term (self-hosted, full control)**  
   - Evaluate **IndicF5** (voice cloning + 11 Indian languages) or **Indic-TTS** (13 languages, no cloning).  
   - Run one of them as a separate service (similar to `xtts-service`) and add a backend adapter that calls it when `language` is an supported Indic code.

---

## 4. **Summary table**

| Goal | Option | Indian coverage | Cloning |
|------|--------|------------------|--------|
| Fast, minimal change | ElevenLabs (existing) | Tamil + 28+ others | ✅ |
| Native Indic quality, API | Sarvam Bulbul / Bhashini | 11–22+ languages | Varies |
| Self-hosted, Indic-first | IndicF5 / Indic-TTS | 11–13 languages | IndicF5 ✅ |
| Current | XTTS (ta→hi mapping) | Hindi only native | ✅ |

Using **ElevenLabs for Indian languages** (and keeping XTTS for Hindi or non-Indian) is the quickest way to “bring all Indian languages” with better quality than XTTS alone; adding Sarvam or Bhashini (or later, IndicF5) gives you dedicated Indic models and more control.
