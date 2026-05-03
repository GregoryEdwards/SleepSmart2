package com.sleepsmart.app.ui.trends

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sleepsmart.app.ui.theme.Navy800
import com.sleepsmart.app.ui.theme.Navy950
import com.sleepsmart.app.ui.theme.Purple400
import com.sleepsmart.app.ui.theme.TextMuted
import com.sleepsmart.app.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrendsScreen(
    onBack: () -> Unit,
    onSessionTap: (String) -> Unit,
    viewModel: TrendsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

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
            Text("Trends", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }

        Spacer(Modifier.height(8.dp))

        if (state.loading) {
            Text(
                "Loading…",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted,
                modifier = Modifier.padding(16.dp)
            )
            return@Column
        }

        if (state.sessions.isEmpty()) {
            EmptyCard()
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                "Avg score",
                state.averageScore?.toString() ?: "—",
                Modifier.weight(1f)
            )
            StatCard(
                "Sleep debt",
                "${state.sleepDebtMinutes / 60}h ${state.sleepDebtMinutes % 60}m",
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            color = Navy800,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(160.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Sparkline(scores = state.sessions.reversed().mapNotNull { it.score })
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Recent nights",
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted
        )
        Spacer(Modifier.height(8.dp))
        state.sessions.forEach { s ->
            SessionRow(s) { onSessionTap(s.id) }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyCard() {
    Surface(
        color = Navy800,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "No nights tracked yet.",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Once you finish a session, it'll show up here with score history and sleep debt.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Navy800,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }
    }
}

@Composable
private fun Sparkline(scores: List<Int>) {
    Canvas(modifier = Modifier.fillMaxSize()) { drawSparkline(scores) }
}

private fun DrawScope.drawSparkline(scores: List<Int>) {
    if (scores.size < 2) return
    val padding = 8f
    val w = size.width - padding * 2
    val h = size.height - padding * 2
    val minS = 0f
    val maxS = 100f
    val stepX = w / (scores.size - 1)

    for (i in 1 until scores.size) {
        val x0 = padding + (i - 1) * stepX
        val x1 = padding + i * stepX
        val y0 = padding + h - ((scores[i - 1] - minS) / (maxS - minS) * h)
        val y1 = padding + h - ((scores[i] - minS) / (maxS - minS) * h)
        drawLine(
            color = Purple400,
            start = Offset(x0, y0),
            end = Offset(x1, y1),
            strokeWidth = 4f
        )
    }
    // Goal line at score 80
    val goalY = padding + h - ((80f - minS) / (maxS - minS) * h)
    drawLine(
        color = Purple400.copy(alpha = 0.3f),
        start = Offset(padding, goalY),
        end = Offset(padding + w, goalY),
        strokeWidth = 1.5f
    )
}

@Composable
private fun SessionRow(
    s: com.sleepsmart.app.domain.GetTrendsUseCase.TrendsSession,
    onTap: () -> Unit
) {
    Surface(
        color = Navy800,
        shape = RoundedCornerShape(14.dp),
        onClick = onTap,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatDate(s.startedAtMs),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (!s.journal.isNullOrBlank()) {
                    Text(
                        s.journal,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    s.score?.toString() ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    color = Purple400
                )
                Text(
                    s.asleepMinutes?.let {
                        "%dh %02dm".format(it / 60, it % 60)
                    } ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
        }
    }
}

private fun formatDate(ms: Long): String =
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(ms))
