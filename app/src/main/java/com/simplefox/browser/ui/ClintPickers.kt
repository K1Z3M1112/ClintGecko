package com.simplefox.browser.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplefox.browser.R
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar

@Composable
fun ClintDatePickerDialog(
    currentMillis: Long,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    title: String? = null,
    onDismiss: () -> Unit,
    onPicked: (year: Int, month: Int, dayOfMonth: Int) -> Unit
) {
    val initial = remember(currentMillis) {
        Calendar.getInstance().apply { timeInMillis = currentMillis }.let {
            LocalDate.of(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
    }
    var selected by remember(initial) { mutableStateOf(initial) }
    var displayed by remember(initial) { mutableStateOf(YearMonth.from(initial)) }
    ClintPickerDialogShell(
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        onConfirm = {
            onPicked(selected.year, selected.monthValue - 1, selected.dayOfMonth)
            onDismiss()
        }
    ) { _ ->
        PickerTitle(title ?: stringResource(R.string.download_schedule_date_picker_title))
        ClintCalendar(
            displayedMonth = displayed,
            onDisplayedMonthChange = { displayed = it },
            selectedDate = selected,
            weekSelection = false,
            onDateSelected = { selected = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
    }
}

@Composable
fun ClintTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onPicked: (hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    var hour by remember(initialHour, initialMinute) { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember(initialHour, initialMinute) { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    ClintPickerDialogShell(
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        onConfirm = {
            onPicked(hour, minute)
            onDismiss()
        }
    ) { _ ->
        PickerTitle(title)
        Box(
            Modifier.fillMaxWidth().height(248.dp).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            ClintTimeWheels(
                hour = hour,
                minute = minute,
                is24Hour = is24Hour,
                onTimeChange = { h, m -> hour = h.coerceIn(0, 23); minute = m.coerceIn(0, 59) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
