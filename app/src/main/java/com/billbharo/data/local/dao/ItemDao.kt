package com.billbharo.data.local.dao

import androidx.room.*
import com.billbharo.data.local.entities.ItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the `items` table.
 *
 * This interface provides the methods for interacting with the item data in the database,
 * including creating, reading, updating, and deleting items.
 */
@Dao
interface ItemDao {
    /**
     * Inserts a new item into the database or replaces an existing one.
     *
     * @param item The [ItemEntity] to insert or replace.
     * @return The row ID of the newly inserted item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    /**
     * Inserts a list of items into the database, replacing any existing items.
     *
     * @param items The list of [ItemEntity] objects to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    /**
     * Retrieves all items from the database, ordered by name.
     *
     * @return A [Flow] emitting a list of all [ItemEntity] objects.
     */
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    /**
     * Retrieves an item by its ID.
     *
     * @param itemId The ID of the item to retrieve.
     * @return The [ItemEntity] with the specified ID, or null if not found.
     */
    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): ItemEntity?

    /**
     * Searches for items by name in English, Hindi, or Marathi.
     *
     * @param query The search query to match against item names.
     * @return A [Flow] emitting a list of matching [ItemEntity] objects.
     */
    @Query("""
        SELECT * FROM items 
        WHERE name LIKE '%' || :query || '%' 
        OR hindiName LIKE '%' || :query || '%' 
        OR marathiName LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    fun searchItems(query: String): Flow<List<ItemEntity>>

    /**
     * Retrieves all items belonging to a specific category, ordered by name.
     *
     * @param category The category to filter by.
     * @return A [Flow] emitting a list of [ItemEntity] objects in the specified category.
     */
    @Query("SELECT * FROM items WHERE category = :category ORDER BY name ASC")
    fun getItemsByCategory(category: String): Flow<List<ItemEntity>>

    /**
     * Updates an existing item in the database.
     *
     * @param item The [ItemEntity] to update.
     */
    @Update
    suspend fun updateItem(item: ItemEntity)

    /**
     * Deletes an item from the database.
     *
     * @param item The [ItemEntity] to delete.
     */
    @Delete
    suspend fun deleteItem(item: ItemEntity)
}
