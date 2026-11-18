package com.simplefox.browser.settings.datasaver

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.simplefox.browser.util.PreferenceManager
import com.simplefox.browser.R
import com.simplefox.browser.base.ClintActivity
import com.simplefox.browser.settings.site.AddSiteDialog
import com.simplefox.browser.settings.site.SiteEntry
import com.simplefox.browser.settings.site.SiteListDeleteConfirmDialog
import com.simplefox.browser.settings.site.SiteListScreen
import com.simplefox.browser.settings.site.SiteListUiState
import com.simplefox.browser.settings.sitepermissions.SitePermissionDatabase
import com.simplefox.browser.settings.sitepermissions.SitePermissionManager
import com.simplefox.browser.ui.rememberMaxContentWidth
import com.simplefox.browser.ui.theme.ClintComposeTheme
import com.simplefox.browser.ui.theme.LocalClintColors

class DataSaverExceptionActivity : ClintActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val type = SitePermissionDatabase.TYPE_DATA_SAVER_EXCEPTION
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

        val listState = SiteListUiState()
        fun reload() {
            listState.allItems = SitePermissionManager.getAllByType(this, type).map { SiteEntry(it.first, it.second, it.third) }
        }
        reload()

        setContent {
            ClintComposeTheme(theme = theme) {
                val colors = LocalClintColors.current
                val maxContentWidth = rememberMaxContentWidth(this)

                Box {
                    SiteListScreen(
                        state = listState,
                        maxContentWidth = maxContentWidth,
                        title = stringResource(R.string.data_saver_title),
                        searchHint = stringResource(R.string.data_saver_exception_search_hint),
                        emptyText = stringResource(R.string.data_saver_exception_no_saved_sites),
                        stateLabel = { _ -> stringResource(R.string.data_saver_exception_state_label) to colors.primary },
                        onExit = { finish() },
                        onAddClick = { listState.addDialogOpen = true },
                        onDeleteClick = { listState.deleteConfirmOpen = true }
                    )

                    if (listState.addDialogOpen) {
                        AddSiteDialog(
                            title = stringResource(R.string.data_saver_exception_add_site),
                            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                            showStateChoice = false,
                            onConfirm = { origin, _ ->
                                DataSaverSiteException.add(this@DataSaverExceptionActivity, origin)
                                reload()
                                listState.addDialogOpen = false
                            },
                            onDismiss = { listState.addDialogOpen = false }
                        )
                    }
                    if (listState.deleteConfirmOpen) {
                        SiteListDeleteConfirmDialog(
                            title = stringResource(R.string.data_saver_exception_delete_title),
                            message = stringResource(R.string.data_saver_exception_delete_message, listState.selectedOrigins.size),
                            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                            onConfirm = {
                                listState.selectedOrigins.forEach { origin -> DataSaverSiteException.remove(this@DataSaverExceptionActivity, origin) }
                                listState.removeSelectedItems()
                                listState.deleteConfirmOpen = false
                                Toast.makeText(this@DataSaverExceptionActivity, getString(R.string.data_saver_exception_items_deleted), Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = { listState.deleteConfirmOpen = false }
                        )
                    }
                }
            }
        }
    }
}
