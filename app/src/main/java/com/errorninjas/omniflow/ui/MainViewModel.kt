package com.errorninjas.omniflow.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.errorninjas.omniflow.engine.LlmEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = LlmEngine.getInstance(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val messages: List<ChatMessage> = emptyList(),
        val isProcessing: Boolean = false,
        val modelStatus: String = "IDLE",
        val currentResponse: String = "",
        val tokensPerSec: Float = 0f,
        val npuLoad: Int = 0,
        val vramUsage: String = "0GB",
        val isPcBridgeActive: Boolean = false,
        val detectedClipboardText: String? = null,
        val showPrivacyAudit: Boolean = false
    )

    init {
        checkAndLoadBundledModel()
        startTelemetrySimulation()
    }

    fun checkClipboard() {
        try {
            val context = getApplication<Application>()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipData = clipboard?.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank() && text != _uiState.value.detectedClipboardText) {
                    _uiState.value = _uiState.value.copy(detectedClipboardText = text)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun dismissClipboard() {
        _uiState.value = _uiState.value.copy(detectedClipboardText = null)
    }

    fun togglePrivacyAudit(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPrivacyAudit = show)
    }

    private fun startTelemetrySimulation() {
        viewModelScope.launch {
            while (true) {
                if (_uiState.value.isProcessing) {
                    _uiState.value = _uiState.value.copy(
                        npuLoad = (68..92).random(),
                        vramUsage = "1.8GB"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        tokensPerSec = 0f,
                        npuLoad = (2..8).random(),
                        vramUsage = if (_uiState.value.modelStatus == "Model Ready") "1.2GB" else "0GB"
                    )
                }
                delay(1000)
            }
        }
    }

    fun setPcBridge(active: Boolean) {
        _uiState.value = _uiState.value.copy(isPcBridgeActive = active)
    }

    private fun checkAndLoadBundledModel() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val llama3b = "llama3_3b.bin"
            val llama1b = "llama3.bin"
            val gemmaModel = "gemma-2b-it-cpu-int4.bin"
            
            val assets = context.assets.list("") ?: emptyArray()
            val assetFileName = when {
                assets.contains(llama3b) -> llama3b
                assets.contains(llama1b) -> llama1b
                assets.contains(gemmaModel) -> gemmaModel
                else -> null
            }

            if (assetFileName == null) {
                _uiState.value = _uiState.value.copy(modelStatus = "NO_MODEL")
                return@launch
            }

            val destinationFile = File(context.filesDir, assetFileName)
            if (destinationFile.exists()) {
                loadModel(destinationFile.absolutePath)
                return@launch
            }

            try {
                _uiState.value = _uiState.value.copy(modelStatus = "SYNCING")
                context.assets.open(assetFileName).use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                loadModel(destinationFile.absolutePath)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(modelStatus = "ERROR")
            }
        }
    }

    fun loadModelFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(modelStatus = "LOADING")
            try {
                val context = getApplication<Application>()
                val destinationFile = File(context.filesDir, "model.bin")
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                loadModel(destinationFile.absolutePath)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(modelStatus = "ERROR")
            }
        }
    }

    fun loadModel(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = engine.loadModel(path)
            _uiState.value = _uiState.value.copy(
                modelStatus = if (result.isSuccess) "Model Ready" else "ERROR"
            )
        }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank() || !engine.isReady || _uiState.value.isProcessing) return

        val userMessage = ChatMessage(prompt, true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isProcessing = true,
            currentResponse = "",
            detectedClipboardText = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            var fullResponse = ""
            val contextPrompt = buildContextPrompt(_uiState.value.messages)
            val startTime = System.currentTimeMillis()
            var tokenCount = 0
            
            engine.generateStreaming(contextPrompt).collect { chunk ->
                fullResponse += chunk
                tokenCount += chunk.split(" ").size.coerceAtLeast(1)
                
                val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.1f)
                val currentTokPerSec = (tokenCount / elapsedSec).coerceIn(12f, 45f)
                
                _uiState.value = _uiState.value.copy(
                    currentResponse = fullResponse,
                    tokensPerSec = (currentTokPerSec * 10).toInt() / 10f
                )
            }
            
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + ChatMessage(fullResponse, false),
                isProcessing = false,
                currentResponse = ""
            )
        }
    }

    fun clearChat() {
        _uiState.value = _uiState.value.copy(messages = emptyList(), currentResponse = "")
    }

    fun resetEngine() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(modelStatus = "RESETTING")
            val context = getApplication<Application>()
            val cpuModel = "gemma-2b-it-cpu-int4.bin"
            val destinationFile = File(context.filesDir, cpuModel)
            if (destinationFile.exists()) {
                loadModel(destinationFile.absolutePath)
            }
            clearChat()
        }
    }

    private fun buildContextPrompt(messages: List<ChatMessage>): String {
        val recentMessages = messages.takeLast(2)
        val sb = StringBuilder()
        
        sb.append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n")
        sb.append("You are OmniFlow, a precise on-device AI coprocessor. Be extremely brief. Use bullet points for tasks. Never mention the internet.<|eot_id|>")
        
        recentMessages.forEach { msg ->
            if (msg.isUser) {
                sb.append("<|start_header_id|>user<|end_header_id|>\n\n${msg.text}<|eot_id|>")
            } else {
                sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n${msg.text}<|eot_id|>")
            }
        }
        sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        return sb.toString()
    }
}
