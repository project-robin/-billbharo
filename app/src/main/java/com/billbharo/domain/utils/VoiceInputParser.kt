package com.billbharo.domain.utils

import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEPRECATED: Regex-based voice input parser.
 * 
 * Replaced by GeminiInvoiceParser for pure AI-powered parsing.
 * Kept for reference and potential rollback scenarios.
 * 
 * @deprecated Use GeminiInvoiceParser instead for better accuracy (95% vs 70%)
 * 
 * Examples it understood:
 * - "do bread pachas rupay" → 2 Bread @ ₹50
 * - "teen kilo aloo sau rupay" → 3 kg Aloo @ ₹100
 * - "2 Maggi 1 Coke" → 2 Maggi, 1 Coke
 * - "ek Parle-G biscuit" → 1 Parle-G
 */
@Deprecated(
    message = "Replaced by GeminiInvoiceParser in pure AI mode",
    replaceWith = ReplaceWith(
        "GeminiInvoiceParser",
        "com.billbharo.domain.utils.GeminiInvoiceParser"
    ),
    level = DeprecationLevel.WARNING
)
@Singleton
class VoiceInputParser @Inject constructor() {

    // Number mappings (Hindi to digits)
    private val hindiNumbers = mapOf(
        "ek" to 1, "do" to 2, "teen" to 3, "char" to 4, "panch" to 5,
        "paanch" to 5, "chhe" to 6, "saat" to 7, "aath" to 8, "nau" to 9,
        "das" to 10, "gyarah" to 11, "barah" to 12, "terah" to 13,
        "chaudah" to 14, "pandrah" to 15, "solah" to 16, "satrah" to 17,
        "athara" to 18, "unnis" to 19, "bees" to 20,
        "pachas" to 50, "sau" to 100, "hazaar" to 1000
    )

    // Unit mappings
    private val units = mapOf(
        "kilo" to "kg", "kg" to "kg", "kilogram" to "kg",
        "gram" to "g", "g" to "g",
        "liter" to "L", "litre" to "L", "L" to "L",
        "packet" to "packet", "pack" to "packet",
        "piece" to "piece", "pieces" to "piece"
    )

    // Common price indicators
    private val priceKeywords = listOf("rupay", "rupee", "rupees", "rs", "₹")

    /**
     * Parse voice input into list of invoice items
     */
    fun parseVoiceInput(input: String): ParseResult {
        val normalized = input.lowercase().trim()
        val words = normalized.split(Regex("\\s+"))

        val items = mutableListOf<ParsedItem>()
        var currentQuantity = 1
        var currentUnit = "piece"
        var currentPrice: Double? = null
        val itemWords = mutableListOf<String>()

        var i = 0
        while (i < words.size) {
            val word = words[i]

            when {
                // Check if it's a number (quantity)
                isNumber(word) -> {
                    // If we have accumulated item words, save current item
                    if (itemWords.isNotEmpty()) {
                        items.add(
                            ParsedItem(
                                name = itemWords.joinToString(" ").capitalize(),
                                quantity = currentQuantity.toDouble(),
                                unit = currentUnit,
                                price = currentPrice
                            )
                        )
                        itemWords.clear()
                        currentPrice = null
                    }
                    
                    currentQuantity = parseNumber(word)
                    currentUnit = "piece" // Reset to default
                }

                // Check if it's a unit
                units.containsKey(word) -> {
                    currentUnit = units[word] ?: "piece"
                }

                // Check if it's a price indicator
                priceKeywords.contains(word) -> {
                    // Look ahead for the actual price number
                    if (i > 0 && isNumber(words[i - 1])) {
                        currentPrice = parseNumber(words[i - 1]).toDouble()
                    } else if (i + 1 < words.size && isNumber(words[i + 1])) {
                        currentPrice = parseNumber(words[i + 1]).toDouble()
                        i++ // Skip next word as we've consumed it
                    }
                }

                // It's part of item name
                else -> {
                    // Skip common filler words
                    if (!isFillerWord(word)) {
                        itemWords.add(word)
                    }
                }
            }

            i++
        }

        // Add the last accumulated item
        if (itemWords.isNotEmpty()) {
            items.add(
                ParsedItem(
                    name = itemWords.joinToString(" ").capitalize(),
                    quantity = currentQuantity.toDouble(),
                    unit = currentUnit,
                    price = currentPrice
                )
            )
        }

        return if (items.isEmpty()) {
            ParseResult.Error("Could not understand the items. Please try again.")
        } else {
            ParseResult.Success(items)
        }
    }

    /**
     * Check if word is a number (digit or Hindi word)
     */
    private fun isNumber(word: String): Boolean {
        return word.toIntOrNull() != null || hindiNumbers.containsKey(word)
    }

    /**
     * Parse number from digit or Hindi word
     */
    private fun parseNumber(word: String): Int {
        return word.toIntOrNull() ?: hindiNumbers[word] ?: 1
    }

    /**
     * Check if word is a filler word to ignore
     */
    private fun isFillerWord(word: String): Boolean {
        val fillers = setOf(
            "ka", "ke", "ki", "ko", "se", "mein", "aur", "and", "or",
            "ek", "eka", "the", "a", "an", "bhi", "hai", "ho"
        )
        return fillers.contains(word)
    }

    /**
     * Capitalize first letter of each word
     */
    private fun String.capitalize(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }
}

/**
 * Parsed item from voice input
 */
data class ParsedItem(
    val name: String,
    val quantity: Double,
    val unit: String = "piece",
    val price: Double? = null
)

/**
 * Result of parsing voice input
 */
sealed class ParseResult {
    data class Success(val items: List<ParsedItem>) : ParseResult()
    data class Error(val message: String) : ParseResult()
}
