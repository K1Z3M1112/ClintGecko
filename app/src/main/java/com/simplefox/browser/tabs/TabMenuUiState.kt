package com.simplefox.browser.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TabMenuUiState {
    var selectionMode by mutableStateOf(false)
    var reorderMode by mutableStateOf(false)
    val selectedIds = mutableStateListOf<String>()

    fun beginReorder() {
        if (selectionMode) reorderMode = true
    }

    fun endReorder() {
        reorderMode = false
    }

    fun toggleSelected(tabId: String) {
        if (selectedIds.contains(tabId)) selectedIds.remove(tabId) else selectedIds.add(tabId)
        if (selectedIds.isEmpty()) exitSelectionMode()
    }

    fun enterSelectionMode(tabId: String) {
        reorderMode = false
        selectionMode = true
        if (!selectedIds.contains(tabId)) selectedIds.add(tabId)
    }

    fun selectAll(tabIds: List<String>) {
        selectedIds.clear()
        selectedIds.addAll(tabIds)
    }

    fun invertSelection(tabIds: List<String>) {
        val inverted = tabIds.filterNot { selectedIds.contains(it) }
        selectedIds.clear()
        selectedIds.addAll(inverted)
        if (selectedIds.isEmpty()) exitSelectionMode()
    }

    fun setSelected(tabIds: List<String>, selected: Boolean) {
        if (selected) {
            reorderMode = false
            selectionMode = true
            tabIds.forEach { if (!selectedIds.contains(it)) selectedIds.add(it) }
        } else {
            selectedIds.removeAll(tabIds.toSet())
            if (selectedIds.isEmpty()) exitSelectionMode()
        }
    }

    fun retainExisting(tabIds: Collection<String>) {
        val existing = tabIds.toSet()
        selectedIds.retainAll { it in existing }
    }

    fun exitSelectionMode() {
        reorderMode = false
        selectionMode = false
        selectedIds.clear()
    }
}
