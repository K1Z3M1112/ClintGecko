package com.simplefox.browser.browser.menu

import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DataSaverOff
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.simplefox.browser.R

enum class CustomizableMenuItem(val id: String) {
    NEW_TAB("new_tab"),
    NEW_INCOGNITO_TAB("new_incognito_tab"),
    NEW_PROFILE_TAB("new_profile_tab"),
    SHARE("share"),
    OPEN_IN_APP("open_in_app"),
    CREATE_SHORTCUT("create_shortcut"),
    DOWNLOADS("downloads"),
    EXTENSIONS("extensions"),
    WEBSITE_BLOCKER("website_blocker"),
    BOOKMARKS("bookmarks"),
    HISTORY("history"),
    TRANSLATE("translate"),
    DESKTOP_MODE("desktop_mode"),
    DATA_SAVER("data_saver"),
    DISABLE_DATA_SAVER_FOR_SITE("disable_data_saver_for_site"),
    MUTE_TAB("mute_tab"),
    COOKIES_CONTROL("cookies_control");

    companion object {
        val DEFAULT_ORDER: List<CustomizableMenuItem> = entries.toList()
        fun fromId(id: String): CustomizableMenuItem? = entries.find { it.id == id }
    }
}

fun CustomizableMenuItem.icon(): ImageVector = when (this) {
    CustomizableMenuItem.NEW_TAB -> Icons.Filled.Add
    CustomizableMenuItem.NEW_INCOGNITO_TAB -> Icons.Filled.VisibilityOff
    CustomizableMenuItem.NEW_PROFILE_TAB -> Icons.Filled.AccountCircle
    CustomizableMenuItem.SHARE -> Icons.Filled.Share
    CustomizableMenuItem.OPEN_IN_APP -> Icons.AutoMirrored.Filled.OpenInNew
    CustomizableMenuItem.CREATE_SHORTCUT -> Icons.AutoMirrored.Filled.AddToHomeScreen
    CustomizableMenuItem.DOWNLOADS -> Icons.Filled.Download
    CustomizableMenuItem.EXTENSIONS -> Icons.Filled.Extension
    CustomizableMenuItem.WEBSITE_BLOCKER -> Icons.Filled.Shield
    CustomizableMenuItem.BOOKMARKS -> Icons.Filled.BookmarkBorder
    CustomizableMenuItem.HISTORY -> Icons.Filled.History
    CustomizableMenuItem.TRANSLATE -> Icons.Filled.Translate
    CustomizableMenuItem.DESKTOP_MODE -> Icons.Filled.DesktopWindows
    CustomizableMenuItem.DATA_SAVER -> Icons.Filled.DataSaverOn
    CustomizableMenuItem.DISABLE_DATA_SAVER_FOR_SITE -> Icons.Filled.DataSaverOff
    CustomizableMenuItem.MUTE_TAB -> Icons.AutoMirrored.Filled.VolumeOff
    CustomizableMenuItem.COOKIES_CONTROL -> CookieIcon
}

fun CustomizableMenuItem.titleRes(): Int = when (this) {
    CustomizableMenuItem.NEW_TAB -> R.string.new_tab
    CustomizableMenuItem.NEW_INCOGNITO_TAB -> R.string.new_incognito_tab
    CustomizableMenuItem.NEW_PROFILE_TAB -> R.string.profiles_new_tab_in
    CustomizableMenuItem.SHARE -> R.string.share_url
    CustomizableMenuItem.OPEN_IN_APP -> R.string.menu_open_in_app
    CustomizableMenuItem.CREATE_SHORTCUT -> R.string.menu_create_shortcut
    CustomizableMenuItem.DOWNLOADS -> R.string.menu_downloads
    CustomizableMenuItem.EXTENSIONS -> R.string.ext_title
    CustomizableMenuItem.WEBSITE_BLOCKER -> R.string.menu_website_blocker
    CustomizableMenuItem.BOOKMARKS -> R.string.menu_bookmarks
    CustomizableMenuItem.HISTORY -> R.string.menu_history
    CustomizableMenuItem.TRANSLATE -> R.string.translation_menu_translate
    CustomizableMenuItem.DESKTOP_MODE -> R.string.desktop_mode
    CustomizableMenuItem.DATA_SAVER -> R.string.menu_data_saver
    CustomizableMenuItem.DISABLE_DATA_SAVER_FOR_SITE -> R.string.menu_disable_data_saver_for_site
    CustomizableMenuItem.MUTE_TAB -> R.string.menu_mute_tab
    CustomizableMenuItem.COOKIES_CONTROL -> R.string.menu_cookies_control
}

object BrowserMenuCustomizationStore {
    private const val PREF_ORDER = "browser_menu_item_order"
    private const val PREF_HIDDEN = "browser_menu_hidden_items"
    private const val PREF_ROWS = "browser_menu_rows"
    const val MIN_ROWS = 1
    const val MAX_ROWS = 3

    fun readRows(prefs: SharedPreferences): Int =
        runCatching { prefs.getInt(PREF_ROWS, MIN_ROWS) }.getOrDefault(MIN_ROWS).coerceIn(MIN_ROWS, MAX_ROWS)

    fun writeRows(prefs: SharedPreferences, rows: Int) {
        prefs.edit().putInt(PREF_ROWS, rows.coerceIn(MIN_ROWS, MAX_ROWS)).apply()
    }

    fun readOrder(prefs: SharedPreferences): List<CustomizableMenuItem> {
        val storedIds = prefs.getString(PREF_ORDER, null)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val storedItems = storedIds.mapNotNull { CustomizableMenuItem.fromId(it) }
        val missing = CustomizableMenuItem.DEFAULT_ORDER.filter { it !in storedItems }
        return storedItems + missing
    }

    fun readHidden(prefs: SharedPreferences): Set<CustomizableMenuItem> {
        val storedIds = prefs.getStringSet(PREF_HIDDEN, null) ?: emptySet()
        return storedIds.mapNotNull { CustomizableMenuItem.fromId(it) }.toSet()
    }

    fun writeOrder(prefs: SharedPreferences, order: List<CustomizableMenuItem>) {
        prefs.edit().putString(PREF_ORDER, order.joinToString(",") { it.id }).apply()
    }

    fun writeHidden(prefs: SharedPreferences, hidden: Set<CustomizableMenuItem>) {
        prefs.edit().putStringSet(PREF_HIDDEN, hidden.map { it.id }.toSet()).apply()
    }

    fun resetToDefault(prefs: SharedPreferences) {
        prefs.edit().remove(PREF_ORDER).remove(PREF_HIDDEN).remove(PREF_ROWS).apply()
    }

    fun visibleOrderedItems(prefs: SharedPreferences): List<CustomizableMenuItem> {
        val hidden = readHidden(prefs)
        return readOrder(prefs).filter { it !in hidden }
    }
}
