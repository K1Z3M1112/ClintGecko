package com.simplefox.browser.downloads

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplefox.browser.util.PreferenceManager
import com.simplefox.browser.settings.downloads.DownloadSettingsKeys
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

internal object DownloadScheduleMonitor {

    private const val REQUEST_CODE = 79_999

    internal val scheduleWaitingIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    fun isEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(
            DownloadSettingsKeys.PREF_SCHEDULE_ENABLED, DownloadSettingsKeys.DEFAULT_SCHEDULE_ENABLED
        )
    }

    private fun windowMinutes(context: Context): Pair<Int, Int> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val start = prefs.getInt(
            DownloadSettingsKeys.PREF_SCHEDULE_START_MINUTES, DownloadSettingsKeys.DEFAULT_SCHEDULE_START_MINUTES
        )
        val end = prefs.getInt(
            DownloadSettingsKeys.PREF_SCHEDULE_END_MINUTES, DownloadSettingsKeys.DEFAULT_SCHEDULE_END_MINUTES
        )
        return start to end
    }

    fun isWithinWindow(context: Context): Boolean {
        if (!isEnabled(context)) return true
        val (start, end) = windowMinutes(context)
        if (start == end) return true
        val now = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        return if (start < end) now in start until end else now >= start || now < end
    }

    private fun millisUntilNextBoundary(context: Context): Long {
        val (start, end) = windowMinutes(context)
        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val boundary = if (isWithinWindow(context)) end else start
        var deltaMinutes = boundary - nowMinutes
        if (deltaMinutes <= 0) deltaMinutes += 24 * 60
        val secondsIntoMinute = cal.get(Calendar.SECOND)
        return (deltaMinutes * 60_000L - secondsIntoMinute * 1000L).coerceAtLeast(1_000L)
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, DownloadScheduleReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun scheduleNextCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = pendingIntent(context)
        val (start, end) = windowMinutes(context)
        if (!isEnabled(context) || start == end) {
            alarmManager.cancel(pendingIntent)
            return
        }
        val triggerAt = System.currentTimeMillis() + millisUntilNextBoundary(context)
        if (DownloadCustomScheduleMonitor.canScheduleExact(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun reconcile(context: Context) {
        if (isWithinWindow(context)) {
            val toResume = ClintDownloadManager.downloadsFlow.value
                .filter { it.status == DownloadStatus.PAUSED && it.waitingForSchedule }
                .map { it.id }
            scheduleWaitingIds.clear()
            toResume.forEach { ClintDownloadManager.resume(context, it) }
            ClintDownloadManager.tryDequeueNext(context)
        } else {
            val toPause = ClintDownloadManager.downloadsFlow.value.filter {
                (it.status == DownloadStatus.QUEUED || it.status in DownloadStatus.ACTIVELY_WORKING) &&
                    it.scheduledStartAtMillis == 0L
            }
            toPause.forEach { item ->
                ClintDownloadManager.updateItem(item.id) { it.copy(waitingForSchedule = true) }
                scheduleWaitingIds.add(item.id)
                ClintDownloadManager.pause(context, item.id)
            }
        }
        scheduleNextCheck(context)
    }

    fun onScheduleChanged(context: Context) {
        reconcile(context)
    }
}

class DownloadScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        ClintDownloadManager.init(appContext).invokeOnCompletion {
            try {
                DownloadScheduleMonitor.reconcile(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
