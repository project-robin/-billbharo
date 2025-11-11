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

/**
 * Repository for managing customer-related data.
 *
 * This class provides a clean API for accessing and modifying customer data, abstracting
 * the underlying data sources (in this case, [CustomerDao] and [InvoiceDao]).
 *
 * @property customerDao The DAO for accessing customer data.
 * @property invoiceDao The DAO for accessing invoice data.
 */
@Singleton
class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val invoiceDao: InvoiceDao
) {
    /**
     * Retrieves all customers from the database.
     *
     * @return A [Flow] emitting a list of all [CustomerEntity] objects.
     */
    fun getAllCustomers(): Flow<List<CustomerEntity>> {
        return customerDao.getAllCustomers()
    }

    /**
     * Retrieves all customers with outstanding credit.
     *
     * @return A [Flow] emitting a list of [CustomerEntity] objects with credit.
     */
    fun getCustomersWithCredit(): Flow<List<CustomerEntity>> {
        return customerDao.getCustomersWithCredit()
    }

    /**
     * Searches for customers by name or phone number.
     *
     * @param query The search query.
     * @return A [Flow] emitting a list of matching [CustomerEntity] objects.
     */
    fun searchCustomers(query: String): Flow<List<CustomerEntity>> {
        return customerDao.searchCustomers(query)
    }

    /**
     * Retrieves a customer by their ID.
     *
     * @param customerId The ID of the customer.
     * @return The [CustomerEntity] with the specified ID, or null if not found.
     */
    suspend fun getCustomerById(customerId: Long): CustomerEntity? {
        return customerDao.getCustomerById(customerId)
    }

    /**
     * Retrieves a customer by their phone number.
     *
     * @param phone The phone number of the customer.
     * @return The [CustomerEntity] with the specified phone number, or null if not found.
     */
    suspend fun getCustomerByPhone(phone: String): CustomerEntity? {
        return customerDao.getCustomerByPhone(phone)
    }

    /**
     * Inserts a new customer into the database.
     *
     * @param customer The [CustomerEntity] to insert.
     * @return The row ID of the newly inserted customer.
     */
    suspend fun insertCustomer(customer: CustomerEntity): Long {
        return customerDao.insertCustomer(customer)
    }

    /**
     * Updates an existing customer in the database.
     *
     * @param customer The [CustomerEntity] to update.
     */
    suspend fun updateCustomer(customer: CustomerEntity) {
        customerDao.updateCustomer(customer)
    }

    /**
     * Deletes a customer from the database.
     *
     * @param customer The [CustomerEntity] to delete.
     */
    suspend fun deleteCustomer(customer: CustomerEntity) {
        customerDao.deleteCustomer(customer)
    }

    /**
     * Calculates the total credit amount across all customers.
     *
     * @return The total credit amount.
     */
    suspend fun getTotalCreditAmount(): Double {
        return customerDao.getTotalCreditAmount() ?: 0.0
    }

    /**
     * Retrieves all unpaid credit invoices for a specific customer.
     *
     * @param customerPhone The phone number of the customer.
     * @return A [Flow] emitting a list of unpaid credit [Invoice] objects.
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
     * Updates a customer's credit amount based on their unpaid invoices. (This is a placeholder and does not currently update the customer)
     *
     * @param phone The phone number of the customer to update.
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
