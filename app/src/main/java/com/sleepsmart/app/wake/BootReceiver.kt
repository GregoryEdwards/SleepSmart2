package com.sleepsmart.app.wake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleepsmart.app.data.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * On boot, rearm the user's next-night alarm at their saved target wake time.
 * Without this, an alarm set the night before survives in AlarmManager (which
 * is system-persistent), but a fresh app install + reboot before any session
 * would have nothing scheduled. This handles both cases idempotently.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var prefs: PreferencesRepository

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val p = prefs.preferences.first()
                val target = nextTargetWakeAt(p.targetWakeAtMinutesOfDay, Clock.System.now())
                alarmScheduler.schedule(target)
            } catch (_: Throwable) {
                // best-effort; never crash on boot
            } finally {
                pending.finish()
            }
        }
    }

    private fun nextTargetWakeAt(targetMinutes: Int, now: Instant): Instant {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now.toEpochMilliseconds()
            set(java.util.Calendar.HOUR_OF_DAY, targetMinutes / 60)
            set(java.util.Calendar.MINUTE, targetMinutes % 60)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now.toEpochMilliseconds()) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return Instant.fromEpochMilliseconds(cal.timeInMillis)
    }
}
