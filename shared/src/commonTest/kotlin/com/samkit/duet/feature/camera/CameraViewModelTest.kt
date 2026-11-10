package com.samkit.duet.feature.camera

import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.CaptureType
import com.samkit.duet.core.model.FLASH
import com.samkit.duet.core.model.LENS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private fun videoViewModel() = CameraViewModel().apply { onCaptureTypeSelected(CaptureType.VIDEO) }

    @Test
    fun startsWithDefaultState() {
        assertEquals(CameraUiState(), CameraViewModel().state.value)
    }

    @Test
    fun shutterInVideoTogglesRecording() =
        runTest(dispatcher) {
            val vm = videoViewModel()
            vm.onShutterClick()
            assertTrue(vm.state.value.isRecording)
            vm.onShutterClick()
            assertFalse(vm.state.value.isRecording)
        }

    @Test
    fun shutterInPhotoDoesNotRecord() {
        val vm = CameraViewModel()
        vm.onShutterClick()
        assertFalse(vm.state.value.isRecording)
    }

    @Test
    fun timerCountsWhileRecordingAndStopsAfter() =
        runTest(dispatcher) {
            val vm = videoViewModel()
            vm.onShutterClick()
            advanceTimeBy(3.seconds)
            runCurrent()
            assertEquals(3.seconds, vm.state.value.elapsed)

            vm.onShutterClick()
            advanceTimeBy(5.seconds)
            runCurrent()
            assertEquals(3.seconds, vm.state.value.elapsed)
        }

    @Test
    fun newRecordingResetsTimer() =
        runTest(dispatcher) {
            val vm = videoViewModel()
            vm.onShutterClick()
            advanceTimeBy(2.seconds)
            runCurrent()
            vm.onShutterClick()

            vm.onShutterClick()
            assertEquals(Duration.ZERO, vm.state.value.elapsed)
            vm.onShutterClick()
        }

    @Test
    fun controlsAreLockedWhileRecording() =
        runTest(dispatcher) {
            val vm = videoViewModel()
            vm.onShutterClick()
            vm.onLayoutSelected(CaptureLayout.SPLIT)
            vm.onCaptureTypeSelected(CaptureType.PHOTO)
            assertEquals(CaptureLayout.SINGLE, vm.state.value.layout)
            assertEquals(CaptureType.VIDEO, vm.state.value.captureType)
            vm.onShutterClick()
        }

    @Test
    fun separateModeRecordsWhenEitherCameraIsVideo() =
        runTest(dispatcher) {
            val vm = CameraViewModel()
            vm.onLayoutSelected(CaptureLayout.SEPARATE)
            vm.onSeparateTypeSelected(LENS.BACK, CaptureType.PHOTO)
            vm.onSeparateTypeSelected(LENS.FRONT, CaptureType.PHOTO)
            vm.onShutterClick()
            assertFalse(vm.state.value.isRecording)

            vm.onSeparateTypeSelected(LENS.FRONT, CaptureType.VIDEO)
            vm.onShutterClick()
            assertTrue(vm.state.value.isRecording)
            vm.onShutterClick()
        }

    @Test
    fun splitRatioIsClamped() {
        val vm = CameraViewModel()
        vm.onSplitRatioChange(0.9f)
        assertEquals(MAX_SPLIT_RATIO, vm.state.value.splitRatio)
        vm.onSplitRatioChange(0.1f)
        assertEquals(MIN_SPLIT_RATIO, vm.state.value.splitRatio)
    }

    @Test
    fun flashCycles() {
        val vm = CameraViewModel()
        val seen = List(3) { vm.onFlashClick().let { vm.state.value.flash } }
        assertEquals(listOf(FLASH.AUTO, FLASH.ON, FLASH.OFF), seen)
    }

    @Test
    fun swapFlipsMainLens() {
        val vm = CameraViewModel()
        vm.onSwapCameras()
        assertEquals(LENS.FRONT, vm.state.value.mainLens)
    }
}
