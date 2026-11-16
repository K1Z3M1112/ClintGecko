package com.simplefox.browser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplefox.browser.R
import com.simplefox.browser.ui.foundation.Text
import com.simplefox.browser.ui.theme.LocalClintColors
import java.util.Calendar
import java.util.Date

/**
 * Two tappable rows (Date / Time) used by the "Schedule This Download" option.
 * Opens [ClintDatePickerDialog] / [ClintTimePickerDialog] and reports the new timestamp.
 */
@Composable
fun ClintScheduleDateTimeRows(
    scheduledMillis: Long,
    onScheduledMillisChange: (Long) -> Unit,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val colors = LocalClintColors.current
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val effectiveMillis = if (scheduledMillis > 0L) scheduledMillis else System.currentTimeMillis()
    val date = Date(effectiveMillis)
    val dateText = android.text.format.DateFormat.getMediumDateFormat(context).format(date)
    val timeText = android.text.format.DateFormat.getTimeFormat(context).format(date)

    val titleSize = if (compact) 13.sp else 14.sp
    val valueSize = if (compact) 12.sp else 13.sp
    val rowMinHeight = if (compact) 32.dp else 40.dp

    Column(Modifier.fillMaxWidth().padding(top = if (compact) 2.dp else 4.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = rowMinHeight).clickable { showDate = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.download_schedule_date_title),
                color = colors.onSurface, fontSize = titleSize, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(dateText, color = colors.primary, fontSize = valueSize, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = rowMinHeight).clickable { showTime = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.download_schedule_time_title),
                color = colors.onSurface, fontSize = titleSize, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(timeText, color = colors.primary, fontSize = valueSize, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        }
    }

    if (showDate) {
        ClintDatePickerDialog(
            currentMillis = effectiveMillis,
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { showDate = false }
        ) { year, month, day ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = effectiveMillis
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onScheduledMillisChange(cal.timeInMillis)
        }
    }

    if (showTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = effectiveMillis }
        ClintTimePickerDialog(
            title = stringResource(R.string.download_schedule_time_picker_title),
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { showTime = false }
        ) { hour, minute ->
            val updated = Calendar.getInstance().apply {
                timeInMillis = effectiveMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onScheduledMillisChange(updated.timeInMillis)
        }
    }
}
