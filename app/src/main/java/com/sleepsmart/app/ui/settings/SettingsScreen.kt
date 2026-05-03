package com.sleepsmart.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sleepsmart.app.BuildConfig
import com.sleepsmart.app.ui.theme.Navy800
import com.sleepsmart.app.ui.theme.Navy950
import com.sleepsmart.app.ui.theme.Purple400
import com.sleepsmart.app.ui.theme.TextMuted
import com.sleepsmart.app.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val state by viewModel.state.collectAsState()

    val micGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val notifGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(8.dp))
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }

        Spacer(Modifier.height(8.dp))

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Wake window", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${state.wakeWindowMinutes} minutes",
                    style = MaterialTheme.typography.titleLarge, color = TextPrimary
                )
                Slider(
                    value = state.wakeWindowMinutes.toFloat(),
                    onValueChange = { viewModel.setWakeWindow(it.toInt()) },
                    valueRange = 15f..45f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = Purple400,
                        activeTrackColor = Purple400
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sleep goal", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    "%.1f hours / night".format(state.sleepGoalHours),
                    style = MaterialTheme.typography.titleLarge, color = TextPrimary
                )
                Slider(
                    value = state.sleepGoalHours,
                    onValueChange = { viewModel.setSleepGoal(it) },
                    valueRange = 6f..10f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = Purple400,
                        activeTrackColor = Purple400
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Permissions", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                PermRow("Microphone", micGranted)
                PermRow("Notifications", notifGranted)
            }
        }

        if (BuildConfig.DEMO_MODE_AVAILABLE) {
            Spacer(Modifier.height(12.dp))
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Demo mode",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                "Replays a synthetic 8-hour night in 8 minutes (60× speed).",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = state.demoModeEnabled,
                            onCheckedChange = { viewModel.setDemoMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Purple400,
                                checkedTrackColor = Navy800
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("What we keep", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Audio is processed in 30-second windows on this phone and discarded " +
                        "immediately. Only derived numbers (sleep stages, breathing rate, " +
                        "score) are saved, in an encrypted database. Nothing leaves your device.",
                    style = MaterialTheme.typography.bodyLarge, color = TextPrimary
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Text(
            if (granted) "granted" else "not granted",
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) Purple400 else TextMuted
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        color = Navy800,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}
