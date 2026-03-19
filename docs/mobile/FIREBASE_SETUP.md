# Firebase setup (Android)

The Android app uses **Firebase Crashlytics** for crash reporting. The repo ships with a **placeholder** `composeApp/google-services.json` so the project builds without a Firebase project. In that case you will see logcat warnings like:

- `Please set a valid API key. A Firebase API key is required to communicate with Firebase server APIs`
- `Error getting Firebase installation id`
- `Settings request failed` (FileNotFoundException to firebase-settings.crashlytics.com)

**The app still runs**; these are non-fatal. Crashlytics collection is **disabled in debug** builds when using the placeholder.

## To remove the warnings and enable Crashlytics

1. Open [Firebase Console](https://console.firebase.google.com/) and create or select a project.
2. Add an **Android app** with package name **`com.tamixa.android`**.
3. Download **`google-services.json`** from the project settings.
4. **Replace** `mobile/composeApp/google-services.json` with the downloaded file.
5. Rebuild the app: `./gradlew :mobile:composeApp:assembleDebug`

After replacing, Firebase and Crashlytics will initialize correctly and the warnings will stop. For **release** builds, use a real `google-services.json` and ensure Crashlytics is enabled (it is by default when not in debug).

## Security

Do **not** commit a production `google-services.json` with real API keys to a public repo. Use one of:

- A **private** repo or CI secrets to inject the file at build time.
- A **debug** Firebase project for local development and a separate project for production.
