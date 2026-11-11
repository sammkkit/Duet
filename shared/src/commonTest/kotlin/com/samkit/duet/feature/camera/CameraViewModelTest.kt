package com.samkit.duet.feature.camera

import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.CaptureType
import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.SavedFile
import com.samkit.duet.core.settings.SettingsRepository
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.msg_photo_failed
import duet.shared.generated.resources.msg_photo_saved
import duet.shared.generated.resources.msg_photos_saved
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {
    // viewModelScope runs on Dispatchers.Main, which doesn't exist in a JVM unit test.
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun videoViewModel() =
        CameraViewModel(SettingsRepository(), FakeCameraController()).apply { onAction(CameraAction.SelectCaptureType(CaptureType.VIDEO)) }

    @Test
    fun startsWithDefaultState() {
        assertEquals(CameraUiState(), CameraViewModel(SettingsRepository(), FakeCameraController()).state.value)
    }

    @Test
    fun shutterInVideoTogglesRecording() =
        runTest(dispatcher) {
            val vm = videoViewModel()
            vm.onAction(CameraAction.ShutterClick)
            assertTrue(vm.state.value.isRecording)
            vm.onAction(CameraAction.ShutterClick)
            assertFalse(vm.state.value.isRecording)
        }

    @Test
    fun shutterInPhotoDoesNotRecordAndShowsMessage() =
        runTest(dispatcher) {
            val vm = CameraViewModel(SettingsRepository(), FakeCameraController())
            vm.onAction(CameraAction.ShutterClick)
            assertFalse(vm.state.value.isRecording)
            assertIs<CameraEffect.ShowMessage>(vm.effects.first())
        }

    @Test
    fun timerCountsWhileRecordingAndStopsAfter() =
        runTest(dispatcher) {
            val vm = videoViewModel()
            vm.onAction(CameraAction.ShutterClick)
            advanceTimeBy(3.seconds)
            runCurrent()
            assertEquals(3.seconds, vm.state.value.elapsed)

            vm.onAction(CameraAction.ShutterClick)
            advanceTimeBy(5.seconds)
            runCurrent()
            assertEquals(3.seconds, vm.state.value.elapsed)
        }

    @Test
    fun newRecordingResetsTimer() =
        runTest(dispatcher) {
            val vm = videoViewModel()
            vm.onAction(CameraAction.ShutterClick)
            advanceTimeBy(2.seconds)
            runCurrent()
            vm.onAction(CameraAction.ShutterClick)

            vm.onAction(CameraAction.ShutterClick)
            assertEquals(Duration.ZERO, vm.state.value.elapsed)
            vm.onAction(CameraAction.ShutterClick)
        }

    @Test
    fun controlsAreLockedWhileRecording() =
        runTest(dispatcher) {
            val vm = videoViewModel()
            vm.onAction(CameraAction.ShutterClick)
            vm.onAction(CameraAction.SelectLayout(CaptureLayout.SPLIT))
            vm.onAction(CameraAction.SelectCaptureType(CaptureType.PHOTO))
            assertEquals(CaptureLayout.SINGLE, vm.state.value.layout)
            assertEquals(CaptureType.VIDEO, vm.state.value.captureType)
            assertIs<CameraEffect.ShowMessage>(vm.effects.first())
            vm.onAction(CameraAction.ShutterClick)
        }

    @Test
    fun separateModeRecordsWhenEitherCameraIsVideo() =
        runTest(dispatcher) {
            val vm = CameraViewModel(SettingsRepository(), FakeCameraController())
            vm.onAction(CameraAction.SelectLayout(CaptureLayout.SEPARATE))
            vm.onAction(CameraAction.SelectSeparateType(LENS.BACK, CaptureType.PHOTO))
            vm.onAction(CameraAction.SelectSeparateType(LENS.FRONT, CaptureType.PHOTO))
            vm.onAction(CameraAction.ShutterClick)
            assertFalse(vm.state.value.isRecording)

            vm.onAction(CameraAction.SelectSeparateType(LENS.FRONT, CaptureType.VIDEO))
            vm.onAction(CameraAction.ShutterClick)
            assertTrue(vm.state.value.isRecording)
            vm.onAction(CameraAction.ShutterClick)
        }

    @Test
    fun settingsButtonEmitsNavigation() =
        runTest(dispatcher) {
            val vm = CameraViewModel(SettingsRepository(), FakeCameraController())
            vm.onAction(CameraAction.OpenSettings)
            assertEquals(CameraEffect.NavigateToSettings, vm.effects.first())
        }

    @Test
    fun splitRatioIsClamped() {
        val vm = CameraViewModel(SettingsRepository(), FakeCameraController())
        vm.onAction(CameraAction.ChangeSplitRatio(0.9f))
        assertEquals(MAX_SPLIT_RATIO, vm.state.value.splitRatio)
        vm.onAction(CameraAction.ChangeSplitRatio(0.1f))
        assertEquals(MIN_SPLIT_RATIO, vm.state.value.splitRatio)
    }

    @Test
    fun flashCycles() {
        val vm = CameraViewModel(SettingsRepository(), FakeCameraController())
        val seen = List(3) { vm.onAction(CameraAction.CycleFlash).let { vm.state.value.flash } }
        assertEquals(listOf(FLASH.AUTO, FLASH.ON, FLASH.OFF), seen)
    }

    @Test
    fun swapFlipsMainLens() {
        val vm = CameraViewModel(SettingsRepository(), FakeCameraController())
        vm.onAction(CameraAction.SwapCameras)
        assertEquals(LENS.FRONT, vm.state.value.mainLens)
    }

    @Test
    fun dualLayoutOnUnsupportedPhoneOpensDialogInsteadOfSwitching() =
        runTest(dispatcher) {
            val vm = CameraViewModel(SettingsRepository(), FakeCameraController(supportsDual = false))
            runCurrent() // let the startup capability check finish
            vm.onAction(CameraAction.SelectLayout(CaptureLayout.SPLIT))
            assertTrue(vm.state.value.isDualUnavailableOpen)
            assertEquals(CaptureLayout.SINGLE, vm.state.value.layout)

            vm.onAction(CameraAction.UseSingleCamera)
            assertFalse(vm.state.value.isDualUnavailableOpen)
            assertEquals(CaptureLayout.SINGLE, vm.state.value.layout)
        }

    @Test
    fun keepOriginalsIsSharedThroughTheRepository() {
        val repository = SettingsRepository()
        val vm = CameraViewModel(repository, FakeCameraController())
        val before = repository.settings.value.keepOriginals
        vm.onAction(CameraAction.ToggleKeepOriginals)
        assertEquals(!before, repository.settings.value.keepOriginals)
        assertEquals(!before, vm.settings.value.keepOriginals)
    }

    @Test
    fun layoutPickerCannotOpenWhileRecording() =
        runTest(dispatcher) {
            val vm = videoViewModel()
            vm.onAction(CameraAction.ShutterClick)
            vm.onAction(CameraAction.OpenLayoutPicker)
            assertFalse(vm.state.value.isLayoutPickerOpen)
            vm.onAction(CameraAction.ShutterClick)
        }

    @Test
    fun photoShutterSavesThroughTheCameraAndConfirms() =
        runTest(dispatcher) {
            val camera = FakeCameraController()
            val vm = CameraViewModel(SettingsRepository(), camera)
            vm.onAction(CameraAction.ShutterClick)
            assertEquals(CameraEffect.ShowMessage(Res.string.msg_photo_saved), vm.effects.first())
            assertEquals(1, camera.photosTaken)
        }

    @Test
    fun twoSavedFilesShowTheBothPhotosMessage() =
        runTest(dispatcher) {
            val camera = FakeCameraController().apply { photoResult = Result.success(listOf(SavedFile("b"), SavedFile("f"))) }
            val vm = CameraViewModel(SettingsRepository(), camera)
            vm.onAction(CameraAction.SnapBothPhotos)
            assertEquals(CameraEffect.ShowMessage(Res.string.msg_photos_saved), vm.effects.first())
        }

    @Test
    fun failedPhotoShowsErrorMessage() =
        runTest(dispatcher) {
            val camera = FakeCameraController().apply { photoResult = Result.failure(IllegalStateException("disk full")) }
            val vm = CameraViewModel(SettingsRepository(), camera)
            vm.onAction(CameraAction.ShutterClick)
            assertEquals(CameraEffect.ShowMessage(Res.string.msg_photo_failed), vm.effects.first())
        }

    @Test
    fun zoomAndFlashReachTheCamera() {
        val camera = FakeCameraController()
        val vm = CameraViewModel(SettingsRepository(), camera)
        vm.onAction(CameraAction.SelectZoom(2f))
        vm.onAction(CameraAction.CycleFlash)
        assertEquals(2f, camera.currentZoom)
        assertEquals(FLASH.AUTO, camera.currentFlash)
    }
}
