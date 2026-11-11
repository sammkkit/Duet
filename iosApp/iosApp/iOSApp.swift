import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        MainViewControllerKt.setUpKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
