package com.billbharo.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents an inventory record for an item in the database.
 *
 * This data class defines the schema for the `inventory` table and includes a foreign key
 * relationship with the `items` table.
 *
 * @property id The unique identifier for the inventory record.
 * @property itemId The foreign key referencing the ID of the item in the `items` table.
 * @property currentStock The current stock level of the item.
 * @property reorderLevel The stock level at which a reorder should be triggered.
 * @property lastRestockDate The date of the last restock (optional).
 * @property lastRestockQuantity The quantity of the last restock (optional).
 */
@Entity(
    tableName = "inventory",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val currentStock: Double,
    val reorderLevel: Double,
    val lastRestockDate: Date?,
    val lastRestockQuantity: Double?
)
