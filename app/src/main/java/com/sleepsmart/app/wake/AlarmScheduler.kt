package com.sleepsmart.app.wake

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    /**
     * Schedule an exact alarm at [fireAt]. Idempotent: a second call replaces
     * the first.
     */
    fun schedule(fireAt: Instant) {
        val am = context.getSystemService<AlarmManager>() ?: return
        val pi = pendingIntent()
        val triggerMs = fireAt.toEpochMilliseconds()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMs, null), pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
        } else {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMs, null), pi)
        }
    }

    fun cancel() {
        val am = context.getSystemService<AlarmManager>() ?: return
        am.cancel(pendingIntent())
    }

    companion object {
        private const val REQUEST_CODE = 4242
    }
}
