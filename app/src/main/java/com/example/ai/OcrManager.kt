package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device real OCR manager using Google ML Kit Text Recognition.
 * Extracts 100% real text from camera captures and gallery uploads
 * with zero cloud dependencies, working completely offline.
 */
object OcrManager {
    private const val TAG = "OcrManager"
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    data class OcrResult(
        val fullText: String,
        val lines: List<String>,
        val blocks: List<String>
    )

    suspend fun recognizeText(bitmap: Bitmap): Result<OcrResult> = suspendCancellableCoroutine { continuation ->
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText: Text ->
                    val fullText = visionText.text
                    val lines = mutableListOf<String>()
                    val blocks = mutableListOf<String>()

                    for (block in visionText.textBlocks) {
                        blocks.add(block.text)
                        for (line in block.lines) {
                            if (line.text.isNotBlank()) {
                                lines.add(line.text.trim())
                            }
                        }
                    }

                    Log.d(TAG, "OCR Success! Extracted ${lines.size} lines, total chars: ${fullText.length}")
                    continuation.resume(
                        Result.success(
                            OcrResult(
                                fullText = fullText,
                                lines = lines,
                                blocks = blocks
                            )
                        )
                    )
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "OCR recognition error", exception)
                    continuation.resume(Result.failure(exception))
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating OCR process", e)
            continuation.resume(Result.failure(e))
        }
    }
}
