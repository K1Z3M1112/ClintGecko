package com.simplefox.browser.settings.shortcutmanager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.simplefox.browser.shortcuts.WebAppShortcut
import com.simplefox.browser.ui.listscreen.ConfirmDialogConfig
import com.simplefox.browser.ui.listscreen.ListSortKey
import com.simplefox.browser.ui.listscreen.ListSortOrder

class ShortcutManagerUiState {
    var shortcuts by mutableStateOf<List<WebAppShortcut>>(emptyList())
    var iconRevision by mutableStateOf(0)
    var confirmDialog by mutableStateOf<ConfirmDialogConfig?>(null)

    var searchQuery by mutableStateOf("")
    var isSearchMode by mutableStateOf(false)
    var sortKey by mutableStateOf(ListSortKey.DATE_ADDED)
    var sortOrder by mutableStateOf(ListSortOrder.DESCENDING)
    var sortMenuOpen by mutableStateOf(false)

    var selectedIds by mutableStateOf<Set<String>>(emptySet())
    var isInSelectionMode by mutableStateOf(false)
    var selectionOptionsMenuOpen by mutableStateOf(false)
    var deleteConfirm by mutableStateOf<ConfirmDialogConfig?>(null)

    val selectedCount get() = selectedIds.size

    fun toggleSelection(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun enterSelectionWith(id: String) {
        isInSelectionMode = true
        selectedIds = selectedIds + id
    }

    fun selectAll(displayed: List<WebAppShortcut>) {
        selectedIds = selectedIds + displayed.map { it.id }
    }

    fun invertSelection(displayed: List<WebAppShortcut>) {
        val displayedIds = displayed.map { it.id }.toSet()
        selectedIds = (selectedIds - displayedIds) + (displayedIds - selectedIds)
    }

    fun deselectAll() {
        selectedIds = emptySet()
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedIds = emptySet()
    }

    fun closeSearch() {
        isSearchMode = false
        searchQuery = ""
    }
}
