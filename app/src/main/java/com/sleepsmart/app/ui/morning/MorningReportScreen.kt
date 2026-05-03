package com.sleepsmart.app.ui.morning

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sleepsmart.app.classifier.SleepStage
import com.sleepsmart.app.ui.theme.Navy800
import com.sleepsmart.app.ui.theme.Navy950
import com.sleepsmart.app.ui.theme.Purple400
import com.sleepsmart.app.ui.theme.StageAwake
import com.sleepsmart.app.ui.theme.StageN1
import com.sleepsmart.app.ui.theme.StageN2
import com.sleepsmart.app.ui.theme.StageN3
import com.sleepsmart.app.ui.theme.StageRem
import com.sleepsmart.app.ui.theme.TextMuted
import com.sleepsmart.app.ui.theme.TextPrimary

@Composable
fun MorningReportScreen(
    sessionId: String,
    onDone: () -> Unit,
    viewModel: MorningReportViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .systemBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Good morning.", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(state.dateLabel, style = MaterialTheme.typography.bodyLarge, color = TextMuted)

        Spacer(Modifier.height(32.dp))

        if (state.epochs.isEmpty()) {
            Text(
                "No data yet — finish a session to see your hypnogram here.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )
        } else {
            Surface(
                color = Navy800,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(220.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Hypnogram(epochs = state.epochs)
                }
            }
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Score", "${state.score}", Modifier.weight(1f))
                StatTile("Asleep", state.timeAsleep, Modifier.weight(1f))
                StatTile("Disturb.", state.disturbances.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            Text(
                state.observation,
                style = MaterialTheme.typography.headlineMedium.copy(fontStyle = FontStyle.Italic),
                color = Purple400
            )

            Spacer(Modifier.height(24.dp))

            JournalCard(
                value = state.journal,
                dirty = state.journalDirty,
                onChange = { viewModel.setJournalDraft(it) },
                onSave = { viewModel.saveJournal() }
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.saveJournal()
                onDone()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple400,
                contentColor = Navy950
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Done", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun JournalCard(
    value: String,
    dirty: Boolean,
    onChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = Navy800,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Journal",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                placeholder = {
                    Text(
                        "How did you sleep? One line is enough.",
                        color = TextMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple400,
                    unfocusedBorderColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Purple400
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (dirty) {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onSave) {
                        Text("Save", color = Purple400)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
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
private fun Hypnogram(epochs: List<HypnogramPoint>) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        drawHypnogram(epochs)
    }
}

private fun DrawScope.drawHypnogram(epochs: List<HypnogramPoint>) {
    if (epochs.isEmpty()) return
    val laneCount = 5
    val laneHeight = size.height / laneCount
    val perEpochW = size.width / epochs.size.coerceAtLeast(1)

    epochs.forEachIndexed { i, p ->
        val laneIndex = stageLaneIndex(p.stage)
        val color = stageColor(p.stage).copy(alpha = if (p.confidence < 0.5f) 0.5f else 1f)
        drawRect(
            color = color,
            topLeft = Offset(i * perEpochW, laneIndex * laneHeight + 4f),
            size = Size(perEpochW.coerceAtLeast(1f), laneHeight - 8f)
        )
    }
}

private fun stageLaneIndex(s: SleepStage) = when (s) {
    SleepStage.AWAKE -> 0
    SleepStage.REM -> 1
    SleepStage.N1 -> 2
    SleepStage.N2 -> 3
    SleepStage.N3 -> 4
}

private fun stageColor(s: SleepStage): Color = when (s) {
    SleepStage.AWAKE -> StageAwake
    SleepStage.REM -> StageRem
    SleepStage.N1 -> StageN1
    SleepStage.N2 -> StageN2
    SleepStage.N3 -> StageN3
}
