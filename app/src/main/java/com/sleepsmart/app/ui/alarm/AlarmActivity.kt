package com.sleepsmart.app.ui.alarm

import android.app.KeyguardManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sleepsmart.app.ui.theme.Navy950
import com.sleepsmart.app.ui.theme.Purple400
import com.sleepsmart.app.ui.theme.SleepSmartTheme
import com.sleepsmart.app.ui.theme.TextMuted
import com.sleepsmart.app.ui.theme.TextPrimary
import com.sleepsmart.app.wake.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject lateinit var scheduler: AlarmScheduler

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) setShowWhenLocked(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) setTurnScreenOn(true)
        getSystemService(KeyguardManager::class.java)?.let { km ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                km.requestDismissKeyguard(this, null)
            }
        }
        startAlarmFx()

        setContent {
            SleepSmartTheme {
                AlarmUi(
                    nowText = nowText(),
                    onSnooze = {
                        scheduler.schedule(Clock.System.now().plus(9.minutes))
                        finish()
                    },
                    onDismiss = {
                        scheduler.cancel()
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        runCatching { ringtone?.stop() }
        runCatching { vibrator?.cancel() }
        super.onDestroy()
    }

    private fun nowText(): String {
        val cal = java.util.Calendar.getInstance()
        val h24 = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        val h12 = if (h24 == 0) 12 else if (h24 > 12) h24 - 12 else h24
        return "%d:%02d".format(h12, m)
    }

    private fun startAlarmFx() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, uri).also { it.play() }
        }
        runCatching {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
            val pattern = longArrayOf(0, 600, 400)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }
}

@androidx.compose.runtime.Composable
private fun AlarmUi(nowText: String, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            "Time to wake.",
            style = MaterialTheme.typography.headlineLarge,
            color = TextMuted
        )
        Spacer(Modifier.height(48.dp))
        Text(
            nowText,
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Smart wake fired at the lightest moment in your window.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.weight(1f).height(72.dp),
                shape = RoundedCornerShape(20.dp)
            ) { Text("Snooze 9m", color = TextPrimary) }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(72.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple400, contentColor = Navy950
                )
            ) { Text("Dismiss", style = MaterialTheme.typography.titleLarge) }
        }
        Spacer(Modifier.height(24.dp))
    }
}
