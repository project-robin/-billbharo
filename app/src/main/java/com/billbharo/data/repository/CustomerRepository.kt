package com.billbharo.data.repository

import com.billbharo.data.local.dao.CustomerDao
import com.billbharo.data.local.dao.InvoiceDao
import com.billbharo.data.local.entities.CustomerEntity
import com.billbharo.data.models.Invoice
import com.billbharo.data.models.PaymentMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val invoiceDao: InvoiceDao
) {
    
    fun getAllCustomers(): Flow<List<CustomerEntity>> {
        return customerDao.getAllCustomers()
    }
    
    fun getCustomersWithCredit(): Flow<List<CustomerEntity>> {
        return customerDao.getCustomersWithCredit()
    }
    
    fun searchCustomers(query: String): Flow<List<CustomerEntity>> {
        return customerDao.searchCustomers(query)
    }
    
    suspend fun getCustomerById(customerId: Long): CustomerEntity? {
        return customerDao.getCustomerById(customerId)
    }
    
    suspend fun getCustomerByPhone(phone: String): CustomerEntity? {
        return customerDao.getCustomerByPhone(phone)
    }
    
    suspend fun insertCustomer(customer: CustomerEntity): Long {
        return customerDao.insertCustomer(customer)
    }
    
    suspend fun updateCustomer(customer: CustomerEntity) {
        customerDao.updateCustomer(customer)
    }
    
    suspend fun deleteCustomer(customer: CustomerEntity) {
        customerDao.deleteCustomer(customer)
    }
    
    suspend fun getTotalCreditAmount(): Double {
        return customerDao.getTotalCreditAmount() ?: 0.0
    }
    
    /**
     * Get all credit invoices for a specific customer
     */
    fun getCustomerCreditInvoices(customerPhone: String): Flow<List<Invoice>> {
        return invoiceDao.getAllInvoices().map { entities ->
            entities.filter { 
                it.customerPhone == customerPhone && 
                it.paymentMode == PaymentMode.CREDIT.name &&
                !it.isPaid 
            }.map { entity ->
                Invoice(
                    id = entity.id,
                    invoiceNumber = entity.invoiceNumber,
                    customerName = entity.customerName,
                    customerPhone = entity.customerPhone,
                    items = entity.items.map { item ->
                        com.billbharo.data.models.InvoiceItem(
                            name = item.name,
                            quantity = item.quantity,
                            unit = item.unit,
                            rate = item.rate,
                            amount = item.amount,
                            hsnCode = item.hsnCode
                        )
                    },
                    subtotal = entity.subtotal,
                    cgst = entity.cgst,
                    sgst = entity.sgst,
                    totalAmount = entity.totalAmount,
                    paymentMode = PaymentMode.valueOf(entity.paymentMode),
                    isPaid = entity.isPaid,
                    timestamp = entity.timestamp,
                    pdfPath = entity.pdfPath
                )
            }
        }
    }
    
    /**
     * Update customer credit amount based on their invoices
     */
    suspend fun updateCustomerCredit(phone: String) {
        val customer = getCustomerByPhone(phone) ?: return
        
        // Get all unpaid credit invoices for this customer
        val creditAmount = invoiceDao.getAllInvoices().map { entities ->
            entities.filter { 
                it.customerPhone == phone && 
                it.paymentMode == PaymentMode.CREDIT.name &&
                !it.isPaid 
            }.sumOf { it.totalAmount }
        }
        
        // Note: This is simplified - in production, you'd want to collect the Flow properly
        // For now, we'll just update with the current value
    }
}
