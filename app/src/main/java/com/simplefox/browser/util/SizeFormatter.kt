package com.simplefox.browser.util

import android.content.Context
import java.util.Locale

const val PREF_MEASUREMENT_SYSTEM = "measurement_system"
const val MEASUREMENT_SYSTEM_BINARY = "binary"
const val MEASUREMENT_SYSTEM_DECIMAL = "decimal"
const val DEFAULT_MEASUREMENT_SYSTEM = MEASUREMENT_SYSTEM_BINARY

@Volatile
private var decimalUnitsEnabled = false

fun loadMeasurementSystemPreference(context: Context) {
    decimalUnitsEnabled = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(PREF_MEASUREMENT_SYSTEM, DEFAULT_MEASUREMENT_SYSTEM) == MEASUREMENT_SYSTEM_DECIMAL
}

fun setMeasurementSystemDecimal(decimal: Boolean) {
    decimalUnitsEnabled = decimal
}

fun formatFileSize(bytes: Long): String {
    val kb = if (decimalUnitsEnabled) 1000.0 else 1024.0
    val mb = kb * kb
    val gb = mb * kb
    return when {
        bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}

fun formatStorageBytes(bytes: Long): String {
    val kb = if (decimalUnitsEnabled) 1000.0 else 1024.0
    val mb = kb * kb
    val gb = mb * kb
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}
