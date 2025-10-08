package com.billbharo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

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
