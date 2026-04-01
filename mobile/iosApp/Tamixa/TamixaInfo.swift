import Foundation

enum TamixaInfo {
    /// Resolves Info.plist entries; falls back if build setting was not substituted.
    static func string(_ key: String, default defaultValue: String) -> String {
        guard let raw = Bundle.main.object(forInfoDictionaryKey: key) as? String else {
            return defaultValue
        }
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return defaultValue }
        if trimmed.hasPrefix("$(") { return defaultValue }
        return trimmed
    }

    static var apiBaseUrl: String {
        string("TAMIXA_API_BASE_URL", default: "http://127.0.0.1:8080")
    }

    static var subscriptionWebUrl: String {
        string("TAMIXA_SUBSCRIPTION_WEB_URL", default: "https://app.tamixa.com/subscription")
    }

    static var environment: String {
        string("TAMIXA_ENVIRONMENT", default: "prod")
    }
}
