package com.billbharo.data.models

import java.util.Date

/**
 * Represents the domain model for an invoice.
 *
 * This data class is used throughout the application to represent an invoice, decoupled
 * from the database entity.
 *
 * @property id The unique identifier for the invoice.
 * @property invoiceNumber The invoice number.
 * @property customerName The name of the customer (optional).
 * @property customerPhone The phone number of the customer (optional).
 * @property items A list of items included in the invoice.
 * @property subtotal The total amount before taxes.
 * @property cgst The Central Goods and Services Tax amount.
 * @property sgst The State Goods and Services Tax amount.
 * @property totalAmount The final amount including taxes.
 * @property paymentMode The mode of payment.
 * @property isPaid A flag indicating whether the invoice has been paid.
 * @property timestamp The date and time when the invoice was created.
 * @property pdfPath The file path to the generated PDF for the invoice (optional).
 */
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

/**
 * Represents a single item within an invoice in the domain layer.
 *
 * @property name The name of the item.
 * @property quantity The quantity of the item.
 * @property unit The unit of measurement for the item.
 * @property rate The price per unit of the item.
 * @property amount The total amount for this item (quantity * rate).
 * @property hsnCode The Harmonized System of Nomenclature (HSN) code for the item (optional).
 */
data class InvoiceItem(
    val name: String,
    val quantity: Double,
    val unit: String,
    val rate: Double,
    val amount: Double,
    val hsnCode: String? = null
)

/**
 * Represents the possible payment modes for an invoice.
 */
enum class PaymentMode {
    /** Payment made in cash. */
    CASH,

    /** Payment made through UPI. */
    UPI,

    /** Payment to be made later (credit). */
    CREDIT
}
