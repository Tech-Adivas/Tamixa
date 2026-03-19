import SwiftUI

struct ComposeViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let baseUrl = (Bundle.main.infoDictionary?["TAMIXA_API_BASE_URL"] as? String) ?? "http://127.0.0.1:8080"
        let vc = IosAppKt.MainViewController(baseUrl: baseUrl)
        IosAppKt.setHostViewControllerForPickers(vc: vc)
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
