package com.sleepsmart.app.ui.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sleepsmart.app.ui.theme.Navy950
import com.sleepsmart.app.ui.theme.Purple400
import com.sleepsmart.app.ui.theme.TextMuted
import com.sleepsmart.app.ui.theme.TextPrimary

@Composable
fun PermissionsScreen(onComplete: () -> Unit) {
    val ctx = LocalContext.current
    var step by remember { mutableStateOf(0) }
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var notifGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (granted) step = 1
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        step = 2
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .systemBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Welcome to SleepSmart.",
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))

        when {
            !micGranted -> StepCard(
                title = "We listen to ambient sound",
                body = "SleepSmart uses your phone's microphone to detect your sleep stages. " +
                    "Audio is processed in 30-second windows and discarded immediately. " +
                    "Nothing leaves your device.",
                cta = "Allow microphone",
                onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
            !notifGranted -> StepCard(
                title = "Stay informed",
                body = "We'll show one quiet notification while tracking, and the alarm in the morning.",
                cta = "Allow notifications",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notifGranted = true
                        step = 2
                    }
                }
            )
            else -> StepCard(
                title = "Exact alarms",
                body = "On Android 12+, we need permission to schedule the alarm precisely. " +
                    "Tap below to open Settings, toggle the permission on, and come back.",
                cta = "Open settings",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", ctx.packageName, null)
                        }
                        runCatching { ctx.startActivity(intent) }
                    }
                },
                secondaryCta = "I'm done",
                onSecondary = onComplete
            )
        }
    }
}

@Composable
private fun StepCard(
    title: String,
    body: String,
    cta: String,
    onClick: () -> Unit,
    secondaryCta: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = TextMuted)
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Purple400, contentColor = Navy950)
        ) { Text(cta) }
        if (secondaryCta != null && onSecondary != null) {
            TextButton(onClick = onSecondary) { Text(secondaryCta, color = TextMuted) }
        }
    }
}
