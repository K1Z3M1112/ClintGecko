package com.simplefox.browser.browser.delegates

import android.app.ActivityManager
import com.simplefox.browser.browser.MainActivity
import com.simplefox.browser.tabs.BrowserTab

internal const val MAX_LIVE_BACKGROUND_TABS = 3
internal const val BACKGROUND_TRIM_KEEP_LIVE = 1
private const val GIB = 1024L * 1024L * 1024L
private const val MIB = 1024L * 1024L
private const val VISIT_BONUS_MS = 60_000L
private const val MAX_VISIT_BONUS_COUNT = 10

/** Higher means more likely to be revisited soon: recent use, plus a bonus for tabs the user keeps coming back to. */
internal fun BrowserTab.keepScore(): Long =
    lastActiveAt + visitCount.coerceAtMost(MAX_VISIT_BONUS_COUNT) * VISIT_BONUS_MS

internal fun MainActivity.liveBackgroundTabBudget(): Int {
    val manager = getSystemService(ActivityManager::class.java) ?: return 1
    val memory = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(memory)
    return when {
        manager.isLowRamDevice || memory.lowMemory || memory.availMem < 256 * MIB -> 0
        memory.availMem < 512 * MIB || memory.totalMem < 3 * GIB -> 1
        memory.totalMem < 5 * GIB -> 2
        else -> MAX_LIVE_BACKGROUND_TABS
    }
}

internal fun MainActivity.trimBackgroundTabs(keepLive: Int = MAX_LIVE_BACKGROUND_TABS) {
    val activeId = tabManager.activeTab?.id
    tabManager.tabs
        .filter { it.id != activeId && it.webView.canHibernate() }
        .sortedByDescending { it.keepScore() }
        .drop(minOf(keepLive, liveBackgroundTabBudget()))
        .forEach { it.webView.hibernate() }
}

internal fun MainActivity.hibernateActiveTabIfHidden() {
    if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return
    tabManager.activeTab?.webView?.hibernate(force = true)
}
