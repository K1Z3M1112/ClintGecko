package com.simplefox.browser.util

import android.content.Context
import android.content.res.Configuration

/**
 * Replacement for AppCompatDelegate night mode. Returns the uiMode night bits that must be forced onto an
 * activity's resources for the user's theme choice, or 0 to follow the system.
 */
object NightMode {
    fun override(context: Context): Int =
        when (PreferenceManager.getDefaultSharedPreferences(context).getString("app_theme", "system")) {
            "dark" -> Configuration.UI_MODE_NIGHT_YES
            "light" -> Configuration.UI_MODE_NIGHT_NO
            else -> 0
        }
}
