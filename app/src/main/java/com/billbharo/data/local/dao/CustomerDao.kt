package com.billbharo.data.local.dao

import androidx.room.*
import com.billbharo.data.local.entities.CustomerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the `customers` table.
 *
 * This interface provides the methods for interacting with the customer data in the database,
 * including creating, reading, updating, and deleting customers.
 */
@Dao
interface CustomerDao {
    /**
     * Inserts a new customer into the database or replaces an existing one.
     *
     * @param customer The [CustomerEntity] to insert or replace.
     * @return The row ID of the newly inserted customer.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    /**
     * Retrieves all customers from the database, ordered by name.
     *
     * @return A [Flow] emitting a list of all [CustomerEntity] objects.
     */
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    /**
     * Retrieves a customer by their ID.
     *
     * @param customerId The ID of the customer to retrieve.
     * @return The [CustomerEntity] with the specified ID, or null if not found.
     */
    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: Long): CustomerEntity?

    /**
     * Retrieves a customer by their phone number.
     *
     * @param phone The phone number of the customer to retrieve.
     * @return The [CustomerEntity] with the specified phone number, or null if not found.
     */
    @Query("SELECT * FROM customers WHERE phone = :phone")
    suspend fun getCustomerByPhone(phone: String): CustomerEntity?

    /**
     * Searches for customers by name or phone number.
     *
     * @param query The search query to match against customer names and phone numbers.
     * @return A [Flow] emitting a list of matching [CustomerEntity] objects.
     */
    @Query("""
        SELECT * FROM customers 
        WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    /**
     * Retrieves all customers with a positive credit amount, ordered by the credit amount.
     *
     * @return A [Flow] emitting a list of [CustomerEntity] objects with outstanding credit.
     */
    @Query("SELECT * FROM customers WHERE totalCreditAmount > 0 ORDER BY totalCreditAmount DESC")
    fun getCustomersWithCredit(): Flow<List<CustomerEntity>>

    /**
     * Calculates the total credit amount across all customers.
     *
     * @return The sum of all credit amounts, or null if there are no customers.
     */
    @Query("SELECT SUM(totalCreditAmount) FROM customers")
    suspend fun getTotalCreditAmount(): Double?

    /**
     * Updates an existing customer in the database.
     *
     * @param customer The [CustomerEntity] to update.
     */
    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    /**
     * Deletes a customer from the database.
     *
     * @param customer The [CustomerEntity] to delete.
     */
    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)
}
