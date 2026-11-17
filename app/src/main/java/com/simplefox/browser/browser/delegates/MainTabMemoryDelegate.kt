package com.simplefox.browser.browser.delegates

import android.app.ActivityManager
import com.simplefox.browser.browser.MainActivity

internal const val MAX_LIVE_BACKGROUND_TABS = 3
internal const val BACKGROUND_TRIM_KEEP_LIVE = 1
private const val GIB = 1024L * 1024L * 1024L

private var cachedLiveTabBudget = -1

internal fun MainActivity.liveBackgroundTabBudget(): Int {
    if (cachedLiveTabBudget < 0) {
        val manager = getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo()
        manager?.getMemoryInfo(memory)
        cachedLiveTabBudget = when {
            manager == null -> 2
            manager.isLowRamDevice -> 0
            memory.totalMem < 3 * GIB -> 1
            memory.totalMem < 5 * GIB -> 2
            else -> MAX_LIVE_BACKGROUND_TABS
        }
    }
    return cachedLiveTabBudget
}

internal fun MainActivity.trimBackgroundTabs(keepLive: Int = MAX_LIVE_BACKGROUND_TABS) {
    val activeId = tabManager.activeTab?.id
    tabManager.tabs
        .filter { it.id != activeId && it.webView.canHibernate() }
        .sortedByDescending { it.lastActiveAt }
        .drop(minOf(keepLive, liveBackgroundTabBudget()))
        .forEach { it.webView.hibernate() }
}

internal fun MainActivity.hibernateActiveTabIfHidden() {
    if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return
    tabManager.activeTab?.webView?.hibernate(force = true)
}
