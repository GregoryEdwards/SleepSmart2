package com.sleepsmart.app.ui.tracking

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sleepsmart.app.tracking.SleepTrackingService
import com.sleepsmart.app.ui.theme.Navy950
import com.sleepsmart.app.ui.theme.Purple400
import com.sleepsmart.app.ui.theme.TextMuted
import com.sleepsmart.app.ui.theme.TextPrimary

@Composable
fun TrackingScreen(
    onSessionEnded: (String) -> Unit,
    viewModel: TrackingViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val state by viewModel.state.collectAsState()
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.finishedSessionId) {
        state.finishedSessionId?.let { onSessionEnded(it) }
    }

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = state.clockText,
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary
        )
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Purple400)
                .alpha(pulseAlpha)
        )

        Spacer(Modifier.height(48.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Stat(label = "epochs", value = state.epochsCaptured.toString())
            Stat(label = "elapsed", value = state.formattedDuration)
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { showConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("End session", color = TextPrimary)
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    val intent = Intent(ctx, SleepTrackingService::class.java).apply {
                        action = SleepTrackingService.ACTION_STOP
                    }
                    ctx.startService(intent)
                }) { Text("End session") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Keep tracking") }
            },
            title = { Text("End session?") },
            text = { Text("Stop tracking and view this session's report.") }
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
    }
}
