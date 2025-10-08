package com.billbharo.data.repository

import com.billbharo.data.local.dao.InvoiceDao
import com.billbharo.data.local.entities.InvoiceEntity
import com.billbharo.data.local.entities.InvoiceItemEntity
import com.billbharo.data.models.Invoice
import com.billbharo.data.models.InvoiceItem
import com.billbharo.data.models.PaymentMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao
) {
    fun getAllInvoices(): Flow<List<Invoice>> {
        return invoiceDao.getAllInvoices().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getInvoiceById(id: Long): Invoice? {
        return invoiceDao.getInvoiceById(id)?.toDomainModel()
    }

    suspend fun insertInvoice(invoice: Invoice): Long {
        return invoiceDao.insertInvoice(invoice.toEntity())
    }

    suspend fun updateInvoice(invoice: Invoice) {
        invoiceDao.updateInvoice(invoice.toEntity())
    }

    suspend fun deleteInvoice(invoice: Invoice) {
        invoiceDao.deleteInvoice(invoice.toEntity())
    }

    fun getUnpaidCreditInvoices(): Flow<List<Invoice>> {
        return invoiceDao.getUnpaidCreditInvoices().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getTotalSalesByDateRange(startDate: Date, endDate: Date): Double {
        return invoiceDao.getTotalSalesByDateRange(startDate, endDate) ?: 0.0
    }

    suspend fun generateInvoiceNumber(): String {
        val lastId = invoiceDao.getLastInvoiceId() ?: 0
        val newId = lastId + 1
        return "INV${String.format("%06d", newId)}"
    }

    private fun InvoiceEntity.toDomainModel(): Invoice {
        return Invoice(
            id = id,
            invoiceNumber = invoiceNumber,
            customerName = customerName,
            customerPhone = customerPhone,
            items = items.map { it.toDomainModel() },
            subtotal = subtotal,
            cgst = cgst,
            sgst = sgst,
            totalAmount = totalAmount,
            paymentMode = PaymentMode.valueOf(paymentMode),
            isPaid = isPaid,
            timestamp = timestamp,
            pdfPath = pdfPath
        )
    }

    private fun Invoice.toEntity(): InvoiceEntity {
        return InvoiceEntity(
            id = id,
            invoiceNumber = invoiceNumber,
            customerName = customerName,
            customerPhone = customerPhone,
            items = items.map { it.toEntity() },
            subtotal = subtotal,
            cgst = cgst,
            sgst = sgst,
            totalAmount = totalAmount,
            paymentMode = paymentMode.name,
            isPaid = isPaid,
            timestamp = timestamp,
            pdfPath = pdfPath
        )
    }

    private fun InvoiceItemEntity.toDomainModel(): InvoiceItem {
        return InvoiceItem(
            name = name,
            quantity = quantity,
            unit = unit,
            rate = rate,
            amount = amount,
            hsnCode = hsnCode
        )
    }

    private fun InvoiceItem.toEntity(): InvoiceItemEntity {
        return InvoiceItemEntity(
            name = name,
            quantity = quantity,
            unit = unit,
            rate = rate,
            amount = amount,
            hsnCode = hsnCode
        )
    }
}
