# DualCam Build Plan

25 Oct 2026 · Samkit Jain

## How to use this plan

You write every line of app code; AI explains, answers questions and reviews. Nine phases, each ending in a working app and a review before you move on.

### Rules while building

- Ask AI "why" and "how does this work", not "write this screen". Good asks: explain an API, decode a Gradle or Xcode error, compare two approaches, suggest test cases.
- If AI shows a snippet, retype it yourself and change it to fit. Never paste more than ~20 lines.
- Read the official docs for each phase's "You'll learn" topics before you start coding.
- One git branch per phase (`phase-2-single-android`), small commits with clear messages.
- Keep a `docs/notes/phase-N.md`: what you learned, what confused you, decisions you made and why.

### Review at the end of every phase (in Claude Code, in your terminal)

1. Tick every box in the phase's "Done when" list yourself first.
2. Open Claude Code at the project root on the phase branch.
3. Ask: "Review phase N of DualCam against docs/PLAN.md. Check architecture, correctness, Kotlin/Swift style, threading and leaks. Don't fix anything: list issues by severity, then quiz me on 3 things from my phase notes."
4. Fix the issues yourself, ask for a re-review, merge, and log it in the tracker at the bottom.

PRs into `main` also get an automatic review from `.github/workflows/pr-review.yml`.

## Module structure

```
Duet/
├── androidApp/            # Android entry point only
├── iosApp/                # Xcode project + Swift camera engine (later)
├── shared/                # features + App()
├── core/
│   ├── model/             # plain data types (Lens, CaptureType, CaptureLayout, SavedFile)
│   ├── designsystem/      # theme + generic components
│   ├── ui/                # camera-aware reusable components
│   ├── camera/            # CameraController + CameraX / AVFoundation
│   ├── media/             # saving files to the gallery
│   ├── settings/          # DataStore
│   └── permissions/       # camera + mic
├── build-logic/           # convention plugins: duet.kmp.library, duet.compose.library
└── gradle/libs.versions.toml
```

Dependency rules:

- `androidApp → shared → core:*`. Nothing in `core` ever depends on `shared`.
- `core:ui → core:designsystem + core:model`.
- `core:camera → core:model`.
- `core:model` and `core:designsystem` depend on nothing of yours.
- Feature-only components stay in the feature package until a second screen needs them.

## Phase 0 — Project setup

**Goal:** an empty app that runs on Android and iOS, already split into the final modules.

**You'll learn:** KMP source sets (commonMain, androidMain, iosMain), Gradle version catalogs, convention plugins, how the shared framework gets into Xcode, a first expect/actual.

**Build**

1. Generate a Compose Multiplatform project (Android + iOS) with the KMP wizard.
2. Add modules: `shared`, `core:model`, `core:designsystem`, `core:ui`, `core:camera`, `core:media`, `core:settings`, `core:permissions`. One placeholder class each.
3. Put every dependency version in `gradle/libs.versions.toml`.
4. Create `build-logic/` convention plugins (kmp-library, compose-library) so module build files stay a few lines.
5. Add an expect/actual `platformName()` and show it on screen.
6. Set up Koin, plus ktlint (detekt 2.x is still alpha; 1.23 doesn't support Kotlin 2.4).
7. Add GitHub Actions: build Android and run tests on every push.

**Done when**

- [x] App shows "Hello from Android" on an emulator
- [ ] App shows "Hello from iOS" on a simulator (deferred until Xcode is installed)
- [x] `./gradlew build` and lint pass locally
- [ ] CI is green

**In review I'll check:** module boundaries and dependency direction (features depend on core, never the reverse), no duplicated versions, convention plugins doing real work.

## Phase 1 — Design system and static UI

**Goal:** all 8 screens from the design, built in Compose with fake data and working navigation. No camera yet: previews are colored boxes.

**Design:** https://claude.ai/artifact/Sf2Uue6UTFGze2ScLtVwmd

**You'll learn:** Material 3 theming (color scheme, typography, shapes), state hoisting, type-safe Navigation Compose, shared ViewModel + StateFlow, Compose resources, drag gestures.

**Build**

1. `designsystem/Theme.kt`: dark color scheme from the design (primary `#ADC6FF`, back camera blue, front camera orange), Roboto type scale.
2. Components: ShutterButton (photo / video idle / recording), ModeTabs, CaptureTypeToggle, CameraChip, ZoomChips, CircleIconButton.
3. Screens: Camera (4 layouts), layout picker as a ModalBottomSheet, Settings, Review, dual-unavailable dialog.
4. CameraViewModel with `CameraUiState(layout, captureType, isRecording, elapsed)` and a fake recording timer.
5. Pure-Compose layout logic: PiP window drags and snaps to corners; split divider drags between 30% and 70%.
6. All strings in Compose resources.

**Done when**

- [ ] Every screen matches the design on both platforms
- [ ] Layout tabs switch layouts; the shutter toggles recording and the timer runs
- [ ] PiP drag-and-snap and split resize work smoothly
- [ ] Composables hold no business logic; each component has a preview

**In review I'll check:** recomposition (stable state, remember, derivedStateOf), state hoisting, theme tokens instead of hard-coded colors, accessibility (content descriptions, 48 dp touch targets).

## Phase 2 — Single camera on Android

**Goal:** real Single mode on Android: live preview, photo, video with sound, saved to the gallery.

**You'll learn:** CameraX use cases (Preview, ImageCapture, VideoCapture + Recorder), lifecycle binding, runtime permissions, MediaStore, turning callbacks into coroutines (suspendCancellableCoroutine), CameraX threading.

**Build**

1. A first, small CameraController interface in `core:camera/commonMain` (bind single, take photo, start/stop recording). Keep it minimal; it grows in Phase 3.
2. CameraXController in androidMain implementing it.
3. CameraPreview actual for Android (PreviewView inside AndroidView).
4. Flip, zoom, tap-to-focus, flash.
5. `core:permissions`: camera + mic for Android, iOS stub that returns "denied".
6. `core:media`: MediaSaver for Android (MediaStore, DCIM/DualCam) and a shared FileNaming (`VID_20261025_1842.mp4`).
7. Handle backgrounding and rotation while recording.

**Done when**

- [ ] Photo and video with sound appear in the phone's gallery
- [ ] Flip, zoom, flash and tap-to-focus work
- [ ] Denying permission shows a clear explanation screen, no crash
- [ ] Backgrounding mid-recording finishes the file cleanly; no leaks

**In review I'll check:** lifecycle and leaks, nothing heavy on the main thread, recording error paths (finalize events), coroutine scopes, and that `shared` never imports CameraX.

## Phase 3 — Camera abstraction + single camera on iOS

**Goal:** the same Single mode on a real iPhone, driven by the exact same CameraViewModel.

**You'll learn:** Kotlin/Native ↔ Swift interop (why Swift can't easily implement suspend or Flow interfaces), AVCaptureSession, AVCapturePhotoOutput, AVCaptureMovieFileOutput, UIKitView in Compose, Info.plist usage strings, PHPhotoLibrary.

**Build**

1. Finish the common API: CaptureLayout (Single / Pip / Split / Separate), Capabilities, SavedFile, a sealed CameraError. Design it now for dual modes even though only Single works.
2. NativeCameraEngine in iosMain: plain callback-style functions Swift can implement.
3. IosCameraController in iosMain: wraps the engine into the same suspend + StateFlow API Android has.
4. Swift SingleCamEngine + a preview UIView using AVCaptureVideoPreviewLayer; pass it to Kotlin from iOSApp.swift.
5. iOS MediaSaver (PHPhotoLibrary) and iOS permissions.
6. A FakeCameraController in commonTest, and ViewModel unit tests that use it.

**Done when**

- [ ] Photo + video work on a real iPhone (the simulator has no camera)
- [ ] CameraViewModel contains zero platform code
- [ ] ViewModel tests pass with the fake controller

**In review I'll check:** whether the interface will survive dual modes, Swift retain cycles, session work on a serial queue (never main), test quality.

## Phase 4 — Dual camera, Separate files mode (Android)

**Goal:** back and front capture at the same time and save as separate files, each camera set to photo or video on its own.

**You'll learn:** ConcurrentCamera, availableConcurrentCameraInfos, the FEATURE_CAMERA_CONCURRENT check, SingleCameraConfig, non-composition binding, hardware limits when two cameras run, starting two recordings together.

**Build**

1. Real Capabilities detection; show the dual-unavailable dialog from Phase 1 when there's no front/back pair.
2. ConcurrentBinder: bind two SingleCameraConfigs, each with its own Preview plus VideoCapture or ImageCapture.
3. Two live previews in SeparateLayout.
4. DualRecorder: start and stop both, share one timestamp in both file names, decide which file gets the mic audio.
5. If one camera fails, stop both and show an error.

**Done when**

- [ ] Each capture saves two files with matching names
- [ ] Mixed mode works (back video + front photos)
- [ ] A phone without dual support shows the dialog, and Single still works
- [ ] Tested on at least one real phone that supports dual

**In review I'll check:** cleanup of both streams, the one-camera-fails path, file naming, how close together the two recordings start.

## Phase 5 — Composed modes: Video call and Split (Android)

**Goal:** Video call (PiP) and Split saved as one combined video that matches what the preview showed.

**You'll learn:** CompositionSettings (alpha, offset, scale, z-order), normalized coordinates, mapping a Compose layout to composition values, changing composition while bound (newer CameraX), and basic OpenGL / CameraEffect if Split needs cropping.

**Build**

1. Put the layout math in commonMain as pure Kotlin (normalized rectangles per camera) so it's unit-testable; convert to CompositionSettings in androidMain.
2. CompositionFactory: Pip (corner, size, shape) and Split (direction, ratio) → two settings.
3. Swap main/small window and move the window corner.
4. Spike: can "keep originals" work alongside composition? Write a short decision record with what you found.
5. Spike: how to take a photo in composed modes (composition mode may not allow ImageCapture). Decide and write it down.

**Done when**

- [ ] Recorded PiP and Split videos match the preview layout
- [ ] Swap and corner change work
- [ ] Layout math has unit tests
- [ ] Two decision records written: photos in composed modes, keep originals

**In review I'll check:** the math and its tests, preview vs saved-file mismatches, how quality limits are handled, whether your decision records hold up.

## Phase 6 — Dual camera on iOS

**Goal:** all four modes on a supported iPhone, behind the same NativeCameraEngine API, ideally with no changes to commonMain.

**You'll learn:** AVCaptureMultiCamSession, manual connections between inputs and outputs, hardwareCost and systemPressureCost, AVCaptureVideoDataOutput, sample and pixel buffers, compositing frames with Metal or Core Image, AVAssetWriter with audio, timestamp sync.

**Build**

1. MultiCamEngine.swift: both cameras in one multi-cam session; check isMultiCamSupported; pick formats that keep hardwareCost under budget.
2. Separate files: one AVAssetWriter per camera.
3. FrameCompositor.swift: draw main + small window, or both halves, into one buffer, and write it with one AVAssetWriter.
4. Reuse your Phase 5 layout math so iOS and Android produce the same layouts.
5. React to thermal / system pressure: lower frame rate, then stop with a message.

**Done when**

- [ ] All 4 modes work on a supported iPhone
- [ ] commonMain unchanged, or every change justified in your notes
- [ ] A 5-minute dual recording finishes without stutter or a crash

**In review I'll check:** pixel-buffer memory, queue usage, audio/video timestamp sync, budget and pressure handling. This phase is the test of your Phase 3 abstraction.

## Phase 7 — Settings, review screen and data

**Goal:** everything around capture works for real: settings persist and change capture, and each capture can be reviewed, shared and deleted as a group.

**You'll learn:** DataStore (multiplatform), a local database (Room KMP or SQLDelight), the repository pattern, querying saved media (MediaStore, PHAsset), video playback per platform (Media3 vs AVPlayer), the share sheet via expect/actual.

**Build**

1. Settings screen live: per-camera quality (only options the device supports), audio, keep originals, mirror front camera, save location.
2. A captures table that groups the files of one capture (combined + back + front).
3. Review screen: play the combined video, list its files, share, delete.
4. Last-capture thumbnail on the camera screen.
5. Validate settings against Capabilities before binding.

**Done when**

- [ ] Settings survive an app restart and actually change what's recorded
- [ ] Review shows the right files for a capture on both platforms
- [ ] Share and delete work, and deleting removes the whole group

**In review I'll check:** separation of data, domain and UI layers, database schema and migrations, settings that can't exceed device limits.

## Phase 8 — Quality and release

**Goal:** a store-ready app in internal testing on Play Console and TestFlight.

**You'll learn:** testing in KMP (commonTest, Turbine for flows, Compose UI tests), performance work (baseline profiles, startup and memory), crash reporting, release builds (R8, signing, App Store Connect), store privacy forms.

**Build**

1. Tests: ViewModels, layout math, file naming, settings validation; UI tests for switching modes.
2. Error audit: every CameraError has a clear message and a recovery action.
3. Accessibility pass with TalkBack and VoiceOver.
4. Performance: camera open time, memory during dual recording, a baseline profile.
5. Crash reporting that supports KMP.
6. CI: all tests, an Android release build, an iOS build; signing keys from secrets.
7. Store listing, camera/mic usage text, Play data safety form, App Store privacy labels.

**Done when**

- [ ] CI runs every test and produces release builds
- [ ] Internal test release live on both stores
- [ ] Accessibility and error audits done

**In review I'll check:** a full-codebase review against this whole plan, plus the release checklist.

## Tracker

Update a row after each review: set the status, add the date, and note the top issue you fixed.

| Phase | Branch | Status | Reviewed on | Key review notes |
|---|---|---|---|---|
| 0 · Project setup | main | In progress | | iOS deferred until Xcode is installed |
| 1 · Design system + static UI | phase-1 | Not started | | |
| 2 · Single camera Android | phase-2-single-android | Not started | | |
| 3 · Abstraction + iOS single | phase-3-single-ios | Not started | | |
| 4 · Separate files Android | phase-4-separate-android | Not started | | |
| 5 · Composed modes Android | phase-5-composed-android | Not started | | |
| 6 · Dual camera iOS | phase-6-dual-ios | Not started | | |
| 7 · Settings, review, data | phase-7-data | Not started | | |
| 8 · Quality + release | phase-8-release | Not started | | |
