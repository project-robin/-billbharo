package com.billbharo.domain.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GstCalculator @Inject constructor() {
    
    private val gstRate = 18.0
    
    fun calculateCGST(subtotal: Double): Double {
        val gstAmount = subtotal * gstRate / 100
        return gstAmount / 2
    }
    
    fun calculateSGST(subtotal: Double): Double {
        val gstAmount = subtotal * gstRate / 100
        return gstAmount / 2
    }
    
    fun calculateTotalWithGST(subtotal: Double): Double {
        val gstAmount = subtotal * gstRate / 100
        return subtotal + gstAmount
    }
    
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
    
    fun calculateItemAmount(quantity: Double, rate: Double): Double {
        return quantity * rate
    }
}

data class GstResult(
    val subtotal: Double,
    val cgst: Double,
    val sgst: Double,
    val totalGst: Double,
    val total: Double
)
