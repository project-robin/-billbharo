package com.billbharo.data.local.dao

import androidx.room.*
import com.billbharo.data.local.entities.InvoiceEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface InvoiceDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity): Long
    
    @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>
    
    @Query("SELECT * FROM invoices WHERE id = :invoiceId")
    suspend fun getInvoiceById(invoiceId: Long): InvoiceEntity?
    
    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber")
    suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceEntity?
    
    @Query("""
        SELECT * FROM invoices 
        WHERE timestamp BETWEEN :startDate AND :endDate 
        ORDER BY timestamp DESC
    """)
    fun getInvoicesByDateRange(startDate: Date, endDate: Date): Flow<List<InvoiceEntity>>
    
    @Query("""
        SELECT * FROM invoices 
        WHERE paymentMode = 'CREDIT' AND isPaid = 0 
        ORDER BY timestamp DESC
    """)
    fun getUnpaidCreditInvoices(): Flow<List<InvoiceEntity>>
    
    @Query("""
        SELECT SUM(totalAmount) FROM invoices 
        WHERE timestamp BETWEEN :startDate AND :endDate AND isPaid = 1
    """)
    suspend fun getTotalSalesByDateRange(startDate: Date, endDate: Date): Double?
    
    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)
    
    @Delete
    suspend fun deleteInvoice(invoice: InvoiceEntity)
    
    @Query("SELECT MAX(id) FROM invoices")
    suspend fun getLastInvoiceId(): Long?
}
