package com.samkit.duet.core.model

enum class LENS {
    FRONT,
    BACK,
}

enum class FLASH {
    AUTO,
    ON,
    OFF,
}

enum class CaptureType {
    PHOTO,
    VIDEO,
}

enum class CaptureLayout {
    SINGLE,
    PIP,
    SPLIT,
    SEPARATE,
}

enum class PipCorner { TopStart, TopEnd, BottomStart, BottomEnd }

enum class SplitDirection { TopBottom, LeftRight }
