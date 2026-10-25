package com.samkit.duet.core.model

enum class LENS(
    val value: String
) {
    FRONT("front"),
    BACK("back")
}

enum class FLASH(
    val value: String
) {
    AUTO("auto"),
    ON("on"),
    OFF("off")
}

enum class CAPTURE_TYPE(
    val value: String
) {
    PHOTO("photo"),
    VIDEO("video")
}

enum class CaptureLayout(
    val value: String
) {
    SINGLE("single"),
    PIP("pip"),
    SPLIT("split"),
    SEPARATE("separate")
}

enum class PipCorner { TopStart, TopEnd, BottomStart, BottomEnd }
enum class SplitDirection { TopBottom, LeftRight }
