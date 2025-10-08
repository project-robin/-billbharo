package com.billbharo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.billbharo.data.local.converters.Converters

@Entity(tableName = "items")
@TypeConverters(Converters::class)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val hindiName: String?,
    val marathiName: String?,
    val alternateNames: List<String>, // For voice recognition fuzzy matching
    val category: String,
    val defaultRate: Double,
    val unit: String,
    val hsnCode: String?,
    val gstRate: Double = 0.0,
    val barcode: String?
)
