import SwiftUI

@main
struct TamixaApp: App {
    init() {
        // Load Kotlin framework on main thread so Dispatchers.Main can bind to main queue.
        IosAppKt.warmupOnMainThread()
    }

    var body: some Scene {
        WindowGroup {
            ComposeViewController()
        }
    }
}
