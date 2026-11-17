package com.simplefox.browser.browser

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import com.simplefox.browser.ui.foundation.DropdownMenu
import com.simplefox.browser.ui.foundation.HorizontalDivider
import com.simplefox.browser.ui.foundation.Icon
import com.simplefox.browser.ui.foundation.IconButton
import com.simplefox.browser.ui.foundation.Surface
import com.simplefox.browser.ui.foundation.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplefox.browser.R
import com.simplefox.browser.ui.listscreen.ListMenuItem
import com.simplefox.browser.ui.listscreen.PopupShape
import com.simplefox.browser.ui.rememberClintFavicon
import com.simplefox.browser.ui.theme.LocalClintColors
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

private val SuggestionContentMaxWidth = 720.dp

private data class SuggestionSection(val type: SuggestionType, val items: List<SuggestionItem>)

@OptIn(FlowPreview::class)
@Composable
internal fun SearchOverlay(
    initialText: String,
    isBottom: Boolean,
    hint: String,
    suggestions: List<SuggestionItem>,
    statusBarPaddingPx: Int,
    navBarPaddingPx: Int = 0,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onClose: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onSuggestionDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    incognitoKeyboard: Boolean = false
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val fieldState = remember { TextFieldState(initialText, TextRange(0, initialText.length)) }
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)

    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }.drop(1).collect { currentOnQueryChange(it) }
    }

    LaunchedEffect(Unit) {
        // The overlay's first composition and the keyboard animation used to start in the same frame.
        repeat(2) { withFrameNanos { } }
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun fill(text: String) {
        fieldState.setTextAndPlaceCursorAtEnd(text)
    }

    // The highlight only needs to follow the text once typing pauses; reading the field text in the list
    // recomposed every suggestion row on each keystroke, on top of the recomposition when results arrive.
    var highlightQuery by remember { mutableStateOf(initialText.trim()) }
    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString().trim() }.debounce(90).collect { highlightQuery = it }
    }

    // Remembered, stable callbacks let unchanged rows skip recomposition.
    val currentOnSuggestionClick by rememberUpdatedState(onSuggestionClick)
    val currentOnSuggestionDelete by rememberUpdatedState(onSuggestionDelete)
    val onRowClick = remember { { item: SuggestionItem -> currentOnSuggestionClick(item.query) } }
    val onRowFill = remember { { item: SuggestionItem -> fieldState.setTextAndPlaceCursorAtEnd(item.query) } }
    val onRowDelete = remember { { item: SuggestionItem -> currentOnSuggestionDelete(item.query) } }

    val withoutAutoSpaceAfterPeriod = remember {
        InputTransformation {
            val newText = asCharSequence()
            val cursor = selection.end
            val insertedAutoSpace = length == originalText.length + 1 &&
                selection.collapsed &&
                cursor == length &&
                cursor >= 2 &&
                newText[cursor - 1] == ' ' &&
                newText[cursor - 2] == '.' &&
                (cursor < 3 || newText[cursor - 3] != ' ') &&
                newText.startsWith(originalText)
            if (insertedAutoSpace) delete(cursor - 1, cursor)
        }
    }

    val sections = remember(suggestions) {
        SuggestionType.entries.mapNotNull { type ->
            suggestions.filter { it.type == type }.takeIf { it.isNotEmpty() }?.let { SuggestionSection(type, it) }
        }
    }

    val entries = remember(sections, isBottom) { buildEntries(sections, isBottom) }

    val hideKeyboardOnDrag = remember(keyboardController) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) keyboardController?.hide()
                return Offset.Zero
            }
        }
    }

    val fieldRow = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = SuggestionContentMaxWidth)
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = colors.iconTint
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.addressBarColor)
                    .border(1.5.dp, colors.primary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                    .padding(start = 16.dp, end = 4.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (fieldState.text.isEmpty()) {
                        Text(
                            text = hint,
                            color = colors.secondaryText,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    com.simplefox.browser.privacy.IncognitoKeyboardScope(enabled = incognitoKeyboard) {
                        BasicTextField(
                            state = fieldState,
                            inputTransformation = withoutAutoSpaceAfterPeriod,
                            lineLimits = TextFieldLineLimits.SingleLine,
                            textStyle = TextStyle(color = colors.onSurface, fontSize = 16.sp),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                            onKeyboardAction = { onSubmit(fieldState.text.toString().trim()) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    }
                }
                if (fieldState.text.isNotEmpty()) {
                    FieldIconButton(Icons.Filled.Close, stringResource(R.string.action_clear_search)) { fill("") }
                } else {
                    FieldIconButton(Icons.Filled.Mic, stringResource(R.string.voice_search), onVoiceSearch)
                }
            }
        }
    }

    val suggestionsList = @Composable {
        LazyColumn(
            reverseLayout = isBottom,
            verticalArrangement = if (isBottom) Arrangement.Bottom else Arrangement.Top,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().nestedScroll(hideKeyboardOnDrag)
        ) {
            items(entries, key = { it.key }, contentType = { it::class }) { entry ->
                when (entry) {
                    is SuggestionEntry.Header -> SuggestionSectionTitle(entry.type)
                    is SuggestionEntry.Row -> SuggestionRowCard(
                        entry = entry,
                        query = highlightQuery,
                        isBottom = isBottom,
                        onClick = onRowClick,
                        onFill = onRowFill,
                        onDelete = onRowDelete
                    )
                }
            }
        }
    }

    // Track the keyboard's final height, not its per-frame height. The target changes only when the keyboard starts
    // to open or close, so the list is re-measured twice instead of on every animation frame.
    @OptIn(ExperimentalLayoutApi::class)
    val imeInsets = WindowInsets.imeAnimationTarget
    Surface(color = colors.background, modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = with(density) { statusBarPaddingPx.toDp() })
                // Read in the layout phase: only this node is re-measured, never the whole overlay recomposed.
                .layout { measurable, constraints ->
                    val bottom = maxOf(imeInsets.getBottom(this), navBarPaddingPx)
                    val placeable = measurable.measure(constraints.offset(vertical = -bottom))
                    layout(
                        placeable.width,
                        (placeable.height + bottom).coerceIn(constraints.minHeight, constraints.maxHeight)
                    ) { placeable.place(0, 0) }
                }
        ) {
            if (isBottom) {
                Box(modifier = Modifier.weight(1f, fill = true), contentAlignment = Alignment.BottomCenter) { suggestionsList() }
                fieldRow()
            } else {
                fieldRow()
                Box(modifier = Modifier.weight(1f, fill = true), contentAlignment = Alignment.TopCenter) { suggestionsList() }
            }
        }
    }
}

@Composable
private fun FieldIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = colors.secondaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

private sealed interface SuggestionEntry {
    val key: String

    data class Header(val type: SuggestionType) : SuggestionEntry {
        override val key: String get() = "hdr:${type.name}"
    }

    /** [displayIndex] is the row's position from the top of its card, [count] the rows in that card. */
    data class Row(val item: SuggestionItem, val displayIndex: Int, val count: Int, override val key: String) : SuggestionEntry
}

private fun buildEntries(sections: List<SuggestionSection>, isBottom: Boolean): List<SuggestionEntry> {
    val out = ArrayList<SuggestionEntry>()
    val seen = HashMap<String, Int>()
    sections.forEach { section ->
        val n = section.items.size
        val rows = section.items.mapIndexed { index, item ->
            val base = "row:${item.type.name}:${item.query}"
            val occurrence = (seen[base] ?: 0) + 1
            seen[base] = occurrence
            SuggestionEntry.Row(item, if (isBottom) n - 1 - index else index, n, if (occurrence == 1) base else "$base#$occurrence")
        }
        // A reversed list is laid out bottom-up, so the title has to come after its rows there.
        if (isBottom) {
            out.addAll(rows)
            out.add(SuggestionEntry.Header(section.type))
        } else {
            out.add(SuggestionEntry.Header(section.type))
            out.addAll(rows)
        }
    }
    return out
}

@Composable
private fun SuggestionSectionTitle(type: SuggestionType) {
    val colors = LocalClintColors.current
    val title = stringResource(
        when (type) {
            SuggestionType.CLINT_PAGE -> R.string.suggestion_section_clint
            SuggestionType.BOOKMARK -> R.string.bookmarks_title
            SuggestionType.HISTORY -> R.string.history_title
            SuggestionType.SUGGESTION -> R.string.suggestion_section_search
        }
    )
    Text(
        text = title,
        color = colors.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = SuggestionContentMaxWidth)
            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp)
    )
}

@Composable
private fun SuggestionRowCard(
    entry: SuggestionEntry.Row,
    query: String,
    isBottom: Boolean,
    onClick: (SuggestionItem) -> Unit,
    onFill: (SuggestionItem) -> Unit,
    onDelete: (SuggestionItem) -> Unit
) {
    val colors = LocalClintColors.current
    val first = entry.displayIndex == 0
    val last = entry.displayIndex == entry.count - 1
    val shape = remember(first, last) {
        RoundedCornerShape(
            topStart = if (first) 18.dp else 0.dp,
            topEnd = if (first) 18.dp else 0.dp,
            bottomStart = if (last) 18.dp else 0.dp,
            bottomEnd = if (last) 18.dp else 0.dp
        )
    }
    val dividerColor = remember(colors.divider) { colors.divider.copy(alpha = 0.5f) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = SuggestionContentMaxWidth)
            .padding(bottom = if (last) 6.dp else 0.dp)
            .clip(shape)
            .background(colors.cardBackground)
    ) {
        if (!first) {
            HorizontalDivider(
                color = dividerColor,
                modifier = Modifier.padding(start = 64.dp, end = 16.dp)
            )
        }
        SuggestionRow(
            item = entry.item,
            query = query,
            isBottom = isBottom,
            onClick = onClick,
            onFill = onFill,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionRow(
    item: SuggestionItem,
    query: String,
    isBottom: Boolean,
    onClick: (SuggestionItem) -> Unit,
    onFill: (SuggestionItem) -> Unit,
    onDelete: (SuggestionItem) -> Unit
) {
    val colors = LocalClintColors.current
    val isUrl = item.query.startsWith("http://") || item.query.startsWith("https://")
    val isClintPage = item.type == SuggestionType.CLINT_PAGE
    val readableUrl = remember(item.query, isClintPage) { if (isClintPage) item.query else readableUrl(item.query) }
    val hasTitle = (isUrl || isClintPage) && item.displayText != item.query
    val titleText = if (isUrl && !hasTitle) readableUrl else item.displayText
    val favicon = if (isUrl) rememberClintFavicon(item.query) else null
    val suggestionUrlDesc = stringResource(R.string.suggestion_url_desc)
    val removeLabel = stringResource(R.string.history_delete_desc)
    var menuOpen by remember { mutableStateOf(false) }
    val canRemove = item.type == SuggestionType.HISTORY

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .combinedClickable(
                    onClick = { onClick(item) },
                    onLongClick = if (canRemove) ({ menuOpen = true }) else null,
                    onLongClickLabel = if (canRemove) removeLabel else null
                )
                .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (favicon != null) {
                    val faviconImage = remember(favicon) { favicon.asImageBitmap() }
                    Image(
                        bitmap = faviconImage,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                    )
                } else {
                    val fallbackIcon = when {
                        isClintPage -> Icons.Filled.SportsEsports
                        item.type == SuggestionType.BOOKMARK -> Icons.Filled.Bookmark
                        item.type == SuggestionType.HISTORY && !isUrl -> Icons.Filled.History
                        isUrl -> Icons.Filled.Public
                        else -> Icons.Filled.Search
                    }
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = stringResource(R.string.suggestion_icon_desc),
                        tint = if (item.type == SuggestionType.BOOKMARK || isClintPage) colors.primary else colors.iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f).padding(start = 14.dp, end = 4.dp)) {
                Text(
                    text = highlightMatch(titleText, query),
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasTitle) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = readableUrl,
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { contentDescription = suggestionUrlDesc }
                    )
                }
            }

            IconButton(onClick = { onFill(item) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.suggestion_fill_desc),
                    tint = colors.secondaryText,
                    modifier = Modifier.size(20.dp).rotate(if (isBottom) -45f else 45f)
                )
            }
        }

        if (canRemove) {
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = PopupShape,
                containerColor = colors.popupBackground,
                border = null
            ) {
                ListMenuItem(Icons.Filled.DeleteOutline, removeLabel, checked = false) {
                    menuOpen = false
                    onDelete(item)
                }
            }
        }
    }
}

private fun readableUrl(url: String): String =
    url.removePrefix("https://").removePrefix("http://").removePrefix("www.").removeSuffix("/")

@Composable
private fun highlightMatch(text: String, query: String): AnnotatedString {
    val colors = LocalClintColors.current
    return remember(text, query, colors.onSurface) {
        val start = if (query.isEmpty()) -1 else text.indexOf(query, ignoreCase = true)
        buildAnnotatedString {
            if (start < 0) {
                append(text)
            } else {
                append(text.substring(0, start))
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.onSurface)) {
                    append(text.substring(start, start + query.length))
                }
                append(text.substring(start + query.length))
            }
        }
    }
}
