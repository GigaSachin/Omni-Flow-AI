package com.errorninjas.omniflow.engine

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import java.io.File

class LlmEngine private constructor(private val context: Context) {
    private var llmInference: LlmInference? = null
    private val _partialResults = MutableSharedFlow<Pair<String, Boolean>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val isReady: Boolean
        get() = llmInference != null

    fun loadModel(modelPath: String): Result<Unit> {
        return try {
            val file = File(modelPath)
            if (!file.exists()) {
                return Result.failure(Exception("Model file not found at: $modelPath"))
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(512)
                .setTemperature(0.3f) // Lower temperature = Less hallucination
                .setTopK(40)
                .setResultListener { result, done ->
                    _partialResults.tryEmit(result to done)
                }
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateStreaming(prompt: String): Flow<String> = flow {
        if (llmInference == null) {
            emit("Error: Model not loaded.")
            return@flow
        }

        try {
            llmInference?.generateResponseAsync(prompt)
            // Use collect with a break condition to ensure the flow completes
            _partialResults.collect { (text, done) ->
                emit(text)
                if (done) {
                    // Breaking the collection loop to finish the flow
                    throw Exception("STREAM_COMPLETE")
                }
            }
        } catch (e: Exception) {
            if (e.message != "STREAM_COMPLETE") {
                emit("Inference Error: ${e.message}")
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LlmEngine? = null

        fun getInstance(context: Context): LlmEngine =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LlmEngine(context.applicationContext).also { INSTANCE = it }
            }
    }
}