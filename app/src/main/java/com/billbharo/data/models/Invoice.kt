package com.billbharo.data.models

import java.util.Date

data class Invoice(
    val id: Long = 0,
    val invoiceNumber: String,
    val customerName: String?,
    val customerPhone: String?,
    val items: List<InvoiceItem>,
    val subtotal: Double,
    val cgst: Double,
    val sgst: Double,
    val totalAmount: Double,
    val paymentMode: PaymentMode,
    val isPaid: Boolean,
    val timestamp: Date,
    val pdfPath: String?
)

data class InvoiceItem(
    val name: String,
    val quantity: Double,
    val unit: String,
    val rate: Double,
    val amount: Double,
    val hsnCode: String? = null
)

enum class PaymentMode {
    CASH, UPI, CREDIT
}
