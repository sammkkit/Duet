package com.samkit.duet.feature.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.components.InfoPill
import com.samkit.duet.core.designsystem.components.InfoPillDefaults
import com.samkit.duet.core.designsystem.icons.DuetIcons
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.ui.SectionHeader
import com.samkit.duet.ui.TitleAndDescription
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.back_to_camera
import duet.shared.generated.resources.badge_main
import duet.shared.generated.resources.change_layout
import duet.shared.generated.resources.delete_capture
import duet.shared.generated.resources.file_back
import duet.shared.generated.resources.file_back_desc
import duet.shared.generated.resources.file_combined
import duet.shared.generated.resources.file_combined_desc
import duet.shared.generated.resources.file_front
import duet.shared.generated.resources.file_front_desc
import duet.shared.generated.resources.files_header
import duet.shared.generated.resources.more_options
import duet.shared.generated.resources.play_video
import duet.shared.generated.resources.review_time
import duet.shared.generated.resources.review_title
import duet.shared.generated.resources.share
import org.jetbrains.compose.resources.stringResource

/**
 * One capture and its files. Phase 1: fixed example data.
 * Phase 7 adds a ViewModel (captures table, playback, share, delete) behind the same layout.
 */
@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val camera = DuetTheme.cameraColors
    // Surface, not just background(): it also sets the content color, so plain Text is light on dark.
    Surface(modifier.fillMaxSize(), color = colors.surfaceContainerLowest) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                    ) { Icon(DuetIcons.ArrowBack, contentDescription = stringResource(Res.string.back_to_camera)) }
                    Column {
                        Text(stringResource(Res.string.review_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(Res.string.review_time),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = {}) { Icon(DuetIcons.MoreVert, contentDescription = stringResource(Res.string.more_options)) }
            }

            // Player: combined video placeholder with the PiP window, play button and duration.
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(camera.back.placeholder),
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .size(92.dp, 124.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(camera.front.placeholder)
                        .border(2.dp, camera.front.accent, MaterialTheme.shapes.medium),
                )
                FilledIconButton(
                    onClick = {},
                    modifier = Modifier.align(Alignment.Center).size(64.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.onSurface,
                            contentColor = colors.surfaceContainerLowest,
                        ),
                ) {
                    Icon(DuetIcons.Play, contentDescription = stringResource(Res.string.play_video), modifier = Modifier.size(26.dp))
                }
                InfoPill(
                    text = "0:42",
                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                    height = InfoPillDefaults.SmallHeight,
                    containerColor = colors.surfaceContainerLowest,
                    contentColor = colors.onSurface,
                    textStyle = MaterialTheme.typography.labelMedium,
                    contentPadding = InfoPillDefaults.SmallContentPadding,
                )
            }

            SectionHeader(stringResource(Res.string.files_header))
            FileRow(
                title = stringResource(Res.string.file_combined),
                description = stringResource(Res.string.file_combined_desc),
                thumbnail = {
                    Thumbnail(camera.back.container) {
                        Box(
                            Modifier
                                .align(
                                    Alignment.TopEnd,
                                ).padding(5.dp)
                                .size(12.dp, 16.dp)
                                .background(camera.front.accent, RoundedCornerShape(3.dp)),
                        )
                    }
                },
                trailing = {
                    InfoPill(
                        text = stringResource(Res.string.badge_main),
                        height = 24.dp,
                        shape = RoundedCornerShape(6.dp),
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                        textStyle = MaterialTheme.typography.labelSmall,
                        contentPadding = InfoPillDefaults.SmallContentPadding,
                    )
                },
            )
            FileRow(stringResource(Res.string.file_back), stringResource(Res.string.file_back_desc), { Thumbnail(camera.back.container) })
            FileRow(stringResource(Res.string.file_front), stringResource(Res.string.file_front_desc), { Thumbnail(camera.front.accent) })

            // Phase 7 wires these up (share sheet, layout re-render, grouped delete).
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(DuetIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.share), modifier = Modifier.padding(start = 8.dp))
                }
                FilledTonalButton(onClick = {}, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(Res.string.change_layout))
                }
                OutlinedIconButton(
                    onClick = {},
                    modifier = Modifier.size(48.dp),
                    border = BorderStroke(1.dp, colors.outline),
                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = colors.error),
                ) {
                    Icon(DuetIcons.Delete, contentDescription = stringResource(Res.string.delete_capture), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    title: String,
    description: String,
    thumbnail: @Composable () -> Unit,
    trailing: @Composable () -> Unit = {
        Icon(
            DuetIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    },
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        thumbnail()
        TitleAndDescription(title, description, Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun Thumbnail(
    color: Color,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    Box(Modifier.size(40.dp).background(color, RoundedCornerShape(12.dp)), content = content)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ReviewScreenPreview() {
    DuetTheme { ReviewScreen(onBack = {}) }
}
