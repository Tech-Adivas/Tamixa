import SwiftUI

struct ComposeViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let vc = IosAppKt.MainViewController(
            baseUrl: TamixaInfo.apiBaseUrl,
            defaultSubscriptionWebUrl: TamixaInfo.subscriptionWebUrl,
            environment: TamixaInfo.environment
        )
        IosAppKt.setHostViewControllerForPickers(vc: vc)
        return vc
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
