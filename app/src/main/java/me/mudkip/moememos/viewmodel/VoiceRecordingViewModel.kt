package me.mudkip.moememos.viewmodel

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.service.AccountService
import me.mudkip.moememos.data.service.MemoService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedTimeMs: Long = 0,
    val transcription: String = "",
    val isTranscribing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class VoiceRecordingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val memoService: MemoService,
    private val accountService: AccountService
) : ViewModel() {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var recordingStartTime: Long = 0
    private var pausedTime: Long = 0

    var recordingState by mutableStateOf(VoiceRecordingState())
        private set

    private val _audioUri = MutableStateFlow<Uri?>(null)
    val audioUri: StateFlow<Uri?> = _audioUri

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    init {
        startTimerUpdates()
    }

    private fun startTimerUpdates() {
        viewModelScope.launch {
            while (true) {
                if (recordingState.isRecording && !recordingState.isPaused) {
                    val elapsedMs = System.currentTimeMillis() - recordingStartTime + pausedTime
                    recordingState = recordingState.copy(elapsedTimeMs = elapsedMs)
                }
                delay(100)
            }
        }
    }

    fun startRecording() {
        try {
            audioFile = File(appContext.cacheDir, "recording_${UUID.randomUUID()}.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(appContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioBitRate(128000)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }

            recordingStartTime = System.currentTimeMillis()
            pausedTime = 0
            recordingState = recordingState.copy(
                isRecording = true,
                isPaused = false,
                elapsedTimeMs = 0,
                errorMessage = null
            )
        } catch (e: Exception) {
            recordingState = recordingState.copy(
                errorMessage = "Failed to start recording: ${e.message}"
            )
        }
    }

    fun pauseRecording() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
            }
            pausedTime += System.currentTimeMillis() - recordingStartTime
            recordingState = recordingState.copy(isPaused = true)
        } catch (e: Exception) {
            recordingState = recordingState.copy(
                errorMessage = "Failed to pause recording: ${e.message}"
            )
        }
    }

    fun resumeRecording() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
            }
            recordingStartTime = System.currentTimeMillis()
            recordingState = recordingState.copy(isPaused = false)
        } catch (e: Exception) {
            recordingState = recordingState.copy(
                errorMessage = "Failed to resume recording: ${e.message}"
            )
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null

            audioFile?.let { file ->
                _audioUri.value = Uri.fromFile(file)
            }

            recordingState = recordingState.copy(
                isRecording = false,
                isPaused = false
            )

            transcribeWithMemosAI()
        } catch (e: Exception) {
            recordingState = recordingState.copy(
                errorMessage = "Failed to stop recording: ${e.message}"
            )
        }
    }

    private fun transcribeWithMemosAI() {
        viewModelScope.launch {
            try {
                recordingState = recordingState.copy(isTranscribing = true)

                audioFile?.let { file ->
                    val account = accountService.currentAccount.first()

                    if (account != null) {
                        val transcribedText = when (account) {
                            is me.mudkip.moememos.data.model.Account.MemosV1 -> {
                                transcribeWithMemosAPI(
                                    account.info.host,
                                    account.info.accessToken,
                                    file,
                                    v0 = false
                                )
                            }
                            is me.mudkip.moememos.data.model.Account.MemosV0 -> {
                                transcribeWithMemosAPI(
                                    account.info.host,
                                    account.info.accessToken,
                                    file,
                                    v0 = true
                                )
                            }
                            else -> {
                                "Transcription unavailable for local account"
                            }
                        }
                        _transcribedText.value = transcribedText
                    } else {
                        _transcribedText.value = "No account selected"
                    }
                }

                recordingState = recordingState.copy(isTranscribing = false)
            } catch (e: Exception) {
                recordingState = recordingState.copy(
                    isTranscribing = false,
                    errorMessage = "Transcription failed: ${e.message}"
                )
                _transcribedText.value = "Transcription failed"
            }
        }
    }

    private suspend fun transcribeWithMemosAPI(
        host: String,
        accessToken: String?,
        audioFile: File,
        v0: Boolean
    ): String {
        return try {
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.getEncoder().encodeToString(audioBytes)

            val requestBody = buildJsonObject {
                put("audio_content", base64Audio)
                put("filename", audioFile.name)
                put("content_type", "audio/mp4")
            }.toString().toRequestBody("application/json".toMediaType())

            val (client, _) = if (v0) {
                accountService.createMemosV0Client(host, accessToken)
            } else {
                accountService.createMemosV1Client(host, accessToken)
            }

            val request = okhttp3.Request.Builder()
                .url("$host/memos.api.AIService/Transcribe")
                .post(requestBody)
                .addHeader("Authorization", "Bearer ${accessToken ?: ""}")
                .addHeader("connect-protocol-version", "1")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.string()?.let { responseBody ->
                    try {
                        val jsonResponse = Json.parseToJsonElement(responseBody)
                        jsonResponse.jsonObject["text"]?.jsonPrimitive?.content
                            ?: "Transcription completed but text is empty"
                    } catch (e: Exception) {
                        responseBody.take(200)
                    }
                } ?: "Empty response from server"
            } else {
                "Transcription failed: ${response.code} ${response.message}"
            }
        } catch (e: Exception) {
            "Error during transcription: ${e.message}"
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            _audioUri.value = null
            _transcribedText.value = ""

            recordingState = VoiceRecordingState()
        } catch (e: Exception) {
            recordingState = recordingState.copy(
                errorMessage = "Failed to cancel recording: ${e.message}"
            )
        }
    }

    suspend fun saveMemo(
        content: String,
        visibility: MemoVisibility,
        accountKey: String
    ): String? {
        return try {
            _isSaving.value = true

            audioFile?.let { file ->
                val repository = memoService.getRepository()

                val memoContent = if (content.isNotBlank()) {
                    "$content\n\n**Транскрибация:**\n${_transcribedText.value}"
                } else {
                    "**Транскрибация:**\n${_transcribedText.value}"
                }

                val memoResponse = repository.createMemo(
                    content = memoContent,
                    visibility = visibility,
                    resources = emptyList()
                )

                memoResponse.getOrNull()?.identifier?.let { memoId ->
                    try {
                        val resourceUri = Uri.fromFile(file)
                        repository.createResource(
                            filename = file.name,
                            type = "audio/mp4".toMediaType(),
                            contentUri = resourceUri,
                            memoIdentifier = memoId
                        )
                    } catch (e: Exception) {
                        recordingState = recordingState.copy(
                            errorMessage = "Memo saved but audio upload failed: ${e.message}"
                        )
                    }

                    memoId
                }
            }
        } finally {
            _isSaving.value = false
            cancelRecording()
        }
    }

    override fun onCleared() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            // Ignore
        }
        super.onCleared()
    }
}
