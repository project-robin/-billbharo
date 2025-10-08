package com.billbharo.data.local.dao

import androidx.room.*
import com.billbharo.data.local.entities.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)
    
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ItemEntity>>
    
    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): ItemEntity?
    
    @Query("""
        SELECT * FROM items 
        WHERE name LIKE '%' || :query || '%' 
        OR hindiName LIKE '%' || :query || '%' 
        OR marathiName LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchItems(query: String): Flow<List<ItemEntity>>
    
    @Query("SELECT * FROM items WHERE category = :category ORDER BY name ASC")
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>>
    
    @Update
    suspend fun updateItem(item: ItemEntity)
    
    @Delete
    suspend fun deleteItem(item: ItemEntity)
}
