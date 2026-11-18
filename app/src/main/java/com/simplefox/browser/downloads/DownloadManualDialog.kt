package com.simplefox.browser.downloads

import com.simplefox.browser.R

import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import com.simplefox.browser.ui.foundation.ButtonDefaults
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.simplefox.browser.ui.foundation.CircularProgressIndicator
import com.simplefox.browser.ui.foundation.DropdownMenu
import com.simplefox.browser.ui.foundation.DropdownMenuItem
import com.simplefox.browser.ui.foundation.Icon
import com.simplefox.browser.ui.foundation.Text
import com.simplefox.browser.ui.foundation.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplefox.browser.settings.common.dialogSectionBackground
import com.simplefox.browser.settings.common.SettingsSection
import com.simplefox.browser.settings.downloads.DownloadSettingsKeys
import com.simplefox.browser.ui.ClintDialog
import com.simplefox.browser.ui.ClintScheduleDateTimeRows
import com.simplefox.browser.ui.ClintOutlinedTextField
import com.simplefox.browser.ui.ClintSlider
import com.simplefox.browser.ui.listscreen.ConfirmDialogConfig
import com.simplefox.browser.ui.listscreen.ConfirmDialogHost
import com.simplefox.browser.ui.listscreen.PopupShape
import com.simplefox.browser.ui.theme.LocalClintColors
import com.simplefox.browser.util.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class ManualDownloadSubmission(
    val url: String,
    val filename: String,
    val retryEnabled: Boolean,
    val unmeteredOnly: Boolean,
    val splitParts: Int,
    val multithreadingParts: Int,
    val speedLimitBytesPerSec: Long,
    val locationMode: String,
    val customLocationUri: String?,
    val categorizeEnabled: Boolean,
    val scheduledStartAtMillis: Long,
    val startPaused: Boolean = false
)

@Composable
fun DownloadManualDialog(
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    prefillUrl: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (ManualDownloadSubmission, onDismiss: () -> Unit, onRename: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val activity = context as DownloadsActivity
    val colors = LocalClintColors.current
    val scope = rememberCoroutineScope()
    val prefs = remember { com.simplefox.browser.util.PreferenceManager.getDefaultSharedPreferences(context) }

    var url by remember { mutableStateOf(prefillUrl ?: "") }
    var isFetching by remember { mutableStateOf(false) }
    var isFetched by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var filename by remember { mutableStateOf("") }
    var extension by remember { mutableStateOf("") }
    var fileSizeText by remember { mutableStateOf<String?>(null) }
    var fetchedContentLength by remember { mutableStateOf(-1L) }

    var locationMode by remember {
        mutableStateOf(prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT)
    }
    var customUri by remember {
        mutableStateOf(prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)?.let { Uri.parse(it) })
    }
    var locationMenuOpen by remember { mutableStateOf(false) }
    var categorizeEnabled by remember { mutableStateOf(DownloadCategories.isEnabled(context)) }
    val destinationPreviewText = remember(locationMode, customUri, filename, extension, categorizeEnabled) {
        if (!categorizeEnabled) return@remember null
        val resolvedFilename = if (extension.isNotBlank()) "$filename.$extension" else filename
        val basePath = if (locationMode == DownloadSettingsKeys.MODE_CUSTOM) customUri?.let { uriToDisplayPath(it) } else DEFAULT_DOWNLOAD_PATH
        basePath?.let { it.trimEnd('/') + "/" + DownloadCategories.categorySubpathDisplay(categorizeEnabled, resolvedFilename) + resolvedFilename }
    }

    var retryEnabled by remember { mutableStateOf(prefs.getBoolean(DownloadSettingsKeys.PREF_RETRY_ENABLED, DownloadSettingsKeys.DEFAULT_RETRY_ENABLED)) }
    var unmeteredOnly by remember { mutableStateOf(prefs.getBoolean(DownloadSettingsKeys.PREF_UNMETERED_ONLY, DownloadSettingsKeys.DEFAULT_UNMETERED_ONLY)) }
    var splitParts by remember { mutableStateOf(prefs.getInt(DownloadSettingsKeys.PREF_SPLIT_PARTS, DownloadSettingsKeys.DEFAULT_SPLIT_PARTS).coerceIn(0, 32)) }
    var multithreadingParts by remember { mutableStateOf(prefs.getInt(DownloadSettingsKeys.PREF_MULTITHREADING_PARTS, DownloadSettingsKeys.DEFAULT_MULTITHREADING_PARTS).coerceIn(0, 8)) }

    val initSpeedLimitAmount = remember { prefs.getInt(DownloadSettingsKeys.PREF_SPEED_LIMIT_AMOUNT, DownloadSettingsKeys.DEFAULT_SPEED_LIMIT_AMOUNT) }
    val initSpeedLimitUnit = remember { prefs.getString(DownloadSettingsKeys.PREF_SPEED_LIMIT_UNIT, DEFAULT_SPEED_LIMIT_UNIT) ?: DEFAULT_SPEED_LIMIT_UNIT }
    var speedLimitText by remember { mutableStateOf(if (initSpeedLimitAmount > 0) initSpeedLimitAmount.toString() else "") }
    val kbLabel = stringResource(R.string.speed_limit_unit_kb)
    val mbLabel = stringResource(R.string.speed_limit_unit_mb)
    var speedUnitLabel by remember { mutableStateOf(if (initSpeedLimitUnit == SPEED_LIMIT_UNIT_MB) mbLabel else kbLabel) }
    var speedUnitMenuOpen by remember { mutableStateOf(false) }

    var scheduleEnabled by remember { mutableStateOf(false) }
    var scheduledMillis by remember { mutableStateOf(0L) }
    var blockingError by remember { mutableStateOf<ConfirmDialogConfig?>(null) }

    fun doFetch() {
        val typed = url.trim()
        if (!URLUtil.isValidUrl(typed)) {
            urlError = context.getString(R.string.download_manual_error_invalid_url)
            return
        }
        urlError = null
        isFetching = true
        scope.launch {
            val ua = com.simplefox.browser.browser.webview.ClintGecko.defaultUserAgent()
            val result = withContext(Dispatchers.IO) {
                try {
                    val headRequest = okhttp3.Request.Builder().url(typed).head().header("User-Agent", ua).build()
                    val headResponse = ClintDownloadManager.httpClient.newCall(headRequest).execute()
                    headResponse.use { resp ->
                        if (!resp.isSuccessful && resp.code != 405) return@withContext null
                        val contentDisposition = resp.header("Content-Disposition") ?: ""
                        val contentType = resp.header("Content-Type")?.substringBefore(";")?.trim() ?: ""
                        var contentLength = resp.header("Content-Length")?.toLongOrNull() ?: -1L
                        if (contentLength <= 0L) {
                            try {
                                val rangeRequest = okhttp3.Request.Builder().url(typed).get()
                                    .header("User-Agent", ua).header("Range", "bytes=0-0").build()
                                val rangeResponse = ClintDownloadManager.httpClient.newCall(rangeRequest).execute()
                                contentLength = rangeResponse.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                                    ?: rangeResponse.header("Content-Length")?.toLongOrNull() ?: -1L
                                rangeResponse.close()
                            } catch (_: Exception) { }
                        }
                        resolveFilename(typed, contentDisposition, contentType) to contentLength
                    }
                } catch (_: Exception) { null }
            }
            isFetching = false
            if (result == null) {
                urlError = context.getString(R.string.download_manual_error_network)
                return@launch
            }
            val (resolvedName, contentLength) = result
            isFetched = true
            fetchedContentLength = contentLength
            val dot = resolvedName.lastIndexOf('.')
            filename = if (dot > 0) resolvedName.substring(0, dot) else resolvedName
            extension = if (dot > 0) resolvedName.substring(dot + 1) else ""
            fileSizeText = if (contentLength > 0L) context.getString(R.string.download_dialog_file_size_value, formatFileSize(contentLength))
                else context.getString(R.string.download_dialog_file_size_unknown)
        }
    }

    fun resetFetchState() {
        if (isFetched) {
            isFetched = false
            filename = ""
            extension = ""
            fileSizeText = null
            fetchedContentLength = -1L
        }
    }

    LaunchedEffect(Unit) {
        if (!prefillUrl.isNullOrBlank()) doFetch()
    }

    val compact = remember {
        prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_DIALOG_UI, DownloadSettingsKeys.DEFAULT_DOWNLOAD_DIALOG_UI) == DownloadSettingsKeys.DIALOG_UI_COMPACT
    }
    val footerButtonModifier = if (compact) Modifier.height(36.dp) else Modifier
    val footerButtonPadding = if (compact) PaddingValues(horizontal = 12.dp, vertical = 0.dp) else ButtonDefaults.TextButtonContentPadding
    val footerTextSize = if (compact) 13.sp else androidx.compose.ui.unit.TextUnit.Unspecified

    fun submitManual(startPaused: Boolean) {
        val resolvedFilename = if (extension.isNotBlank()) "$filename.$extension" else filename
        val storageError = checkStorageAvailable(context, fetchedContentLength, locationMode, customUri)
        if (storageError != null) {
            blockingError = ConfirmDialogConfig(
                title = context.getString(R.string.download_error_storage_title),
                message = storageError,
                positiveLabel = context.getString(R.string.action_ok)
            )
            return
        }
        val fat32Error = checkFat32FileSizeLimit(context, fetchedContentLength, locationMode, customUri)
        if (fat32Error != null) {
            blockingError = ConfirmDialogConfig(
                title = context.getString(R.string.download_error_fat32_title),
                message = fat32Error,
                positiveLabel = context.getString(R.string.action_ok)
            )
            return
        }
        val effectiveScheduledMillis = if (scheduleEnabled) scheduledMillis else 0L
        if (!startPaused && effectiveScheduledMillis > 0L && effectiveScheduledMillis <= System.currentTimeMillis()) {
            blockingError = ConfirmDialogConfig(
                title = context.getString(R.string.download_schedule_invalid_title),
                message = context.getString(R.string.download_schedule_past_time_error),
                positiveLabel = context.getString(R.string.action_ok)
            )
            return
        }
        val speedAmount = speedLimitText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val speedUnit = if (speedUnitLabel == mbLabel) SPEED_LIMIT_UNIT_MB else SPEED_LIMIT_UNIT_KB
        val speedLimitBytesPerSec = resolveSpeedLimitBytesPerSec(context, speedAmount, speedUnit)
        val submission = ManualDownloadSubmission(
            url = url.trim(),
            filename = resolvedFilename,
            retryEnabled = retryEnabled,
            unmeteredOnly = unmeteredOnly,
            splitParts = splitParts,
            multithreadingParts = multithreadingParts,
            speedLimitBytesPerSec = speedLimitBytesPerSec,
            locationMode = locationMode,
            customLocationUri = customUri?.toString(),
            categorizeEnabled = categorizeEnabled,
            scheduledStartAtMillis = if (startPaused) 0L else effectiveScheduledMillis,
            startPaused = startPaused
        )
        onSubmit(submission, onDismiss) {}
    }

    ClintDialog(
        title = stringResource(R.string.download_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        compact = compact,
        footer = {
            Row(
                Modifier.fillMaxWidth().padding(
                    start = if (compact) 8.dp else 12.dp,
                    end = if (compact) 8.dp else 12.dp,
                    bottom = if (compact) 0.dp else 4.dp
                ),
                horizontalArrangement = Arrangement.End
            ) {
                val addEnabled = isFetched && !isFetching && url.isNotBlank() && !scheduleEnabled
                TextButton(onClick = { submitManual(startPaused = true) }, enabled = addEnabled, modifier = footerButtonModifier, contentPadding = footerButtonPadding) {
                    Text(
                        stringResource(R.string.action_add),
                        color = if (addEnabled) colors.primary else colors.secondaryText,
                        fontWeight = FontWeight.Medium,
                        fontSize = footerTextSize
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss, modifier = footerButtonModifier, contentPadding = footerButtonPadding) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium, fontSize = footerTextSize)
                }
                TextButton(
                    modifier = footerButtonModifier,
                    contentPadding = footerButtonPadding,
                    onClick = {
                        if (!isFetched) doFetch() else submitManual(startPaused = false)
                    },
                    enabled = url.isNotBlank() && !isFetching
                ) {
                    Text(
                        if (isFetching) stringResource(R.string.download_manual_fetching)
                        else if (isFetched) stringResource(R.string.action_download)
                        else stringResource(R.string.download_manual_fetch),
                        color = colors.primary, fontWeight = FontWeight.Medium, fontSize = footerTextSize
                    )
                }
            }
        }
    ) {
        if (compact) {
            fun pickFolder() {
                activity.launchManualFolderPicker { uri ->
                    prefs.edit().putString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, uri.toString()).apply()
                    customUri = uri
                }
            }
            CompactManualDownloadContent(
                url = url, onUrlChange = { url = it; urlError = null; resetFetchState() },
                urlError = urlError, isFetching = isFetching,
                filename = filename, onFilenameChange = { filename = it },
                extension = extension, onExtensionChange = { extension = it },
                fileSizeText = fileSizeText,
                locationMode = locationMode,
                onLocationModeSelected = {
                    locationMode = it
                    if (it == DownloadSettingsKeys.MODE_CUSTOM) pickFolder()
                },
                customUri = customUri, onPickFolder = { pickFolder() },
                categorizeEnabled = categorizeEnabled, onCategorizeToggle = { categorizeEnabled = !categorizeEnabled },
                destinationPreviewText = destinationPreviewText,
                retryEnabled = retryEnabled, onRetryToggle = { retryEnabled = !retryEnabled },
                unmeteredOnly = unmeteredOnly, onUnmeteredToggle = { unmeteredOnly = !unmeteredOnly },
                splitParts = splitParts, onSplitPartsChange = { splitParts = it },
                multithreadingParts = multithreadingParts, onMultithreadingPartsChange = { multithreadingParts = it },
                speedLimitText = speedLimitText, onSpeedLimitTextChange = { speedLimitText = it },
                speedUnitLabel = speedUnitLabel, kbLabel = kbLabel, mbLabel = mbLabel, onSpeedUnitChange = { speedUnitLabel = it },
                scheduleEnabled = scheduleEnabled,
                onScheduleToggle = {
                    scheduleEnabled = !scheduleEnabled
                    if (scheduleEnabled) {
                        scheduledMillis = Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }.timeInMillis
                        if (needsExactAlarmPermissionRationale(context)) blockingError = exactAlarmPermissionDialogConfig(context)
                    }
                },
                scheduledMillis = scheduledMillis, onScheduledMillisChange = { scheduledMillis = it },
                hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation
            )
        } else {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
            DialogSectionLabel(stringResource(R.string.download_dialog_section_link))
            SettingsSection(colors.dialogSectionBackground, bottomSpacing = 8.dp) {
                Column(Modifier.padding(12.dp)) {
                    ClintOutlinedTextField(
                        value = url,
                        onValueChange = { url = it; urlError = null; resetFetchState() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.download_manual_url_hint)) },
                        singleLine = true,
                        isError = urlError != null,
                        trailingIcon = if (isFetching) { { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.primary, strokeWidth = 2.dp) } } else null,
                        supportingText = urlError?.let { err -> { Text(err, color = colors.colorError) } }
                    )
                }
            }

            DialogSectionLabel(stringResource(R.string.download_dialog_section_file))
            SettingsSection(colors.dialogSectionBackground, bottomSpacing = 8.dp) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        ClintOutlinedTextField(
                            value = filename, onValueChange = { filename = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.download_dialog_filename_hint)) }, singleLine = true
                        )
                        ClintOutlinedTextField(
                            value = extension, onValueChange = { extension = it },
                            modifier = Modifier.width(96.dp).padding(start = 8.dp),
                            label = { Text(stringResource(R.string.download_dialog_extension_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) }, singleLine = true
                        )
                    }
                    fileSizeText?.let { Text(it, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
            }

            DialogSectionLabel(stringResource(R.string.download_dialog_section_location))
            SettingsSection(colors.dialogSectionBackground, bottomSpacing = 8.dp) {
                Column(Modifier.padding(12.dp)) {
                    var locationAnchorWidthPx by remember { mutableStateOf(0) }
                    val density = LocalDensity.current
                    Box(Modifier.onGloballyPositioned { coordinates -> locationAnchorWidthPx = coordinates.size.width }) {
                        Row(
                            Modifier.fillMaxWidth().clickable { locationMenuOpen = true }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.download_location_label), color = colors.secondaryText, fontSize = 11.sp)
                                Text(
                                    if (locationMode == DownloadSettingsKeys.MODE_CUSTOM) stringResource(R.string.download_location_option_custom) else stringResource(R.string.download_location_option_default),
                                    color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Icon(com.simplefox.browser.ui.icons.Icons.Filled.ArrowDownward, contentDescription = null, tint = colors.iconTint)
                        }
                        DropdownMenu(
                            expanded = locationMenuOpen, onDismissRequest = { locationMenuOpen = false },
                            modifier = Modifier.width(with(density) { locationAnchorWidthPx.toDp() }),
                            shape = PopupShape, containerColor = colors.popupBackground, border = null
                        ) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.download_location_option_default), color = colors.onSurface) }, onClick = {
                                locationMode = DownloadSettingsKeys.MODE_DEFAULT; locationMenuOpen = false
                            })
                            DropdownMenuItem(text = { Text(stringResource(R.string.download_location_option_custom), color = colors.onSurface) }, onClick = {
                                locationMode = DownloadSettingsKeys.MODE_CUSTOM; locationMenuOpen = false
                                activity.launchManualFolderPicker { uri ->
                                    prefs.edit().putString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, uri.toString()).apply()
                                    customUri = uri
                                }
                            })
                        }
                    }
                    if (locationMode == DownloadSettingsKeys.MODE_CUSTOM) {
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                activity.launchManualFolderPicker { uri ->
                                    prefs.edit().putString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, uri.toString()).apply()
                                    customUri = uri
                                }
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(com.simplefox.browser.ui.icons.Icons.Filled.Folder, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
                            Text(
                                customUri?.let { uriToDisplayPath(it) } ?: stringResource(R.string.download_location_tap_to_choose),
                                color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }
                    DownloadToggleRow(
                        title = stringResource(R.string.download_categorize_title),
                        summary = stringResource(R.string.download_categorize_summary),
                        checked = categorizeEnabled,
                        onToggle = { categorizeEnabled = !categorizeEnabled },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    destinationPreviewText?.let {
                        Text(
                            it, color = colors.secondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            DialogSectionLabel(stringResource(R.string.download_dialog_section_options))
            SettingsSection(colors.dialogSectionBackground, bottomSpacing = 8.dp) {
                Column(Modifier.padding(12.dp)) {
                    DownloadToggleRow(
                        title = stringResource(R.string.download_retry_enabled_title),
                        summary = stringResource(R.string.download_retry_enabled_summary),
                        checked = retryEnabled,
                        onToggle = { retryEnabled = !retryEnabled },
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    DownloadToggleRow(
                        title = stringResource(R.string.download_unmetered_only_title),
                        summary = stringResource(R.string.download_dialog_unmetered_summary),
                        checked = unmeteredOnly,
                        onToggle = { unmeteredOnly = !unmeteredOnly },
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Text(
                        stringResource(R.string.download_split_parts_title), color = colors.onSurface,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        autoOrCountLabel(splitParts, R.plurals.download_split_parts_value),
                        color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                    ClintSlider(
                        value = autoSliderPosition(splitParts, 32).toFloat(),
                        onValueChange = { splitParts = autoSliderValue(it.toInt(), 32) },
                        valueRange = 1f..33f, steps = 31
                    )

                    Text(
                        stringResource(R.string.download_multithreading_title), color = colors.onSurface,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        autoOrCountLabel(multithreadingParts, R.plurals.download_multithreading_value),
                        color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                    ClintSlider(
                        value = autoSliderPosition(multithreadingParts, 8).toFloat(),
                        onValueChange = { multithreadingParts = autoSliderValue(it.toInt(), 8) },
                        valueRange = 1f..9f, steps = 7
                    )

                    Text(
                        stringResource(R.string.download_dialog_speed_limit_title), color = colors.onSurface,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        stringResource(R.string.download_dialog_speed_limit_desc), color = colors.secondaryText,
                        fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClintOutlinedTextField(
                            value = speedLimitText,
                            onValueChange = { speedLimitText = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.download_dialog_speed_limit_hint)) },
                            singleLine = true
                        )
                        Box(Modifier.padding(start = 8.dp)) {
                            Row(
                                Modifier.clickable { speedUnitMenuOpen = true }.padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(speedUnitLabel, color = colors.onSurface, fontSize = 14.sp)
                                Icon(com.simplefox.browser.ui.icons.Icons.Filled.ArrowDownward, contentDescription = null, tint = colors.iconTint, modifier = Modifier.padding(start = 2.dp))
                            }
                            DropdownMenu(
                                expanded = speedUnitMenuOpen, onDismissRequest = { speedUnitMenuOpen = false },
                                shape = PopupShape, containerColor = colors.popupBackground, border = null,
                                modifier = Modifier.width(120.dp)
                            ) {
                                DropdownMenuItem(text = { Text(kbLabel, color = colors.onSurface) }, onClick = { speedUnitLabel = kbLabel; speedUnitMenuOpen = false })
                                DropdownMenuItem(text = { Text(mbLabel, color = colors.onSurface) }, onClick = { speedUnitLabel = mbLabel; speedUnitMenuOpen = false })
                            }
                        }
                    }

                    val toggleScheduleRow = {
                            scheduleEnabled = !scheduleEnabled
                            if (scheduleEnabled) {
                                scheduledMillis = Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }.timeInMillis
                                if (needsExactAlarmPermissionRationale(context)) blockingError = exactAlarmPermissionDialogConfig(context)
                            }
                    }
                    DownloadToggleRow(
                        title = stringResource(R.string.download_schedule_this_title),
                        summary = stringResource(R.string.download_schedule_this_summary),
                        checked = scheduleEnabled,
                        onToggle = { toggleScheduleRow() },
                        modifier = Modifier.padding(vertical = 6.dp).padding(top = 6.dp)
                    )
                    if (scheduleEnabled) {
                        ClintScheduleDateTimeRows(
                            scheduledMillis = scheduledMillis,
                            onScheduledMillisChange = { scheduledMillis = it },
                            hideStatusBar = hideStatusBar,
                            hideSystemNavigation = hideSystemNavigation
                        )
                    }
                }
            }
        }
        }
    }

    ConfirmDialogHost(blockingError, hideStatusBar, hideSystemNavigation) { blockingError = null }
}

@Composable
private fun DialogSectionLabel(text: String) {
    val colors = LocalClintColors.current
    Text(text, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
}
