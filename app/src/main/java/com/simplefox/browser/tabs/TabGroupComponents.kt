package com.simplefox.browser.tabs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import com.simplefox.browser.ui.foundation.DropdownMenu
import com.simplefox.browser.ui.foundation.Icon
import com.simplefox.browser.ui.foundation.IconButton
import com.simplefox.browser.ui.foundation.Surface
import com.simplefox.browser.ui.foundation.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.graphics.Bitmap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplefox.browser.R
import com.simplefox.browser.ui.ClintCheckbox
import com.simplefox.browser.ui.ClintDialog
import com.simplefox.browser.ui.ClintDialogActionFooter
import com.simplefox.browser.ui.ClintOutlinedTextField
import com.simplefox.browser.ui.listscreen.ListMenuItem
import com.simplefox.browser.ui.listscreen.PopupShape
import com.simplefox.browser.ui.theme.LocalClintColors

object TabGroupPrefs {
    const val PREF_SHOW_GROUP_BAR = "show_tab_group_bar"
    const val PREF_TAB_GROUPS_ENABLED = "tab_groups_enabled"

    fun isEnabled(prefs: android.content.SharedPreferences): Boolean = prefs.getBoolean(PREF_TAB_GROUPS_ENABLED, true)
}

data class TabGroupBarState(
    val group: TabGroup,
    val tabs: List<TabPreview>,
    val activeTabId: String
)

internal const val TAB_GROUP_NAME_MAX_LENGTH = 40

internal fun tabGroupTint(color: Int, isLight: Boolean, strong: Boolean = false): Color =
    Color(color).copy(alpha = if (strong) (if (isLight) 0.26f else 0.34f) else (if (isLight) 0.12f else 0.18f))

@Composable
internal fun tabGroupDisplayName(group: TabGroup): String =
    group.name.ifBlank { stringResource(R.string.tab_group_untitled) }

@Composable
internal fun TabGroupDot(color: Int, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Color(color)))
}

@Composable
private fun TabGroupBarItem(tab: TabPreview, active: Boolean, groupColor: Int, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    val favicon = rememberTabFavicon(tab)
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(34.dp)
            .clip(shape)
            .background(if (active) tabGroupTint(groupColor, colors.isLight, strong = true) else colors.surfaceVariant)
            .border(if (active) 2.dp else 0.dp, if (active) Color(groupColor) else Color.Transparent, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (favicon != null && !tab.isIncognito) {
            Image(remember(favicon) { favicon.asImageBitmap() }, contentDescription = tab.title, modifier = Modifier.size(18.dp))
        } else {
            Icon(
                if (tab.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                contentDescription = tab.title,
                tint = colors.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun TabGroupChipsRow(
    groups: List<TabGroup>,
    counts: Map<String, Int>,
    activeGroupId: String?,
    hoverKey: Any?,
    trackBounds: Boolean,
    onChipClick: (TabGroup) -> Unit,
    onNewGroup: () -> Unit,
    onChipBounds: (String, Rect?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(groups, key = { it.id }) { group ->
            val hovered = hoverKey == groupChipKey(group.id)
            val highlighted = hovered || group.id == activeGroupId
            DisposableEffect(group.id, trackBounds) {
                onDispose { if (trackBounds) onChipBounds(group.id, null) }
            }
            Row(
                Modifier
                    .then(
                        if (trackBounds) Modifier.onGloballyPositioned { onChipBounds(group.id, it.boundsInRoot()) }
                        else Modifier
                    )
                    .widthIn(max = 200.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(tabGroupTint(group.color, colors.isLight, strong = hovered))
                    .border(1.5.dp, if (highlighted) Color(group.color) else Color.Transparent, RoundedCornerShape(18.dp))
                    .clickable { onChipClick(group) }
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabGroupDot(group.color, 10.dp)
                Text(
                    tabGroupDisplayName(group),
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 7.dp).weight(1f, fill = false)
                )
                Text(
                    (counts[group.id] ?: 0).toString(),
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 7.dp)
                )
            }
        }
        item(key = "chip:new") {
            Row(
                Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceVariant)
                    .border(1.dp, colors.divider, RoundedCornerShape(18.dp))
                    .clickable(onClick = onNewGroup)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.tab_group_new),
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun TabGroupHeader(
    group: TabGroup,
    tabs: List<TabPreview>,
    containsActive: Boolean,
    highlighted: Boolean,
    selectionMode: Boolean,
    allSelected: Boolean,
    onToggleCollapsed: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
    onNewTab: () -> Unit,
    onEdit: () -> Unit,
    onSelectTabs: () -> Unit,
    onUngroup: () -> Unit,
    onCloseGroup: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier? = null,
    disableLongPressSelection: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    val colors = LocalClintColors.current
    val dragHandle = dragHandleModifier
    val shape: Shape = if (group.collapsed) RoundedCornerShape(18.dp) else RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(tabGroupTint(group.color, colors.isLight, strong = highlighted))
            .then(dragHandle ?: Modifier)
            .border(if (highlighted) 2.dp else 0.dp, if (highlighted) Color(group.color) else Color.Transparent, shape)
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else if (onBack != null) onBack() else onToggleCollapsed() },
                onLongClick = if (!disableLongPressSelection) ({ if (!selectionMode) onLongPress() }) else null
            )
            .padding(start = if (dragHandle != null) 6.dp else 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dragHandle != null) {
            Icon(
                Icons.Filled.DragIndicator,
                contentDescription = stringResource(R.string.tab_menu_drag_handle_desc),
                tint = colors.iconTint,
                modifier = Modifier.padding(end = 6.dp).size(24.dp).padding(2.dp)
            )
        }
        if (selectionMode) {
            ClintCheckbox(checked = allSelected, onCheckedChange = { onToggleSelection() }, modifier = Modifier.size(20.dp))
        } else if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.tab_group_back), tint = colors.iconTint, modifier = Modifier.size(22.dp))
            }
            TabGroupDot(group.color, 14.dp, Modifier.padding(start = 4.dp))
        } else {
            TabGroupDot(group.color, 14.dp)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                tabGroupDisplayName(group),
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pluralStringResource(R.plurals.tab_menu_tab_count, tabs.size, tabs.size),
                    color = colors.secondaryText,
                    fontSize = 12.sp
                )
                if (containsActive) {
                    Box(Modifier.padding(horizontal = 6.dp).size(3.dp).clip(CircleShape).background(colors.secondaryText))
                    Text(
                        stringResource(R.string.tab_group_current),
                        color = Color(group.color),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (group.collapsed) {
            TabGroupFaviconStack(tabs.take(4), modifier = Modifier.padding(start = 8.dp))
        }
        if (!selectionMode) {
            IconButton(onClick = onNewTab, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tab_group_new_tab_in_group), tint = colors.iconTint, modifier = Modifier.size(20.dp))
            }
        }
        if (onBack == null) {
            Icon(
                if (group.collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = stringResource(if (group.collapsed) R.string.tab_group_expand else R.string.tab_group_collapse),
                tint = colors.iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        if (!selectionMode) {
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.tab_group_options_desc), tint = colors.iconTint, modifier = Modifier.size(20.dp))
                }
                TabGroupMenu(
                    expanded = menuExpanded,
                    collapsed = group.collapsed,
                    showCollapse = onBack == null,
                    onDismiss = { menuExpanded = false },
                    onNewTab = onNewTab,
                    onEdit = onEdit,
                    onToggleCollapsed = onToggleCollapsed,
                    onSelectTabs = onSelectTabs,
                    onUngroup = onUngroup,
                    onCloseGroup = onCloseGroup
                )
            }
        } else {
            Spacer(Modifier.width(10.dp))
        }
    }
}

/** Chrome-style group tile for the grid view: a 2x2 preview of the group's tabs; tapping opens the whole group. */
@Composable
internal fun TabGroupCard(
    group: TabGroup,
    tabs: List<TabPreview>,
    containsActive: Boolean,
    highlighted: Boolean,
    selectionMode: Boolean,
    allSelected: Boolean,
    thumbnailFor: (String) -> Bitmap?,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onNewTab: () -> Unit,
    onEdit: () -> Unit,
    onSelectTabs: () -> Unit,
    onUngroup: () -> Unit,
    onCloseGroup: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier? = null
) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(18.dp)
    val accent = Color(group.color)
    val emphasized = highlighted || allSelected || containsActive
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(shape)
            .background(tabGroupTint(group.color, colors.isLight, strong = true))
            .border(if (emphasized) 2.dp else 0.8.dp, if (emphasized) accent else colors.divider.copy(alpha = 0.55f), shape)
            .then(dragHandleModifier ?: Modifier)
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else onOpen() },
                onLongClick = if (!selectionMode) onLongPress else null
            )
            .padding(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                ClintCheckbox(checked = allSelected, onCheckedChange = { onToggleSelection() }, modifier = Modifier.size(20.dp))
            } else {
                TabGroupDot(group.color, 12.dp)
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    tabGroupDisplayName(group),
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    pluralStringResource(R.plurals.tab_menu_tab_count, tabs.size, tabs.size),
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            if (!selectionMode) {
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.tab_group_options_desc), tint = colors.iconTint, modifier = Modifier.size(18.dp))
                    }
                    TabGroupMenu(
                        expanded = menuExpanded,
                        collapsed = false,
                        showCollapse = false,
                        onDismiss = { menuExpanded = false },
                        onNewTab = onNewTab,
                        onEdit = onEdit,
                        onToggleCollapsed = {},
                        onSelectTabs = onSelectTabs,
                        onUngroup = onUngroup,
                        onCloseGroup = onCloseGroup
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0..1) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (col in 0..1) {
                        val index = row * 2 + col
                        val tab = tabs.getOrNull(index)
                        val overflow = if (index == 3 && tabs.size > 4) tabs.size - 3 else 0
                        TabGroupCardTile(tab, tab?.let { thumbnailFor(it.id) }, overflow, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun TabGroupCardTile(tab: TabPreview?, thumbnail: Bitmap?, overflow: Int, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val tileShape = RoundedCornerShape(10.dp)
    Box(
        modifier.clip(tileShape).background(if (tab != null) colors.cardBackground else colors.cardBackground.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        if (tab != null) {
            if (thumbnail != null && !tab.isIncognito) {
                Image(
                    remember(thumbnail) { thumbnail.asImageBitmap() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val favicon = rememberTabFavicon(tab)
                if (favicon != null && !tab.isIncognito) {
                    Image(remember(favicon) { favicon.asImageBitmap() }, contentDescription = null, modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        if (tab.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (overflow > 0) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                    Text("+$overflow", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TabGroupFaviconStack(tabs: List<TabPreview>, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    if (tabs.isEmpty()) return
    val step = 14.dp
    val itemSize = 24.dp
    Box(modifier.width(itemSize + step * (tabs.size - 1)).height(itemSize)) {
        tabs.forEachIndexed { index, tab ->
            val favicon = rememberTabFavicon(tab)
            Box(
                Modifier
                    .offset(x = step * index)
                    .size(itemSize)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.5.dp, colors.surface, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (favicon != null && !tab.isIncognito) {
                    Image(remember(favicon) { favicon.asImageBitmap() }, contentDescription = null, modifier = Modifier.size(16.dp))
                } else {
                    Icon(
                        if (tab.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabGroupMenu(
    expanded: Boolean,
    collapsed: Boolean,
    showCollapse: Boolean = true,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onEdit: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onSelectTabs: () -> Unit,
    onUngroup: () -> Unit,
    onCloseGroup: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = null,
        shadowElevation = 0.dp
    ) {
        ListMenuItem(Icons.Filled.Add, stringResource(R.string.tab_group_new_tab_in_group), checked = false) { onDismiss(); onNewTab() }
        ListMenuItem(Icons.Filled.Edit, stringResource(R.string.tab_group_edit), checked = false) { onDismiss(); onEdit() }
        if (showCollapse) ListMenuItem(
            if (collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
            stringResource(if (collapsed) R.string.tab_group_expand else R.string.tab_group_collapse),
            checked = false
        ) { onDismiss(); onToggleCollapsed() }
        ListMenuItem(Icons.Filled.Checklist, stringResource(R.string.tab_group_select_tabs), checked = false) { onDismiss(); onSelectTabs() }
        ListMenuItem(Icons.Filled.LinkOff, stringResource(R.string.tab_group_ungroup), checked = false) { onDismiss(); onUngroup() }
        ListMenuItem(Icons.Filled.Close, stringResource(R.string.tab_group_close), checked = false) { onDismiss(); onCloseGroup() }
    }
}

@Composable
internal fun TabSelectionActionBar(
    hasSelection: Boolean,
    canUngroup: Boolean,
    groupsEnabled: Boolean,
    onGroup: () -> Unit,
    onMove: () -> Unit,
    onUngroup: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
        shadowElevation = 0.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (groupsEnabled) {
                SelectionAction(Icons.Filled.CreateNewFolder, stringResource(R.string.tab_selection_group), hasSelection, onGroup)
                SelectionAction(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.tab_selection_move), hasSelection, onMove)
                SelectionAction(Icons.Filled.LinkOff, stringResource(R.string.tab_group_ungroup), hasSelection && canUngroup, onUngroup)
            }
            SelectionAction(Icons.Filled.Close, stringResource(R.string.action_close), hasSelection, onClose)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SelectionAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.38f)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(
            label,
            color = colors.onSurface,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
internal fun TabGroupEditorDialog(
    title: String,
    positiveLabel: String,
    initialName: String,
    initialColor: Int,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    val colors = LocalClintColors.current
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableIntStateOf(initialColor) }
    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = positiveLabel,
                onPositive = { onConfirm(name.trim(), color) }
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(tabGroupTint(color, colors.isLight))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabGroupDot(color, 12.dp)
                Text(
                    name.trim().ifBlank { stringResource(R.string.tab_group_untitled) },
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            ClintOutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= TAB_GROUP_NAME_MAX_LENGTH) name = it },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                label = { Text(stringResource(R.string.tab_group_name_label)) },
                singleLine = true
            )
            Text(
                stringResource(R.string.tab_group_color_label),
                color = colors.secondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 10.dp)
            )
            com.simplefox.browser.ui.ClintColorSwatchGrid(
                presets = TabGroupColor.entries.map { it.argb },
                labels = TabGroupColor.entries.map { stringResource(it.labelRes) },
                selected = color,
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onSelect = { color = it }
            )
        }
    }
}

@Composable
internal fun TabGroupPickerDialog(
    groups: List<TabGroup>,
    counts: Map<String, Int>,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onPick: (String) -> Unit,
    onNewGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.tab_group_move_title),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss
    ) {
        groups.forEach { group ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPick(group.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabGroupDot(group.color, 14.dp)
                Text(
                    tabGroupDisplayName(group),
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 14.dp)
                )
                if (group.isIncognito) {
                    Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.padding(start = 8.dp).size(16.dp))
                }
                Text(
                    pluralStringResource(R.plurals.tab_menu_tab_count, counts[group.id] ?: 0, counts[group.id] ?: 0),
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onNewGroup)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.tab_group_new),
                color = colors.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
