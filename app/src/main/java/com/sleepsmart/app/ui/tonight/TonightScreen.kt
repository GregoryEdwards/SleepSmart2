package com.sleepsmart.app.ui.tonight

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sleepsmart.app.tracking.SleepTrackingService
import com.sleepsmart.app.ui.theme.Navy800
import com.sleepsmart.app.ui.theme.Navy950
import com.sleepsmart.app.ui.theme.Purple400
import com.sleepsmart.app.ui.theme.TextMuted
import com.sleepsmart.app.ui.theme.TextPrimary

@Composable
fun TonightScreen(
    onTrackingStarted: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTrends: () -> Unit,
    onMissingPermissions: () -> Unit,
    onSessionFinished: (String) -> Unit,
    viewModel: TonightViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.lastFinishedSessionId) {
        state.lastFinishedSessionId?.let {
            viewModel.consumeLastFinished()
            onSessionFinished(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onOpenTrends) {
                Icon(
                    Icons.Outlined.Timeline,
                    contentDescription = "Trends",
                    tint = TextMuted
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = TextMuted
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        Text(
            text = "Tonight",
            style = MaterialTheme.typography.headlineLarge,
            color = TextMuted
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.formattedTargetWakeTime,
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Normal
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "wake time",
            style = MaterialTheme.typography.bodyLarge,
            color = TextMuted
        )

        Spacer(Modifier.height(48.dp))

        Surface(
            color = Navy800,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Wake window",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${state.wakeWindowMinutes} minutes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = state.wakeWindowMinutes.toFloat(),
                    onValueChange = { viewModel.setWakeWindow(it.toInt()) },
                    valueRange = 15f..45f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = Purple400,
                        activeTrackColor = Purple400,
                        inactiveTrackColor = Navy950
                    )
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val micOk = ContextCompat.checkSelfPermission(
                        ctx, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    val notifOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            ctx, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    if (!micOk || !notifOk) {
                        onMissingPermissions()
                        return@Button
                    }
                    val intent = Intent(ctx, SleepTrackingService::class.java).apply {
                        action = SleepTrackingService.ACTION_START
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ctx.startForegroundService(intent)
                    } else {
                        ctx.startService(intent)
                    }
                    onTrackingStarted()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple400,
                    contentColor = Navy950
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Text("Begin tracking", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Audio is processed locally and never leaves your device.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}
