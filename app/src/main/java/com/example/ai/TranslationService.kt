package com.example.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Universal Multi-language Translator supporting 50+ languages.
 * Translates from ANY selected source language to ANY target language.
 */
object TranslationService {

    private const val TAG = "TranslationService"

    data class Language(val code: String, val name: String, val nativeName: String)

    val SUPPORTED_LANGUAGES = listOf(
        Language("auto", "Auto Detect", "Detect Language"),
        Language("en", "English", "English"),
        Language("hi", "Hindi", "हिन्दी"),
        Language("ta", "Tamil", "தமிழ்"),
        Language("te", "Telugu", "తెలుగు"),
        Language("kn", "Kannada", "ಕನ್ನಡ"),
        Language("ml", "Malayalam", "മലയാളം"),
        Language("bn", "Bengali", "বাংলা"),
        Language("mr", "Marathi", "मराठी"),
        Language("gu", "Gujarati", "ગુજરાતી"),
        Language("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        Language("ur", "Urdu", "اردو"),
        Language("es", "Spanish", "Español"),
        Language("fr", "French", "Français"),
        Language("de", "German", "Deutsch"),
        Language("it", "Italian", "Italiano"),
        Language("pt", "Portuguese", "Português"),
        Language("ru", "Russian", "Русский"),
        Language("zh-CN", "Chinese (Simplified)", "简体中文"),
        Language("ja", "Japanese", "日本語"),
        Language("ko", "Korean", "한국어"),
        Language("ar", "Arabic", "العربية"),
        Language("tr", "Turkish", "Türkçe"),
        Language("vi", "Vietnamese", "Tiếng Việt"),
        Language("th", "Thai", "ไทย"),
        Language("id", "Indonesian", "Bahasa Indonesia"),
        Language("nl", "Dutch", "Nederlands"),
        Language("pl", "Polish", "Polski"),
        Language("sv", "Swedish", "Svenska"),
        Language("el", "Greek", "Ελληνικά"),
        Language("fa", "Persian", "فارسی"),
        Language("he", "Hebrew", "עברית")
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun translate(
        text: String,
        sourceLangCode: String = "auto",
        targetLangCode: String = "en"
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.success("")
        }

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLangCode&tl=$targetLangCode&dt=t&q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && body.isNotBlank()) {
                val jsonArray = JSONArray(body)
                val sentences = jsonArray.optJSONArray(0)
                if (sentences != null && sentences.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until sentences.length()) {
                        val sentencePair = sentences.optJSONArray(i)
                        val translatedChunk = sentencePair?.optString(0)
                        if (!translatedChunk.isNullOrEmpty()) {
                            sb.append(translatedChunk)
                        }
                    }
                    val finalResult = sb.toString().trim()
                    if (finalResult.isNotEmpty()) {
                        return@withContext Result.success(finalResult)
                    }
                }
            }
            Result.failure(Exception("Unable to translate content. Please check network connection."))
        } catch (e: Exception) {
            Log.e(TAG, "Translation error", e)
            Result.failure(e)
        }
    }
}
