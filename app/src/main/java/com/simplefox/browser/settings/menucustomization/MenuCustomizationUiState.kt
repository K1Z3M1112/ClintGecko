package com.simplefox.browser.settings.menucustomization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.simplefox.browser.browser.menu.CustomizableMenuItem

class MenuCustomizationEntry(val item: CustomizableMenuItem, visible: Boolean) {
    var visible by mutableStateOf(visible)
}

class MenuCustomizationUiState(initialEntries: List<MenuCustomizationEntry>, initialRows: Int = 1) {
    var rows by mutableStateOf(initialRows)
    val entries = mutableStateListOf<MenuCustomizationEntry>().apply { addAll(initialEntries) }

    fun replaceAll(newEntries: List<MenuCustomizationEntry>, newRows: Int = rows) {
        rows = newRows
        entries.clear()
        entries.addAll(newEntries)
    }
}
