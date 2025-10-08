package com.billbharo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.billbharo.data.local.converters.Converters
import java.util.Date

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

data class InvoiceItemEntity(
    val name: String,
    val quantity: Double,
    val unit: String, // kg, piece, liter, etc.
    val rate: Double,
    val amount: Double,
    val hsnCode: String? = null
)
