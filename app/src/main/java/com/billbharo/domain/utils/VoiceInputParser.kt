package com.billbharo.domain.utils

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A deprecated regex-based parser for voice input.
 *
 * This class was originally used to parse transcribed text into structured invoice items using a
 * series of rules and keyword matching. It has since been replaced by the more accurate and flexible
 * [GeminiInvoiceParser].
 *
 * It is kept in the codebase for reference purposes and as a potential fallback if the AI-based
 * solution is not viable.
 *
 * @deprecated Use [GeminiInvoiceParser] instead, as it provides significantly better accuracy (95% vs 70%).
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

    private val hindiNumbers = mapOf(
        "ek" to 1, "do" to 2, "teen" to 3, "char" to 4, "panch" to 5,
        "paanch" to 5, "chhe" to 6, "saat" to 7, "aath" to 8, "nau" to 9,
        "das" to 10, "gyarah" to 11, "barah" to 12, "terah" to 13,
        "chaudah" to 14, "pandrah" to 15, "solah" to 16, "satrah" to 17,
        "athara" to 18, "unnis" to 19, "bees" to 20,
        "pachas" to 50, "sau" to 100, "hazaar" to 1000
    )

    private val units = mapOf(
        "kilo" to "kg", "kg" to "kg", "kilogram" to "kg",
        "gram" to "g", "g" to "g",
        "liter" to "L", "litre" to "L", "L" to "L",
        "packet" to "packet", "pack" to "packet",
        "piece" to "piece", "pieces" to "piece"
    )

    private val priceKeywords = listOf("rupay", "rupee", "rupees", "rs", "₹")

    /**
     * Parses a raw voice input string into a list of structured [ParsedItem] objects.
     *
     * @param input The transcribed text from the user's voice command.
     * @return A [ParseResult] which is either a [ParseResult.Success] containing a list of items
     *         or a [ParseResult.Error] with an error message.
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
                isNumber(word) -> {
                    if (itemWords.isNotEmpty()) {
                        items.add(
                            ParsedItem(
                                name = itemWords.joinToString(" ").capitalizeWords(),
                                quantity = currentQuantity.toDouble(),
                                unit = currentUnit,
                                price = currentPrice
                            )
                        )
                        itemWords.clear()
                        currentPrice = null
                    }
                    currentQuantity = parseNumber(word)
                    currentUnit = "piece" // Reset to default unit
                }

                units.containsKey(word) -> {
                    currentUnit = units[word] ?: "piece"
                }

                priceKeywords.contains(word) -> {
                    if (i > 0 && isNumber(words[i - 1])) {
                        currentPrice = parseNumber(words[i - 1]).toDouble()
                    } else if (i + 1 < words.size && isNumber(words[i + 1])) {
                        currentPrice = parseNumber(words[i + 1]).toDouble()
                        i++ // Skip the next word as it has been consumed
                    }
                }

                else -> {
                    if (!isFillerWord(word)) {
                        itemWords.add(word)
                    }
                }
            }
            i++
        }

        if (itemWords.isNotEmpty()) {
            items.add(
                ParsedItem(
                    name = itemWords.joinToString(" ").capitalizeWords(),
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
     * Checks if a word represents a number (either a digit or a Hindi number word).
     */
    private fun isNumber(word: String): Boolean {
        return word.toIntOrNull() != null || hindiNumbers.containsKey(word)
    }

    /**
     * Converts a word into its integer representation.
     */
    private fun parseNumber(word: String): Int {
        return word.toIntOrNull() ?: hindiNumbers[word] ?: 1
    }

    /**
     * Checks if a word is a common filler word that should be ignored during parsing.
     */
    private fun isFillerWord(word: String): Boolean {
        val fillers = setOf(
            "ka", "ke", "ki", "ko", "se", "mein", "aur", "and", "or",
            "ek", "eka", "the", "a", "an", "bhi", "hai", "ho"
        )
        return fillers.contains(word)
    }

    /**
     * Capitalizes the first letter of each word in a string.
     */
    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }
}

/**
 * Represents a single item parsed from the voice input.
 *
 * @property name The name of the item.
 * @property quantity The quantity of the item.
 * @property unit The unit of measurement (e.g., "piece", "kg").
 * @property price The price of the item, if specified.
 */
data class ParsedItem(
    val name: String,
    val quantity: Double,
    val unit: String = "piece",
    val price: Double? = null
)

/**
 * Represents the result of a voice input parsing operation.
 */
sealed class ParseResult {
    /**
     * Indicates that the parsing was successful.
     * @property items The list of [ParsedItem] objects extracted from the input.
     */
    data class Success(val items: List<ParsedItem>) : ParseResult()

    /**
     * Indicates that the parsing failed.
     * @property message A descriptive error message.
     */
    data class Error(val message: String) : ParseResult()
}
