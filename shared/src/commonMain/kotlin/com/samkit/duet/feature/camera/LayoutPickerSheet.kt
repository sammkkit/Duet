package com.samkit.duet.feature.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.components.SegmentedToggle
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.model.CaptureLayout
import com.samkit.duet.core.model.LENS
import com.samkit.duet.core.model.PipShape
import com.samkit.duet.ui.SwitchRow
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.done
import duet.shared.generated.resources.keep_originals_sheet
import duet.shared.generated.resources.keep_originals_sheet_desc
import duet.shared.generated.resources.layout_pip
import duet.shared.generated.resources.layout_pip_desc
import duet.shared.generated.resources.layout_separate
import duet.shared.generated.resources.layout_separate_desc
import duet.shared.generated.resources.layout_single
import duet.shared.generated.resources.layout_single_desc
import duet.shared.generated.resources.layout_split
import duet.shared.generated.resources.layout_split_desc
import duet.shared.generated.resources.lens_back
import duet.shared.generated.resources.lens_front
import duet.shared.generated.resources.main_camera
import duet.shared.generated.resources.options
import duet.shared.generated.resources.shape_circle
import duet.shared.generated.resources.shape_rounded
import duet.shared.generated.resources.sheet_subtitle
import duet.shared.generated.resources.sheet_title
import duet.shared.generated.resources.small_window
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Wide enough for "Rounded" plus the check mark on one line.
private val OptionSegmentWidth = 112.dp

/** Bottom sheet: pick a layout, then that layout's options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LayoutPickerSheet(
    state: CameraUiState,
    keepOriginals: Boolean,
    onAction: (CameraAction) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(CameraAction.DismissLayoutPicker) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.sheet_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(Res.string.sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CaptureLayout.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { layout ->
                            LayoutCard(
                                layout = layout,
                                selected = layout == state.layout,
                                onClick = { onAction(CameraAction.SelectLayout(layout)) },
                            )
                        }
                    }
                }
            }

            LayoutOptions(state, keepOriginals, onAction)

            Button(
                onClick = { onAction(CameraAction.DismissLayoutPicker) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(stringResource(Res.string.done))
            }
        }
    }
}

/** Only the options that apply to the chosen layout. */
@Composable
private fun LayoutOptions(
    state: CameraUiState,
    keepOriginals: Boolean,
    onAction: (CameraAction) -> Unit,
) {
    val layout = state.layout
    if (layout != CaptureLayout.PIP && layout != CaptureLayout.SPLIT) return

    Column {
        Text(
            stringResource(Res.string.options),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
        )
        val back = stringResource(Res.string.lens_back)
        val front = stringResource(Res.string.lens_front)
        OptionRow(stringResource(Res.string.main_camera)) {
            SegmentedToggle(
                items = LENS.entries.reversed(), // Back first
                selected = state.mainLens,
                onSelect = { onAction(CameraAction.SelectMainLens(it)) },
                label = { if (it == LENS.BACK) back else front },
                segmentWidth = OptionSegmentWidth,
            )
        }
        if (layout == CaptureLayout.PIP) {
            val rounded = stringResource(Res.string.shape_rounded)
            val circle = stringResource(Res.string.shape_circle)
            OptionRow(stringResource(Res.string.small_window)) {
                SegmentedToggle(
                    items = PipShape.entries,
                    selected = state.pipShape,
                    onSelect = { onAction(CameraAction.SelectPipShape(it)) },
                    label = { if (it == PipShape.ROUNDED) rounded else circle },
                    segmentWidth = OptionSegmentWidth,
                )
            }
        }
        SwitchRow(
            title = stringResource(Res.string.keep_originals_sheet),
            description = stringResource(Res.string.keep_originals_sheet_desc),
            checked = keepOriginals,
            onToggle = { onAction(CameraAction.ToggleKeepOriginals) },
            horizontalPadding = 8.dp,
        )
    }
}

@Composable
private fun OptionRow(
    title: String,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        control()
    }
}

@Composable
private fun RowScope.LayoutCard(
    layout: CaptureLayout,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Surface(
        selected = selected,
        onClick = onClick,
        shape = shape,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
        border = BorderStroke(2.dp, if (selected) colors.primary else colors.surfaceContainerHigh),
        modifier = Modifier.weight(1f),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LayoutDiagram(layout)
            Text(stringResource(layout.title()), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(layout.description()), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
    }
}

/** Tiny picture of the layout: back camera in blue, front in orange. */
@Composable
private fun LayoutDiagram(layout: CaptureLayout) {
    val back = DuetTheme.cameraColors.back.container
    val front = DuetTheme.cameraColors.front.accent
    val inner = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(8.dp),
    ) {
        when (layout) {
            CaptureLayout.SINGLE -> Box(Modifier.fillMaxSize().background(back, inner))
            CaptureLayout.PIP -> {
                Box(Modifier.fillMaxSize().background(back, inner))
                Box(
                    Modifier
                        .align(
                            Alignment.TopEnd,
                        ).padding(top = 5.dp, end = 6.dp)
                        .size(22.dp, 30.dp)
                        .background(front, RoundedCornerShape(5.dp)),
                )
            }
            CaptureLayout.SPLIT ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth().background(back, inner))
                    Box(Modifier.weight(1f).fillMaxWidth().background(front, inner))
                }
            CaptureLayout.SEPARATE ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(back, inner))
                    Box(Modifier.weight(1f).fillMaxHeight().background(front, inner))
                }
        }
    }
}

private fun CaptureLayout.title(): StringResource =
    when (this) {
        CaptureLayout.SINGLE -> Res.string.layout_single
        CaptureLayout.PIP -> Res.string.layout_pip
        CaptureLayout.SPLIT -> Res.string.layout_split
        CaptureLayout.SEPARATE -> Res.string.layout_separate
    }

private fun CaptureLayout.description(): StringResource =
    when (this) {
        CaptureLayout.SINGLE -> Res.string.layout_single_desc
        CaptureLayout.PIP -> Res.string.layout_pip_desc
        CaptureLayout.SPLIT -> Res.string.layout_split_desc
        CaptureLayout.SEPARATE -> Res.string.layout_separate_desc
    }
