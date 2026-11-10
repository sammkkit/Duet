package com.samkit.duet.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The design's icons, built from its SVG path data (24×24, 2px round stroke).
 * Icon() tints them with the current content color, so the black here never shows.
 */
object DuetIcons {
    val Flash = strokeIcon("Flash", "M13 2 4 14h7l-1 8 9-12h-7z")
    val FlashOff = strokeIcon("FlashOff", "M13 2 4 14h7l-1 8 9-12h-7z", "M3 3l18 18")
    val FlashAuto = strokeIcon("FlashAuto", "M11 2 3 13h6l-1 7 7-10H9z", "M16 21l2.5-7 2.5 7M17 19h3")
    val Layout =
        strokeIcon(
            "Layout",
            "M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z",
            "M13.5 6h3a1.5 1.5 0 0 1 1.5 1.5v4a1.5 1.5 0 0 1-1.5 1.5h-3a1.5 1.5 0 0 1-1.5-1.5v-4a1.5 1.5 0 0 1 1.5-1.5z",
        )
    val Settings =
        strokeIcon(
            "Settings",
            "M4 7h9M17 7h3M4 17h3M11 17h9",
            "M13 7a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
            "M7 17a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
        )
    val Swap = strokeIcon("Swap", "M7 4 3 8l4 4M3 8h13M17 12l4 4-4 4M21 16H8")
    val SwapVertical = strokeIcon("SwapVertical", "M8 3v14M4 7l4-4 4 4M16 21V7M20 17l-4 4-4-4")
    val Flip = strokeIcon("Flip", "M20 11a8 8 0 0 0-14.3-4.9M4 13a8 8 0 0 0 14.3 4.9", "M5 2v4h4M19 22v-4h-4")
    val Photo = strokeIcon("Photo", "M4 8h3l2-3h6l2 3h3v11H4z", "M8.5 13a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0")
    val Video = strokeIcon("Video", "M5 6h9a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z", "m16 10 5-3v10l-5-3")
    val SplitHorizontal =
        strokeIcon("SplitHorizontal", "M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z", "M3 12h18")
    val SplitVertical = strokeIcon("SplitVertical", "M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z", "M12 3v18")
    val File = strokeIcon("File", "M6 3h8l4 4v14H6z", "M14 3v4h4")
    val Timer = strokeIcon("Timer", "M4 13a8 8 0 1 0 16 0a8 8 0 1 0-16 0", "M12 9v4l2 2M10 2h4")
    val DragHandle = strokeIcon("DragHandle", "M8 9l4-4 4 4M8 15l4 4 4-4")
    val ArrowBack = strokeIcon("ArrowBack", "M19 12H5M12 19l-7-7 7-7")
    val ChevronRight = strokeIcon("ChevronRight", "m9 6 6 6-6 6")
    val Info = strokeIcon("Info", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0", "M12 11v5M12 8h.01")
    val Share =
        strokeIcon(
            "Share",
            "M15 5a3 3 0 1 0 6 0a3 3 0 1 0-6 0",
            "M3 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0",
            "M15 19a3 3 0 1 0 6 0a3 3 0 1 0-6 0",
            "m8.6 13.5 6.8 4M15.4 6.5l-6.8 4",
        )
    val CameraOff = strokeIcon("CameraOff", "M3 3l18 18", "M9.5 5H15l2 3h3v10.5M4 8v11h13", "M10.2 10.2a3.5 3.5 0 0 0 4.6 4.6")
    val Delete = strokeIcon("Delete", "M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14")
    val Play = fillIcon("Play", "M8 5v14l11-7z")
    val MoreVert =
        fillIcon(
            "MoreVert",
            "M10 5a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
            "M10 12a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
            "M10 19a2 2 0 1 0 4 0a2 2 0 1 0-4 0",
        )
    val Pause =
        fillIcon(
            "Pause",
            "M7 5h2a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z",
            "M15 5h2a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z",
        )
}

private fun strokeIcon(
    name: String,
    vararg paths: String,
): ImageVector =
    ImageVector
        .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            paths.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

private fun fillIcon(
    name: String,
    vararg paths: String,
): ImageVector =
    ImageVector
        .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply { paths.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) } }
        .build()
