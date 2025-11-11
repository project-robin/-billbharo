package com.billbharo.data.local.dao

import androidx.room.*
import com.billbharo.data.local.entities.InventoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the `inventory` table.
 *
 * This interface provides the methods for interacting with the inventory data in the database,
 * including creating, reading, updating, and deleting inventory records.
 */
@Dao
interface InventoryDao {
    /**
     * Inserts a new inventory record into the database or replaces an existing one.
     *
     * @param inventory The [InventoryEntity] to insert or replace.
     * @return The row ID of the newly inserted inventory record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: InventoryEntity): Long

    /**
     * Retrieves all inventory records, joined with their corresponding items, ordered by item name.
     *
     * @return A [Flow] emitting a list of all [InventoryEntity] objects.
     */
    @Query("""
        SELECT inventory.* FROM inventory 
        INNER JOIN items ON inventory.itemId = items.id 
        ORDER BY items.name ASC
    """)
    fun getAllInventory(): Flow<List<InventoryEntity>>

    /**
     * Retrieves an inventory record by its associated item ID.
     *
     * @param itemId The ID of the item to retrieve the inventory for.
     * @return The [InventoryEntity] for the specified item, or null if not found.
     */
    @Query("SELECT * FROM inventory WHERE itemId = :itemId")
    suspend fun getInventoryByItemId(itemId: Long): InventoryEntity?

    /**
     * Retrieves all inventory records where the current stock is at or below the reorder level.
     *
     * @return A [Flow] emitting a list of low-stock [InventoryEntity] objects.
     */
    @Query("SELECT * FROM inventory WHERE currentStock <= reorderLevel")
    fun getLowStockItems(): Flow<List<InventoryEntity>>

    /**
     * Updates an existing inventory record in the database.
     *
     * @param inventory The [InventoryEntity] to update.
     */
    @Update
    suspend fun updateInventory(inventory: InventoryEntity)

    /**
     * Deletes an inventory record from the database.
     *
     * @param inventory The [InventoryEntity] to delete.
     */
    @Delete
    suspend fun deleteInventory(inventory: InventoryEntity)
}
