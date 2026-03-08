import SwiftUI

struct ComposeViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let baseUrl = (Bundle.main.infoDictionary?["ARARO_API_BASE_URL"] as? String) ?? "http://127.0.0.1:8080"
        return IosAppKt.MainViewController(baseUrl: baseUrl)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
