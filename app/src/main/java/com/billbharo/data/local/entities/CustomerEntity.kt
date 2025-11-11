package com.billbharo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a customer in the database.
 *
 * This data class defines the schema for the `customers` table.
 *
 * @property id The unique identifier for the customer.
 * @property name The name of the customer.
 * @property phone The customer's phone number (optional).
 * @property address The customer's address (optional).
 * @property totalCreditAmount The total outstanding credit amount for the customer.
 * @property lastPurchaseDate The date of the customer's last purchase (optional).
 * @property notes Additional notes about the customer (optional).
 */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String?,
    val address: String?,
    val totalCreditAmount: Double = 0.0,
    val lastPurchaseDate: Date?,
    val notes: String?
)
