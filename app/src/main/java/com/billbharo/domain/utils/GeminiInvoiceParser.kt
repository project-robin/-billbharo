package com.billbharo.domain.utils

import com.billbharo.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PURE AI MODE: Uses ONLY Google Gemini 2.0 Flash for invoice data extraction.
 * No fallbacks, no confidence filtering - purely tests AI capabilities.
 * 
 * Development Mode: Throws exceptions for all failures to expose limitations.
 */
@Singleton
class GeminiInvoiceParser @Inject constructor() {

    companion object {
        private const val TIMEOUT_MS = 15_000L // 15 seconds for thorough processing
        private const val MODEL_NAME = "gemini-2.5-flash" // Gemini 2.5 Flash for structured parsing
    }

    private val generativeModel: GenerativeModel by lazy {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // FAIL-FAST: API key is REQUIRED in pure AI mode
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
            throw IllegalStateException(
                "Gemini API key not configured! Add 'gemini.api.key' to local.properties. " +
                "Get your key from: https://aistudio.google.com/apikey"
            )
        }
        
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.2f // Slightly higher for natural variation
                topP = 0.95f
                topK = 40
                maxOutputTokens = 512 // Increased for detailed responses
            }
        )
    }

    /**
     * PURE AI MODE: Extracts structured invoice item using ONLY Gemini.
     * Throws exceptions for all failures - no fallbacks, no null returns.
     * 
     * @throws IllegalStateException if API key not configured
     * @throws GeminiParsingException if AI parsing fails
     * @throws kotlinx.coroutines.TimeoutCancellationException if request times out
     */
    suspend fun parseInvoiceItem(transcription: String): GeminiParseResult {
        // Trigger API key check (will throw if not configured)
        val model = generativeModel
        
        try {
            return withTimeout(TIMEOUT_MS) {
                val prompt = buildPrompt(transcription)
                val response: GenerateContentResponse = model.generateContent(prompt)
                val responseText = response.text 
                    ?: throw GeminiParsingException(
                        "Gemini returned empty response",
                        transcription = transcription
                    )

                parseJsonResponse(responseText, transcription)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw GeminiParsingException(
                "Gemini request timed out after ${TIMEOUT_MS}ms. Check network connection.",
                transcription = transcription,
                cause = e
            )
        } catch (e: Exception) {
            if (e is GeminiParsingException) throw e
            throw GeminiParsingException(
                "Gemini API error: ${e.message}",
                transcription = transcription,
                cause = e
            )
        }
    }

    private fun buildPrompt(transcription: String): String {
        return """
You are an AI assistant for invoice creation. Extract structured data from voice transcriptions in Hindi, English, or Hinglish.

**Input:** "$transcription"

**Task:** Extract item name, quantity, and price. Handle Hindi/English number words, units (kg, liter, packet), and price expressions (rupay/rupees/rs).

**Output Format (JSON only, no markdown):**
{
  "item": "item_name",
  "quantity": number,
  "price": number,
  "confidence": 0.0_to_1.0,
  "unit": "piece|kg|liter|packet|etc"
}

**Examples:**
- "do bread pachas rupay" → {"item": "Bread", "quantity": 2, "price": 50, "confidence": 0.95, "unit": "piece"}
- "teen kilo aloo sau rupay" → {"item": "Aloo", "quantity": 3, "price": 100, "confidence": 0.92, "unit": "kg"}
- "2 Maggi 20 rupees" → {"item": "Maggi", "quantity": 2, "price": 20, "confidence": 0.98, "unit": "piece"}
- "paanch litre doodh" → {"item": "Doodh", "quantity": 5, "price": 0, "confidence": 0.85, "unit": "liter"}
- "ek packet biscuit das rupay" → {"item": "Biscuit", "quantity": 1, "price": 10, "confidence": 0.90, "unit": "packet"}

**Critical Rules:**
1. ALWAYS return valid JSON (no markdown, no explanations)
2. If price missing → set to 0 (don't reject)
3. If quantity missing → default to 1 (don't reject)
4. Confidence reflects parsing certainty (not rejection threshold)
5. Extract best-effort data even if incomplete
6. Capitalize item names properly
7. Default unit to "piece" if unclear

Extract from: "$transcription"
        """.trimIndent()
    }

    private fun parseJsonResponse(responseText: String, originalTranscription: String): GeminiParseResult {
        try {
            // Clean response (remove markdown code blocks if present)
            val cleanJson = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()
                .let { 
                    // Extract JSON if embedded in text
                    val start = it.indexOf('{')
                    val end = it.lastIndexOf('}') + 1
                    if (start >= 0 && end > start) it.substring(start, end) else it
                }

            val json = JSONObject(cleanJson)
            val item = json.optString("item", "").trim()
            val quantity = json.optDouble("quantity", 1.0)
            val price = json.optDouble("price", 0.0)
            val confidence = json.optDouble("confidence", 0.0)
            val unit = json.optString("unit", "piece").trim()

            // PURE AI MODE: Accept ALL responses, even low confidence
            if (item.isBlank()) {
                throw GeminiParsingException(
                    "Gemini returned empty item name. Response: $cleanJson",
                    transcription = originalTranscription,
                    geminiResponse = cleanJson
                )
            }

            return GeminiParseResult(
                itemName = item,
                quantity = quantity,
                price = price,
                confidence = confidence,
                unit = unit,
                rawResponse = cleanJson
            )
        } catch (e: org.json.JSONException) {
            throw GeminiParsingException(
                "Invalid JSON from Gemini: ${e.message}. Response: $responseText",
                transcription = originalTranscription,
                geminiResponse = responseText,
                cause = e
            )
        } catch (e: Exception) {
            if (e is GeminiParsingException) throw e
            throw GeminiParsingException(
                "JSON parsing error: ${e.message}",
                transcription = originalTranscription,
                geminiResponse = responseText,
                cause = e
            )
        }
    }
}

/**
 * Structured result from Gemini parsing (PURE AI MODE).
 * Returns ALL results regardless of confidence - for testing AI limits.
 */
data class GeminiParseResult(
    val itemName: String,
    val quantity: Double,
    val price: Double,
    val confidence: Double,
    val unit: String = "piece",
    val rawResponse: String = "" // For debugging
)

/**
 * Exception thrown when Gemini parsing fails.
 * Contains detailed context for debugging AI limitations.
 */
class GeminiParsingException(
    message: String,
    val transcription: String,
    val geminiResponse: String? = null,
    cause: Throwable? = null
) : Exception("$message | Input: '$transcription'" + if (geminiResponse != null) " | AI Response: '$geminiResponse'" else "", cause)
