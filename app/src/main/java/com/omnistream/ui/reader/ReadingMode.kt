package com.omnistream.ui.reader

/**
 * Reading modes for manga/manhwa reader
 * Based on Saikou's reader modes
 */
enum class ReadingMode {
    /**
     * Vertical continuous scroll (Webtoon/Manhwa style)
     * Best for: Webtoons, Manhwa, Manhua
     */
    VERTICAL_CONTINUOUS,

    /**
     * Horizontal left-to-right paging (Western comics style)
     * Best for: Western comics, some manga
     */
    HORIZONTAL_LTR,

    /**
     * Horizontal right-to-left paging (Traditional manga style)
     * Best for: Japanese manga
     */
    HORIZONTAL_RTL,

    /**
     * Dual page mode (two pages side-by-side)
     * Best for: Tablets, landscape mode
     */
    DUAL_PAGE;

    val displayName: String
        get() = when (this) {
            VERTICAL_CONTINUOUS -> "Vertical (Webtoon)"
            HORIZONTAL_LTR -> "Left to Right"
            HORIZONTAL_RTL -> "Right to Left"
            DUAL_PAGE -> "Dual Page"
        }

    val description: String
        get() = when (this) {
            VERTICAL_CONTINUOUS -> "Continuous vertical scrolling"
            HORIZONTAL_LTR -> "Swipe left to go forward"
            HORIZONTAL_RTL -> "Swipe right to go forward (manga)"
            DUAL_PAGE -> "Two pages side-by-side"
        }
}
