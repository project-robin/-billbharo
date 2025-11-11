package com.billbharo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.billbharo.data.local.converters.Converters

/**
 * Represents an item in the product catalog.
 *
 * This data class defines the schema for the `items` table.
 *
 * @property id The unique identifier for the item.
 * @property name The name of the item in English.
 * @property hindiName The name of the item in Hindi (optional).
 * @property marathiName The name of the item in Marathi (optional).
 * @property alternateNames A list of alternate names for fuzzy matching in voice recognition.
 * @property category The category of the item.
 * @property defaultRate The default selling price of the item.
 * @property unit The unit of measurement for the item (e.g., kg, piece, liter).
 * @property hsnCode The Harmonized System of Nomenclature (HSN) code for the item (optional).
 * @property gstRate The Goods and Services Tax rate applicable to the item.
 * @property barcode The barcode of the item (optional).
 */
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
