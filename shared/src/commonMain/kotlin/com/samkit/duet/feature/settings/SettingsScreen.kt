package com.samkit.duet.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samkit.duet.core.designsystem.icons.DuetIcons
import com.samkit.duet.core.designsystem.theme.DuetTheme
import com.samkit.duet.core.settings.UserSettings
import com.samkit.duet.ui.SectionHeader
import com.samkit.duet.ui.SwitchRow
import com.samkit.duet.ui.ValueRow
import duet.shared.generated.resources.Res
import duet.shared.generated.resources.audio
import duet.shared.generated.resources.audio_value
import duet.shared.generated.resources.back
import duet.shared.generated.resources.back_quality
import duet.shared.generated.resources.back_quality_value
import duet.shared.generated.resources.dual_supported_body
import duet.shared.generated.resources.dual_supported_title
import duet.shared.generated.resources.front_quality
import duet.shared.generated.resources.front_quality_value
import duet.shared.generated.resources.keep_originals
import duet.shared.generated.resources.keep_originals_desc
import duet.shared.generated.resources.mirror_front
import duet.shared.generated.resources.mirror_front_desc
import duet.shared.generated.resources.photo_format
import duet.shared.generated.resources.photo_format_value
import duet.shared.generated.resources.save_to
import duet.shared.generated.resources.save_to_value
import duet.shared.generated.resources.section_dual
import duet.shared.generated.resources.section_photo
import duet.shared.generated.resources.section_storage
import duet.shared.generated.resources.settings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScreen(settings = settings, onAction = viewModel::onAction, onBack = onBack)
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Surface, not just background(): it also sets the content color, so plain Text is light on dark.
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(DuetIcons.ArrowBack, contentDescription = stringResource(Res.string.back)) }
                Text(stringResource(Res.string.settings), style = MaterialTheme.typography.titleLarge)
            }

            SectionHeader(stringResource(Res.string.section_dual))
            ValueRow(stringResource(Res.string.back_quality), stringResource(Res.string.back_quality_value))
            ValueRow(stringResource(Res.string.front_quality), stringResource(Res.string.front_quality_value))
            ValueRow(stringResource(Res.string.audio), stringResource(Res.string.audio_value))
            SwitchRow(
                title = stringResource(Res.string.keep_originals),
                description = stringResource(Res.string.keep_originals_desc),
                checked = settings.keepOriginals,
                onToggle = { onAction(SettingsAction.ToggleKeepOriginals) },
            )

            SectionHeader(stringResource(Res.string.section_photo))
            ValueRow(stringResource(Res.string.photo_format), stringResource(Res.string.photo_format_value))
            SwitchRow(
                title = stringResource(Res.string.mirror_front),
                description = stringResource(Res.string.mirror_front_desc),
                checked = settings.mirrorFrontCamera,
                onToggle = { onAction(SettingsAction.ToggleMirrorFront) },
            )

            SectionHeader(stringResource(Res.string.section_storage))
            ValueRow(stringResource(Res.string.save_to), stringResource(Res.string.save_to_value))

            DeviceSupportCard()
        }
    }
}

@Composable
private fun DeviceSupportCard() {
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(DuetIcons.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.dual_supported_title), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(Res.string.dual_supported_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() {
    DuetTheme { SettingsScreen(settings = UserSettings(), onAction = {}, onBack = {}) }
}
