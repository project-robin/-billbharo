package com.billbharo.data.local.dao

import androidx.room.*
import com.billbharo.data.local.entities.InvoiceEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object (DAO) for the `invoices` table.
 *
 * This interface provides the methods for interacting with the invoice data in the database,
 * including creating, reading, updating, and deleting invoices.
 */
@Dao
interface InvoiceDao {
    /**
     * Inserts a new invoice into the database or replaces an existing one.
     *
     * @param invoice The [InvoiceEntity] to insert or replace.
     * @return The row ID of the newly inserted invoice.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    /**
     * Retrieves all invoices from the database, ordered by timestamp in descending order.
     *
     * @return A [Flow] emitting a list of all [InvoiceEntity] objects.
     */
    @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    /**
     * Retrieves an invoice by its ID.
     *
     * @param invoiceId The ID of the invoice to retrieve.
     * @return The [InvoiceEntity] with the specified ID, or null if not found.
     */
    @Query("SELECT * FROM invoices WHERE id = :invoiceId")
    suspend fun getInvoiceById(invoiceId: Long): InvoiceEntity?

    /**
     * Retrieves an invoice by its invoice number.
     *
     * @param invoiceNumber The invoice number to search for.
     * @return The [InvoiceEntity] with the specified invoice number, or null if not found.
     */
    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber")
    suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceEntity?

    /**
     * Retrieves all invoices within a specific date range, ordered by timestamp.
     *
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return A [Flow] emitting a list of [InvoiceEntity] objects within the date range.
     */
    @Query("""
        SELECT * FROM invoices 
        WHERE timestamp BETWEEN :startDate AND :endDate 
        ORDER BY timestamp DESC
    """)
    fun getInvoicesByDateRange(startDate: Date, endDate: Date): Flow<List<InvoiceEntity>>

    /**
     * Retrieves all unpaid credit invoices, ordered by timestamp.
     *
     * @return A [Flow] emitting a list of unpaid [InvoiceEntity] objects with 'CREDIT' payment mode.
     */
    @Query("""
        SELECT * FROM invoices 
        WHERE paymentMode = 'CREDIT' AND isPaid = 0 
        ORDER BY timestamp DESC
    """)
    fun getUnpaidCreditInvoices(): Flow<List<InvoiceEntity>>

    /**
     * Calculates the total sales within a specific date range for paid invoices.
     *
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return The total sales amount, or null if there are no sales in the range.
     */
    @Query("""
        SELECT SUM(totalAmount) FROM invoices 
        WHERE timestamp BETWEEN :startDate AND :endDate AND isPaid = 1
    """)
    suspend fun getTotalSalesByDateRange(startDate: Date, endDate: Date): Double?

    /**
     * Updates an existing invoice in the database.
     *
     * @param invoice The [InvoiceEntity] to update.
     */
    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    /**
     * Deletes an invoice from the database.
     *
     * @param invoice The [InvoiceEntity] to delete.
     */
    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)

    /**
     * Retrieves the ID of the most recently inserted invoice.
     *
     * @return The last inserted invoice ID, or null if the table is empty.
     */
    @Query("SELECT MAX(id) FROM invoices")
    suspend fun getLastInvoiceId(): Long?
}
