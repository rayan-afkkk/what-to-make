package com.whattocook.app.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** The app is designed dark-first, so a fresh install starts in dark mode. */
        val DEFAULT = DARK

        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}
