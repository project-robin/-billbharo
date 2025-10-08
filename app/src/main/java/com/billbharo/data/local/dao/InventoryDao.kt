package com.billbharo.data.local.dao

import androidx.room.*
import com.billbharo.data.local.entities.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: InventoryEntity): Long
    
    @Query("""
        SELECT inventory.* FROM inventory 
        INNER JOIN items ON inventory.itemId = items.id 
        ORDER BY items.name ASC
    """)
    fun getAllInventory(): Flow<List<InventoryEntity>>
    
    @Query("SELECT * FROM inventory WHERE itemId = :itemId")
    suspend fun getInventoryByItemId(itemId: Long): InventoryEntity?
    
    @Query("SELECT * FROM inventory WHERE currentStock <= reorderLevel")
    fun getLowStockItems(): Flow<List<InventoryEntity>>
    
    @Update
    suspend fun updateInventory(inventory: InventoryEntity)
    
    @Delete
    suspend fun deleteInventory(inventory: InventoryEntity)
}
