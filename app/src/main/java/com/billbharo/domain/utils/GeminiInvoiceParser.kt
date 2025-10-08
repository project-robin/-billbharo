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
 * Uses Google Gemini 2.5 Flash to extract structured invoice data from voice transcription.
 * Falls back to regex parser if Gemini fails or returns low-confidence results.
 */
@Singleton
class GeminiInvoiceParser @Inject constructor() {

    companion object {
        private const val TIMEOUT_MS = 10_000L // 10 seconds timeout
        private const val CONFIDENCE_THRESHOLD = 0.85 // Only auto-fill if ≥ 85% confident
        private const val MODEL_NAME = "gemini-2.0-flash-exp" // Latest fast model
    }

    private val generativeModel: GenerativeModel? by lazy {
        val apiKey = BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && it != "YOUR_GEMINI_API_KEY_HERE" }
        apiKey?.let {
            GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = it,
                generationConfig = generationConfig {
                    temperature = 0.1f // Low temperature for deterministic output
                    topP = 0.95f
                    topK = 40
                    maxOutputTokens = 256 // Small output for efficiency
                }
            )
        }
    }

    /**
     * Extracts structured invoice item from voice transcription using Gemini.
     * Returns null if parsing fails, confidence is low, or API is unavailable.
     */
    suspend fun parseInvoiceItem(transcription: String): GeminiParseResult? {
        if (generativeModel == null) {
            return null // API key not configured, fallback to regex parser
        }

        return try {
            withTimeout(TIMEOUT_MS) {
                val prompt = buildPrompt(transcription)
                val response: GenerateContentResponse = generativeModel!!.generateContent(prompt)
                val responseText = response.text ?: return@withTimeout null

                parseJsonResponse(responseText)
            }
        } catch (e: Exception) {
            // Network timeout, API error, JSON parsing error → fallback
            null
        }
    }

    private fun buildPrompt(transcription: String): String {
        return """
You are an AI assistant that extracts structured invoice data from voice transcriptions in Hindi, English, or Hinglish.

**Input:** "$transcription"

**Task:** Extract the item name, quantity, and price from the transcription. Handle variations in number formats, units, and price expressions.

**Output:** Return ONLY a valid JSON object (no markdown, no explanations) in this EXACT format:
```json
{
  "item": "item_name_here",
  "quantity": numeric_quantity_here,
  "price": numeric_price_here,
  "confidence": confidence_score_0_to_1
}
```

**Examples:**
- Input: "do bread pachas rupay" → {"item": "Bread", "quantity": 2, "price": 50, "confidence": 0.95}
- Input: "teen kilo aloo sau rupay" → {"item": "Aloo", "quantity": 3, "price": 100, "confidence": 0.90}
- Input: "2 Maggi 20 rupees" → {"item": "Maggi", "quantity": 2, "price": 20, "confidence": 0.98}
- Input: "ek packet biscuit" → {"item": "Biscuit", "quantity": 1, "price": 0, "confidence": 0.70}

**Rules:**
1. If price is missing, set it to 0 and reduce confidence.
2. If quantity is ambiguous, default to 1 and reduce confidence.
3. Confidence should reflect certainty (0.0 = no confidence, 1.0 = fully confident).
4. Return ONLY the JSON object, nothing else.

Now extract from: "$transcription"
        """.trimIndent()
    }

    private fun parseJsonResponse(responseText: String): GeminiParseResult? {
        return try {
            // Clean response (remove markdown code blocks if present)
            val cleanJson = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(cleanJson)
            val item = json.optString("item", "").trim()
            val quantity = json.optDouble("quantity", 1.0)
            val price = json.optDouble("price", 0.0)
            val confidence = json.optDouble("confidence", 0.0)

            if (item.isBlank() || confidence < CONFIDENCE_THRESHOLD) {
                return null // Low confidence or invalid data
            }

            GeminiParseResult(
                itemName = item,
                quantity = quantity,
                price = price,
                confidence = confidence
            )
        } catch (e: Exception) {
            null // JSON parsing failed
        }
    }
}

/**
 * Structured result from Gemini parsing.
 * Only returned if confidence ≥ CONFIDENCE_THRESHOLD.
 */
data class GeminiParseResult(
    val itemName: String,
    val quantity: Double,
    val price: Double,
    val confidence: Double
)
