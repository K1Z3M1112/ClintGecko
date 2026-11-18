package com.simplefox.browser.setup

import androidx.activity.addCallback
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.simplefox.browser.app.ClintApplication
import com.simplefox.browser.customtheme.resolveThemeChrome
import com.simplefox.browser.util.PreferenceManager
import com.simplefox.browser.R
import com.simplefox.browser.base.ClintActivity
import com.simplefox.browser.browser.MainActivity
import com.simplefox.browser.crash.CrashHandler
import com.simplefox.browser.ui.DocumentViewer
import com.simplefox.browser.ui.OverlayHostActivity
import com.simplefox.browser.util.LocaleHelper

class SetupActivity : ClintActivity(), OverlayHostActivity {

    private lateinit var uiState: SetupUiState

    override var overlayContent by mutableStateOf<(@Composable () -> Unit)?>(null)

    companion object {
        const val PRIVACY_POLICY_URL = "https://github.com/jhaiian/ClintGecko/blob/main/PRIVACY_POLICY.md"
        const val TERMS_URL = "https://github.com/jhaiian/ClintGecko/blob/main/TERMS_OF_SERVICE.md"
        private const val KEY_PENDING_PAGE = "setup_pending_page"
        private const val KEY_PENDING_CONSENT = "setup_pending_consent"
        private const val KEY_PENDING_HIDE_STATUS_BAR = "setup_pending_hide_status_bar"
        private const val KEY_PENDING_HIDE_SYSTEM_NAVIGATION = "setup_pending_hide_system_navigation"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            if (uiState.currentPage > 0) {
                uiState.currentPage -= 1
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        CrashHandler.install(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("setup_complete", false)) { startMainActivity(); return }

        var hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        var hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        if (prefs.contains(KEY_PENDING_HIDE_STATUS_BAR)) {
            hideStatusBar = prefs.getBoolean(KEY_PENDING_HIDE_STATUS_BAR, false)
            prefs.edit().remove(KEY_PENDING_HIDE_STATUS_BAR).apply()
        }
        if (prefs.contains(KEY_PENDING_HIDE_SYSTEM_NAVIGATION)) {
            hideSystemNavigation = prefs.getBoolean(KEY_PENDING_HIDE_SYSTEM_NAVIGATION, false)
            prefs.edit().remove(KEY_PENDING_HIDE_SYSTEM_NAVIGATION).apply()
        }
        var consentChecked = false
        if (prefs.contains(KEY_PENDING_CONSENT)) {
            consentChecked = prefs.getBoolean(KEY_PENDING_CONSENT, false)
            prefs.edit().remove(KEY_PENDING_CONSENT).apply()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val pendingPage = prefs.getInt(KEY_PENDING_PAGE, -1)
        val initialPage: Int
        if (pendingPage >= 0) {
            prefs.edit().remove(KEY_PENDING_PAGE).apply()
            initialPage = pendingPage
        } else {
            initialPage = 0
        }

        uiState = SetupUiState(
            initialPage = initialPage,
            initialTheme = prefs.getString("app_theme", "system") ?: "system",
            initialAccent = prefs.getString("accent_color", "material_you") ?: "material_you",
            initialIntensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint",
            initialLanguage = prefs.getString(LocaleHelper.PREF_APP_LANGUAGE, LocaleHelper.LANGUAGE_SYSTEM) ?: LocaleHelper.LANGUAGE_SYSTEM,
            initialAddressBarPosition = prefs.getString("address_bar_position", "bottom") ?: "bottom",
            initialScrollHideMode = prefs.getString("scroll_hide_mode", "off") ?: "off",
            initialHideStatusBar = hideStatusBar, initialHideSystemNavigation = hideSystemNavigation,
            initialEngine = prefs.getString("search_engine", "duckduckgo") ?: "duckduckgo",
            initialCustomEngineName = prefs.getString(com.simplefox.browser.browser.CustomSearchEngineNameKey, "") ?: "",
            initialCustomEngineUrl = prefs.getString(com.simplefox.browser.browser.CustomSearchEngineUrlKey, "") ?: ""
        )
        uiState.consentChecked = consentChecked

        setContent {
          SetupTheme(uiState) {
            SetupScreen(
                state = uiState,
                onPrivacyClick = {
                    DocumentViewer.show(this, getString(R.string.document_viewer_privacy_policy_title), DocumentViewer.PRIVACY_POLICY_URL)
                },
                onTermsClick = {
                    DocumentViewer.show(this, getString(R.string.document_viewer_terms_title), DocumentViewer.TERMS_URL)
                },
                onHideStatusBarToggled = { checked ->
                    uiState.hideStatusBar = checked
                    Toast.makeText(this, getString(R.string.setup_status_bar_applied_later), Toast.LENGTH_SHORT).show()
                },
                onHideSystemNavigationToggled = { checked ->
                    uiState.hideSystemNavigation = checked
                    Toast.makeText(this, getString(R.string.setup_status_bar_applied_later), Toast.LENGTH_SHORT).show()
                },
                onThemeSelected = { theme -> onSetupThemeSelected(theme) },
                onAccentSelected = { accent -> onSetupAccentSelected(accent) },
                onIntensitySelected = { intensity -> onSetupIntensitySelected(intensity) },
                onLanguageSelected = { language -> onSetupLanguageSelected(language) },
                onAddressBarPositionSelected = { position ->
                    uiState.addressBarPosition = position
                    uiState.scrollHideMode = sanitizeScrollHideMode(uiState.scrollHideMode, position)
                },
                onScrollHideModeSelected = { mode -> uiState.scrollHideMode = mode },
                onEngineSelected = { engine -> uiState.engine = engine },
                onCustomEngineSaved = { name, url ->
                    uiState.customEngineName = name
                    uiState.customEngineUrl = url
                    uiState.engine = "custom"
                },
                onContinueFromWelcome = { uiState.currentPage = 1 },
                onNextFromLayoutPage = { onNextFromLayoutPage() },
                onNextFromEnginePage = { onNextFromEnginePage() }
            )
            com.simplefox.browser.ui.listscreen.ConfirmDialogHost(uiState.confirmDialogConfig, uiState.hideStatusBar, uiState.hideSystemNavigation) { uiState.confirmDialogConfig = null }
            overlayContent?.invoke()
          }
        }
    }

    private fun onNextFromLayoutPage() {
        uiState.currentPage = 3
    }

    private fun onNextFromEnginePage() {
        if (uiState.engine == "google") {
            uiState.confirmDialogConfig = com.simplefox.browser.ui.listscreen.ConfirmDialogConfig(
                title = getString(R.string.google_warning_title),
                message = getString(R.string.google_warning_message),
                positiveLabel = getString(R.string.use_google_anyway),
                onPositive = { saveAndProceed() },
                negativeLabel = getString(R.string.choose_another)
            )
        } else {
            saveAndProceed()
        }
    }

    private fun onSetupThemeSelected(theme: String) {
        if (theme == uiState.theme) return
        uiState.theme = theme
        applyLiveChrome()
    }

    private fun onSetupAccentSelected(accent: String) {
        if (accent == uiState.accent) return
        uiState.accent = accent
        applyLiveChrome()
    }

    private fun onSetupIntensitySelected(intensity: String) {
        if (intensity == uiState.intensity) return
        uiState.intensity = intensity
        applyLiveChrome()
    }

    /** Keeps the system bars and window background in step with the live-themed content. */
    @Suppress("DEPRECATION")
    private fun applyLiveChrome() {
        val chrome = resolveThemeChrome(this, uiState.theme, uiState.accent, uiState.intensity)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(chrome.background))
        window.statusBarColor = chrome.statusBar
        window.navigationBarColor = chrome.navigationBar
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = chrome.lightStatusBar
        controller.isAppearanceLightNavigationBars = chrome.lightNavigationBar
    }

    private fun onSetupLanguageSelected(language: String) {
        if (language == uiState.language) return
        // The language change recreates the activity, which re-reads everything from preferences.
        persistSelections()
        savePendingNavigationState(0)
        captureAndApplyLanguage(language)
    }

    private fun savePendingNavigationState(page: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit()
            .putInt(KEY_PENDING_PAGE, page)
            .putBoolean(KEY_PENDING_CONSENT, uiState.consentChecked)
            .putBoolean(KEY_PENDING_HIDE_STATUS_BAR, uiState.hideStatusBar)
            .putBoolean(KEY_PENDING_HIDE_SYSTEM_NAVIGATION, uiState.hideSystemNavigation)
            .apply()
    }

    /** Writes every choice made so far in one batch; theme choices only reach preferences here. */
    private fun persistSelections(complete: Boolean = false) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit()
            .putString("app_theme", uiState.theme)
            .putString("accent_color", uiState.accent)
            .putString("surface_intensity", uiState.intensity)
            .putString("search_engine", uiState.engine)
            .putString(com.simplefox.browser.browser.CustomSearchEngineNameKey, uiState.customEngineName)
            .putString(com.simplefox.browser.browser.CustomSearchEngineUrlKey, uiState.customEngineUrl)
            .putString("address_bar_position", uiState.addressBarPosition)
            .putString("scroll_hide_mode", uiState.scrollHideMode)
            .putBoolean("hide_status_bar", uiState.hideStatusBar)
            .putBoolean("hide_system_navigation", uiState.hideSystemNavigation)
            .apply { if (complete) putBoolean("setup_complete", true) }
            .commit()
    }

    private fun saveAndProceed() {
        persistSelections(complete = true)
        (application as? ClintApplication)?.applyNightMode()
        startMainActivity()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
