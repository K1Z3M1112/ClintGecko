package com.simplefox.browser.browser.webview
import com.simplefox.browser.browser.MainActivity
import java.util.concurrent.ConcurrentHashMap

private val jsAssetCache = ConcurrentHashMap<String, String>()

internal fun MainActivity.loadJsAsset(filename: String): String {
    return jsAssetCache.getOrPut(filename) {
        assets.open("JavaScript/$filename").bufferedReader().use { it.readText() }
    }
}
