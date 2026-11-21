package me.mudkip.moememos.ui.page.voicerecording

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import me.mudkip.moememos.R
import me.mudkip.moememos.ext.string
import me.mudkip.moememos.ui.component.ActionIconButton
import me.mudkip.moememos.viewmodel.VoiceRecordingViewModel
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun VoiceRecordingPage(
    viewModel: VoiceRecordingViewModel = hiltViewModel(),
    onComplete: (String) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current
    val recordingState by remember { derivedStateOf { viewModel.recordingState } }
    val transcribedText by viewModel.transcribedText.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    var waveformAmplitudes by remember { mutableStateOf(listOf<Float>()) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startRecording()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Generate random waveform amplitudes during recording
    LaunchedEffect(recordingState.isRecording, recordingState.isPaused) {
        while (recordingState.isRecording && !recordingState.isPaused) {
            waveformAmplitudes = (0..30).map { Random.nextFloat() * 0.8f + 0.1f }
            delay(50)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = formatRecordingDate(),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {},
                actions = {
                    ActionIconButton(
                        label = R.string.delete.string,
                        onClick = {
                            viewModel.cancelRecording()
                            onCancel()
                        }
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = R.string.delete.string,
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Content card with tabs
            VoiceRecordingCard(
                selectedTab = selectedTab,
                onTabChanged = { selectedTab = it },
                waveformAmplitudes = waveformAmplitudes,
                transcribedText = transcribedText,
                isTranscribing = recordingState.isTranscribing
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recording time
            Text(
                text = formatRecordingTime(recordingState.elapsedTimeMs),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Status indicator
            if (recordingState.isRecording && !recordingState.isPaused) {
                RecordingIndicator()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Control buttons
            RecordingControlButtons(
                isPaused = recordingState.isPaused,
                isRecording = recordingState.isRecording,
                onPauseResume = {
                    if (recordingState.isPaused) {
                        viewModel.resumeRecording()
                    } else {
                        viewModel.pauseRecording()
                    }
                },
                onStop = {
                    viewModel.stopRecording()
                    onComplete(transcribedText)
                }
            )
        }
    }
}

@Composable
private fun VoiceRecordingCard(
    selectedTab: Int,
    onTabChanged: (Int) -> Unit,
    waveformAmplitudes: List<Float>,
    transcribedText: String,
    isTranscribing: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Speech indicator
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = R.string.speech.string,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Content based on selected tab
        if (selectedTab == 0) {
            // Waveform visualization
            Waveform(amplitudes = waveformAmplitudes)
        } else {
            // Transcription display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                if (isTranscribing) {
                    Text(
                        text = "Transcribing…",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Light
                    )
                } else {
                    Text(
                        text = transcribedText.ifEmpty { "No transcription yet" },
                        fontSize = 14.sp,
                        color = if (transcribedText.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab switcher
        TabSwitcher(selectedTab = selectedTab, onTabChanged = onTabChanged)
    }
}

@Composable
private fun Waveform(amplitudes: List<Float>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        amplitudes.forEach { amplitude ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(amplitude)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(horizontal = 1.dp)
            )
        }
    }
}

@Composable
private fun TabSwitcher(selectedTab: Int, onTabChanged: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabButton(
            isSelected = selectedTab == 0,
            onClick = { onTabChanged(0) },
            label = "〰️"
        )
        TabButton(
            isSelected = selectedTab == 1,
            onClick = { onTabChanged(1) },
            label = "📝"
        )
    }
}

@Composable
private fun TabButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Button(
        modifier = Modifier
            .weight(1f)
            .height(36.dp),
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                Color.Transparent,
            contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(label, fontSize = 16.sp)
    }
}

@Composable
private fun RecordingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(600, easing = LinearEasing),
            label = "recording_indicator"
        )

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF6B6B))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Recording",
            fontSize = 12.sp,
            color = Color(0xFFFF6B6B)
        )
    }
}

@Composable
private fun RecordingControlButtons(
    isPaused: Boolean,
    isRecording: Boolean,
    onPauseResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pause/Resume button
        Button(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            onClick = onPauseResume,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(24.dp),
            border = null
        ) {
            Text(
                text = if (isPaused) R.string.resume.string else R.string.pause.string,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Stop button
        Button(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF2B8B5),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = R.string.stop.string,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatRecordingTime(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val centiseconds = (elapsedMs % 1000) / 100
    return String.format("%02d:%02d,%d", minutes, seconds, centiseconds)
}

private fun formatRecordingDate(): String {
    val now = java.time.LocalDateTime.now()
    val monthName = when (now.monthValue) {
        1 -> "янв"
        2 -> "фев"
        3 -> "мар"
        4 -> "апр"
        5 -> "май"
        6 -> "июн"
        7 -> "июл"
        8 -> "авг"
        9 -> "сен"
        10 -> "окт"
        11 -> "ноя"
        else -> "дек"
    }
    return "${now.dayOfMonth} $monthName в ${now.hour}:${String.format("%02d", now.minute)}"
}
