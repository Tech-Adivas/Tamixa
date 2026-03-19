# Tamixa iOS App

SwiftUI app that hosts the Compose Multiplatform UI from the shared Kotlin framework (`shared`).

## Project layout

- **Tamixa.xcodeproj** – Xcode project (open this in Xcode).
- **Tamixa/** – App source and resources:
  - `App.swift` – SwiftUI `@main` entry; initializes Koin and shows Compose via `ComposeViewController`.
  - `ComposeViewController.swift` – `UIViewControllerRepresentable` that presents `IosAppKt.MainViewController()` (Compose UI).
  - `Info.plist` – Bundle config, usage descriptions (microphone, photo library).

## Setup

1. **Build the shared framework first (required for `import shared`)**  
   From the **repo root**, run once:
   ```bash
   ./mobile/iosApp/build-framework.sh
   ```
   This builds the Kotlin framework and copies it to `mobile/iosApp/Frameworks/shared.framework` so Xcode can find the `shared` module. If you skip this, you may see "No such module 'shared'" or "Cannot find 'shared' in scope".

2. **Open in Xcode**  
   Open `mobile/iosApp/Tamixa.xcodeproj` (from repo root: `open mobile/iosApp/Tamixa.xcodeproj`).

3. **Subsequent builds**  
   The Xcode **Run Script** phase builds the framework and copies it to `Frameworks/` before compiling Swift. So after the first successful build, you can just use ⌘B in Xcode. The script uses `-Ptamixa.iosOnly=true` so only the mobile project is configured (avoids backend Gradle issues).

## Running

1. Ensure the framework exists (run `./mobile/iosApp/build-framework.sh` once if needed).
2. Open `Tamixa.xcodeproj` in Xcode.
3. Select a simulator (e.g. iPhone 16).
4. Run (⌘R).

## API base URL

- **Simulator**: `http://127.0.0.1:8080` (backend on host machine).
- **Device**: Use your Mac’s IP, e.g. `http://192.168.1.x:8080`.
- **Production**: Update in `App.swift` init or use a config.

Config is set in `App.swift`: `IosAppKt.doInitKoin(baseUrl: "http://127.0.0.1:8080")`.
