package com.simplefox.browser.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.simplefox.browser.util.PreferenceManager
import com.simplefox.browser.ui.ThemeRevealHolder
import com.simplefox.browser.ui.ThemeRevealOverlay
import com.simplefox.browser.ui.theme.ThemeMode
import com.simplefox.browser.ui.theme.resolveThemeChrome
import com.simplefox.browser.util.LocaleHelper
import kotlin.math.hypot
import kotlin.math.max

abstract class ClintActivity : ComponentActivity() {

    private var appliedTheme: String? = null
    private var appliedLanguage: String? = null
    private var appliedSystemDark = false

    private var storagePermissionCallback: ((Boolean) -> Unit)? = null

    private val legacyStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val callback = storagePermissionCallback
        storagePermissionCallback = null
        callback?.invoke(granted)
    }

    fun requestLegacyStoragePermission(onResult: (Boolean) -> Unit) {
        storagePermissionCallback = onResult
        legacyStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, com.simplefox.browser.util.NightMode.override(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        appliedTheme = prefs.getString("app_theme", "system") ?: "system"
        appliedLanguage = prefs.getString(LocaleHelper.PREF_APP_LANGUAGE, LocaleHelper.LANGUAGE_SYSTEM) ?: LocaleHelper.LANGUAGE_SYSTEM
        appliedSystemDark = ThemeMode.isSystemDark()
        applyWindowChrome()
        super.onCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        syncSystemTheme()
    }

    private fun syncSystemTheme() {
        val systemDark = ThemeMode.isSystemDark()
        if (systemDark == appliedSystemDark) return
        appliedSystemDark = systemDark
        if (appliedTheme == ThemeMode.SYSTEM) {
            applyWindowChrome()
            onSystemThemeChanged()
        }
    }

    protected open fun onSystemThemeChanged() {}

    override fun onResume() {
        super.onResume()
        openDialogCount = 0
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val currentTheme = prefs.getString("app_theme", "system") ?: "system"
        val currentLanguage = prefs.getString(LocaleHelper.PREF_APP_LANGUAGE, LocaleHelper.LANGUAGE_SYSTEM) ?: LocaleHelper.LANGUAGE_SYSTEM
        if (currentTheme != appliedTheme || currentLanguage != appliedLanguage) {
            window.setWindowAnimations(0)
            recreate()
            return
        }
        syncSystemTheme()
        applyStatusBarVisibility()
        applyWindowChrome()
        window.decorView.post {
            applyStatusBarVisibility()
            startRevealIfNeeded()
        }
    }

    @Suppress("DEPRECATION")
    private fun applyWindowChrome() {
        val theme = appliedTheme ?: "system"
        val chrome = resolveThemeChrome(theme)
        window.setBackgroundDrawable(ColorDrawable(chrome.background))
        window.statusBarColor = chrome.statusBar
        window.navigationBarColor = chrome.navigationBar
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = chrome.lightStatusBar
        controller.isAppearanceLightNavigationBars = chrome.lightNavigationBar
    }

    fun captureAndRecreate(newTheme: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val current = prefs.getString("app_theme", "system") ?: "system"
        if (current == newTheme) return
        captureScreenBitmap()
        prefs.edit().putString("app_theme", newTheme).commit()
        window.setWindowAnimations(0)
        recreate()
    }

    fun captureAndApplyLanguage(newLanguage: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val current = prefs.getString(LocaleHelper.PREF_APP_LANGUAGE, LocaleHelper.LANGUAGE_SYSTEM) ?: LocaleHelper.LANGUAGE_SYSTEM
        if (current == newLanguage) return
        captureScreenBitmap()
        prefs.edit().putString(LocaleHelper.PREF_APP_LANGUAGE, newLanguage).commit()
        window.setWindowAnimations(0)
        recreate()
    }

    private fun captureScreenBitmap() {
        val decor = window.decorView
        try {
            val bmp = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(bmp))
            ThemeRevealHolder.bitmap = bmp
            ThemeRevealHolder.cx = decor.width / 2
            ThemeRevealHolder.cy = decor.height / 2
        } catch (_: Exception) {
        }
    }

    private fun startRevealIfNeeded() {
        val (bmp, cx, cy) = ThemeRevealHolder.consume() ?: return
        if (!THEME_REVEAL_ANIMATION_ENABLED) {
            bmp.recycle()
            return
        }
        if (isFinishing || isDestroyed) {
            bmp.recycle()
            return
        }

        val decor = window.decorView as? android.view.ViewGroup ?: run {
            bmp.recycle()
            return
        }

        val maxRadius = hypot(
            max(cx, decor.width - cx).toDouble(),
            max(cy, decor.height - cy).toDouble()
        ).toFloat()

        val overlay = ThemeRevealOverlay(this, bmp, cx, cy).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        decor.addView(overlay)

        ValueAnimator.ofFloat(0f, maxRadius).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                overlay.revealRadius = it.animatedValue as Float
                overlay.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    decor.removeView(overlay)
                    bmp.recycle()
                }
            })
            start()
        }
    }

    private var openDialogCount = 0

    fun trackDialogShown() { openDialogCount++ }
    fun trackDialogDismissed() { if (openDialogCount > 0) openDialogCount-- }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && openDialogCount == 0) applyStatusBarVisibility()
    }

    private fun applyStatusBarVisibility() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hideStatusBar) {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
        if (hideSystemNavigation) {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
        window.decorView.requestApplyInsets()
    }
}

private const val THEME_REVEAL_ANIMATION_ENABLED = false
