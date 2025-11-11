package com.billbharo.domain.utils

import javax.inject.Inject
import javax.inject.Singleton

/**
 * A utility class for calculating Goods and Services Tax (GST).
 *
 * This class provides methods to calculate Central GST (CGST), State GST (SGST),
 * and the total amount including GST, based on a default or specified tax rate.
 */
@Singleton
class GstCalculator @Inject constructor() {

    private val gstRate = 18.0 // Default GST rate of 18%

    /**
     * Calculates the Central GST (CGST) amount for a given subtotal.
     *
     * CGST is half of the total GST amount.
     *
     * @param subtotal The amount before tax.
     * @return The calculated CGST amount.
     */
    fun calculateCGST(subtotal: Double): Double {
        val gstAmount = subtotal * gstRate / 100
        return gstAmount / 2
    }

    /**
     * Calculates the State GST (SGST) amount for a given subtotal.
     *
     * SGST is half of the total GST amount.
     *
     * @param subtotal The amount before tax.
     * @return The calculated SGST amount.
     */
    fun calculateSGST(subtotal: Double): Double {
        val gstAmount = subtotal * gstRate / 100
        return gstAmount / 2
    }

    /**
     * Calculates the total amount including GST for a given subtotal.
     *
     * @param subtotal The amount before tax.
     * @return The total amount including GST.
     */
    fun calculateTotalWithGST(subtotal: Double): Double {
        val gstAmount = subtotal * gstRate / 100
        return subtotal + gstAmount
    }

    /**
     * Calculates all GST components for a given subtotal and returns them in a [GstResult] object.
     *
     * @param subtotal The amount before tax.
     * @param rate The GST rate to apply (defaults to the class's default rate).
     * @return A [GstResult] object containing the detailed tax breakdown.
     */
    fun calculateGst(subtotal: Double, rate: Double = gstRate): GstResult {
        val gstAmount = subtotal * rate / 100
        val cgst = gstAmount / 2
        val sgst = gstAmount / 2
        val total = subtotal + gstAmount

        return GstResult(
            subtotal = subtotal,
            cgst = cgst,
            sgst = sgst,
            totalGst = gstAmount,
            total = total
        )
    }

    /**
     * Calculates the total amount for an item based on its quantity and rate.
     *
     * @param quantity The quantity of the item.
     * @param rate The price per unit of the item.
     * @return The total amount (quantity * rate).
     */
    fun calculateItemAmount(quantity: Double, rate: Double): Double {
        return quantity * rate
    }
}

/**
 * A data class to hold the results of a GST calculation.
 *
 * @property subtotal The initial amount before tax.
 * @property cgst The calculated Central GST amount.
 * @property sgst The calculated State GST amount.
 * @property totalGst The total GST amount (CGST + SGST).
 * @property total The final amount including all taxes.
 */
data class GstResult(
    val subtotal: Double,
    val cgst: Double,
    val sgst: Double,
    val totalGst: Double,
    val total: Double
)
