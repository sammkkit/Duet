package com.samkit.duet.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samkit.duet.core.designsystem.components.InfoPill
import com.samkit.duet.core.designsystem.theme.DuetTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Red dot + mm:ss while recording. */
@Composable
fun RecordingPill(
    elapsed: Duration,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    InfoPill(
        text = formatElapsed(elapsed),
        modifier = modifier.border(1.dp, colors.error, CircleShape),
        leading = { Box(Modifier.size(8.dp).background(colors.error, CircleShape)) },
        containerColor = colors.errorContainer,
        contentColor = colors.onErrorContainer,
        // tnum: every digit the same width, so the pill doesn't wobble each second
        textStyle = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
    )
}

// String.format is JVM-only, so pad by hand in commonMain.
internal fun formatElapsed(elapsed: Duration): String =
    elapsed.toComponents { minutes, seconds, _ ->
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

@Preview
@Composable
private fun RecordingPillPreview() {
    DuetTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            RecordingPill(elapsed = 42.seconds)
        }
    }
}
