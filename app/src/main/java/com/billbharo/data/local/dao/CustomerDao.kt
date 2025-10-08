package com.billbharo.data.local.dao

import androidx.room.*
import com.billbharo.data.local.entities.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long
    
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>
    
    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: Long): CustomerEntity?
    
    @Query("SELECT * FROM customers WHERE phone = :phone")
    suspend fun getCustomerByPhone(phone: String): CustomerEntity?
    
    @Query("""
        SELECT * FROM customers 
        WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>
    
    @Query("SELECT * FROM customers WHERE totalCreditAmount > 0 ORDER BY totalCreditAmount DESC")
    fun getCustomersWithCredit(): Flow<List<CustomerEntity>>
    
    @Query("SELECT SUM(totalCreditAmount) FROM customers")
    suspend fun getTotalCreditAmount(): Double?
    
    @Update
    suspend fun updateCustomer(customer: CustomerEntity)
    
    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)
}
