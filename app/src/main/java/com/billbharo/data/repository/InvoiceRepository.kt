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

/**
 * Repository for managing invoice-related data.
 *
 * This class provides a clean API for accessing and modifying invoice data, abstracting
 * the underlying data source (in this case, [InvoiceDao]).
 *
 * @property invoiceDao The DAO for accessing invoice data.
 */
@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao
) {
    /**
     * Retrieves all invoices from the database.
     *
     * @return A [Flow] emitting a list of all [Invoice] objects.
     */
    fun getAllInvoices(): Flow<List<Invoice>> {
        return invoiceDao.getAllInvoices().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Retrieves an invoice by its ID.
     *
     * @param id The ID of the invoice.
     * @return The [Invoice] with the specified ID, or null if not found.
     */
    suspend fun getInvoiceById(id: Long): Invoice? {
        return invoiceDao.getInvoiceById(id)?.toDomainModel()
    }

    /**
     * Inserts a new invoice into the database.
     *
     * @param invoice The [Invoice] to insert.
     * @return The row ID of the newly inserted invoice.
     */
    suspend fun insertInvoice(invoice: Invoice): Long {
        return invoiceDao.insertInvoice(invoice.toEntity())
    }

    /**
     * Updates an existing invoice in the database.
     *
     * @param invoice The [Invoice] to update.
     */
    suspend fun updateInvoice(invoice: Invoice) {
        invoiceDao.updateInvoice(invoice.toEntity())
    }

    /**
     * Deletes an invoice from the database.
     *
     * @param invoice The [Invoice] to delete.
     */
    suspend fun deleteInvoice(invoice: Invoice) {
        invoiceDao.deleteInvoice(invoice.toEntity())
    }

    /**
     * Retrieves all unpaid credit invoices.
     *
     * @return A [Flow] emitting a list of unpaid credit [Invoice] objects.
     */
    fun getUnpaidCreditInvoices(): Flow<List<Invoice>> {
        return invoiceDao.getUnpaidCreditInvoices().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Calculates the total sales within a specific date range.
     *
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return The total sales amount.
     */
    suspend fun getTotalSalesByDateRange(startDate: Date, endDate: Date): Double {
        return invoiceDao.getTotalSalesByDateRange(startDate, endDate) ?: 0.0
    }

    /**
     * Generates a new, unique invoice number.
     *
     * The format is "INV" followed by a zero-padded 6-digit number.
     *
     * @return A new invoice number string.
     */
    suspend fun generateInvoiceNumber(): String {
        val lastId = invoiceDao.getLastInvoiceId() ?: 0
        val newId = lastId + 1
        return "INV${String.format("%06d", newId)}"
    }

    /**
     * Converts an [InvoiceEntity] to a domain model [Invoice].
     */
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

    /**
     * Converts a domain model [Invoice] to an [InvoiceEntity].
     */
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

    /**
     * Converts an [InvoiceItemEntity] to a domain model [InvoiceItem].
     */
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

    /**
     * Converts a domain model [InvoiceItem] to an [InvoiceItemEntity].
     */
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
