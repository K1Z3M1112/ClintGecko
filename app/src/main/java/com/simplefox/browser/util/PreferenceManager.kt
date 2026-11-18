package com.simplefox.browser.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Drop-in for com.simplefox.browser.util.PreferenceManager's default-prefs lookup. The file name
 * (`<package>_preferences`) matches AndroidX so existing user settings are read unchanged.
 */
object PreferenceManager {

    fun getDefaultSharedPreferencesName(context: Context): String = context.packageName + "_preferences"

    fun getDefaultSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(getDefaultSharedPreferencesName(context), Context.MODE_PRIVATE)
}
