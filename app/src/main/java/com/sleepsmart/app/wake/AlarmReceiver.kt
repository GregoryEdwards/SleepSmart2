package com.sleepsmart.app.wake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleepsmart.app.ui.alarm.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_FIRE) return
        val launch = Intent(context, AlarmActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
            )
        }
        context.startActivity(launch)
    }

    companion object {
        const val ACTION_FIRE = "com.sleepsmart.app.action.ALARM_FIRE"
    }
}
