package com.billbharo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.billbharo.data.local.converters.Converters
import java.util.Date

/**
 * Represents an invoice in the database.
 *
 * This data class defines the schema for the `invoices` table.
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
 * @property paymentMode The mode of payment (e.g., CASH, UPI, CREDIT).
 * @property isPaid A flag indicating whether the invoice has been paid.
 * @property timestamp The date and time when the invoice was created.
 * @property pdfPath The file path to the generated PDF for the invoice (optional).
 */
@Entity(tableName = "invoices")
@TypeConverters(Converters::class)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerName: String?,
    val customerPhone: String?,
    val items: List<InvoiceItemEntity>,
    val subtotal: Double,
    val cgst: Double,
    val sgst: Double,
    val totalAmount: Double,
    val paymentMode: String, // CASH, UPI, CREDIT
    val isPaid: Boolean,
    val timestamp: Date,
    val pdfPath: String?
)

/**
 * Represents a single item within an invoice.
 *
 * This data class is used to store the details of each item in the `items` list
 * of an [InvoiceEntity].
 *
 * @property name The name of the item.
 * @property quantity The quantity of the item.
 * @property unit The unit of measurement for the item (e.g., kg, piece, liter).
 * @property rate The price per unit of the item.
 * @property amount The total amount for this item (quantity * rate).
 * @property hsnCode The Harmonized System of Nomenclature (HSN) code for the item (optional).
 */
data class InvoiceItemEntity(
    val name: String,
    val quantity: Double,
    val unit: String, // kg, piece, liter, etc.
    val rate: Double,
    val amount: Double,
    val hsnCode: String? = null
)
