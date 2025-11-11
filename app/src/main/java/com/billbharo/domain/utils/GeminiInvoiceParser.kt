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
 * Handles the second step of the voice-to-invoice process: parsing transcribed text into structured data.
 *
 * This class uses the Google Gemini 2.5 Flash model to extract invoice item details (name, quantity, price)
 * from a raw text string. It is designed to work in a "Pure AI Mode" for development, which means it
 * relies solely on the AI's capabilities without any fallback mechanisms. This mode is intended to
 * test the limits and accuracy of the AI model.
 *
 * **Development Mode Behavior:**
 * - Throws exceptions for all failures to clearly expose limitations.
 * - No fallbacks or confidence filtering.
 *
 * @constructor Creates an instance of the GeminiInvoiceParser.
 */
@Singleton
class GeminiInvoiceParser @Inject constructor() {

    companion object {
        /** Timeout for the Gemini API request (15 seconds). */
        private const val TIMEOUT_MS = 15_000L

        /** The name of the Gemini model used for parsing. */
        private const val MODEL_NAME = "gemini-2.5-flash"
    }

    private val generativeModel: GenerativeModel by lazy {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Fail-fast if the API key is not configured
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
                temperature = 0.2f // Lower temperature for more deterministic, structured output
                topP = 0.95f
                topK = 40
                maxOutputTokens = 512 // Generous token limit for JSON response
            }
        )
    }

    /**
     * Parses a transcribed text string to extract structured invoice item data using the Gemini model.
     *
     * In this "Pure AI Mode," the function will throw an exception if parsing fails for any reason,
     * providing detailed context for debugging.
     *
     * @param transcription The raw text string from the speech-to-text process.
     * @return A [GeminiParseResult] containing the extracted data.
     * @throws IllegalStateException if the Gemini API key is not configured.
     * @throws GeminiParsingException if the AI parsing fails, the request times out, or the response is invalid.
     */
    suspend fun parseInvoiceItem(transcription: String): GeminiParseResult {
        // Trigger the lazy initialization and API key check
        val model = generativeModel

        try {
            return withTimeout(TIMEOUT_MS) {
                val prompt = buildPrompt(transcription)
                val response: GenerateContentResponse = model.generateContent(prompt)
                val responseText = response.text
                    ?: throw GeminiParsingException(
                        "Gemini returned an empty response",
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

    /**
     * Constructs the prompt for the Gemini model, providing clear instructions and examples.
     *
     * @param transcription The user's transcribed voice input.
     * @return A formatted prompt string.
     */
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

    /**
     * Parses the JSON response from the Gemini model.
     *
     * This function cleans the response text, handles potential JSON formatting issues,
     * and extracts the structured data into a [GeminiParseResult].
     *
     * @param responseText The raw response text from the Gemini API.
     * @param originalTranscription The original transcription, for context in error messages.
     * @return A [GeminiParseResult] containing the parsed data.
     * @throws GeminiParsingException if the JSON is invalid or missing required fields.
     */
    private fun parseJsonResponse(responseText: String, originalTranscription: String): GeminiParseResult {
        try {
            // Clean the response to remove markdown code blocks and extract the JSON object
            val cleanJson = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()
                .let {
                    // Extract JSON if it's embedded in other text
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

            // In Pure AI Mode, we require an item name to consider the parse successful
            if (item.isBlank()) {
                throw GeminiParsingException(
                    "Gemini returned an empty item name. Response: $cleanJson",
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
 * Represents the structured result of a Gemini parsing operation.
 *
 * This data class holds all the extracted information from the AI model, including a confidence score
 * and the raw response for debugging purposes.
 *
 * @property itemName The name of the extracted item.
 * @property quantity The quantity of the item.
 * @property price The price of the item.
 * @property confidence The AI's confidence in the accuracy of the parsing (0.0 to 1.0).
 * @property unit The unit of measurement for the item (e.g., "piece", "kg").
 * @property rawResponse The raw JSON response from the Gemini API, useful for debugging.
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
 * Custom exception for handling errors during the Gemini parsing process.
 *
 * This exception includes detailed context, such as the original transcription and the AI's response,
 * to make debugging parsing failures easier.
 *
 * @param message A descriptive error message.
 * @param transcription The original input transcription that caused the failure.
 * @param geminiResponse The raw response from the Gemini API, if available.
 * @param cause The underlying cause of the exception (optional).
 */
class GeminiParsingException(
    message: String,
    val transcription: String,
    val geminiResponse: String? = null,
    cause: Throwable? = null
) : Exception("$message | Input: '$transcription'" + if (geminiResponse != null) " | AI Response: '$geminiResponse'" else "", cause)
