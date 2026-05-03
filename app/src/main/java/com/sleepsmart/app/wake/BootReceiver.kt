package com.sleepsmart.app.wake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * On boot, ensure no zombie tracking service is left over. The user's
 * upcoming alarms remain scheduled in AlarmManager (system-persistent).
 * v0.1: no rearm logic — this is a placeholder receiver to satisfy the
 * manifest contract.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // intentionally empty for v0.1
    }
}
