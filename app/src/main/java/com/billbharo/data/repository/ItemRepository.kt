package com.billbharo.data.repository

import com.billbharo.data.local.dao.ItemDao
import com.billbharo.data.local.entities.ItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing item-related data.
 *
 * This class provides a clean API for accessing and modifying item data, abstracting
 * the underlying data source (in this case, [ItemDao]).
 *
 * @property itemDao The DAO for accessing item data.
 */
@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao
) {
    /**
     * Retrieves all items from the database.
     *
     * @return A [Flow] emitting a list of all [ItemEntity] objects.
     */
    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAllItems()

    /**
     * Retrieves an item by its ID.
     *
     * @param id The ID of the item.
     * @return The [ItemEntity] with the specified ID, or null if not found.
     */
    suspend fun getItemById(id: Long): ItemEntity? = itemDao.getItemById(id)

    /**
     * Searches for items by name.
     *
     * @param query The search query.
     * @return A [Flow] emitting a list of matching [ItemEntity] objects.
     */
    fun searchItems(query: String): Flow<List<ItemEntity>> = itemDao.searchItems(query)

    /**
     * Inserts a new item into the database.
     *
     * @param item The [ItemEntity] to insert.
     * @return The row ID of the newly inserted item.
     */
    suspend fun insertItem(item: ItemEntity): Long = itemDao.insertItem(item)

    /**
     * Inserts a list of items into the database.
     *
     * @param items The list of [ItemEntity] objects to insert.
     */
    suspend fun insertItems(items: List<ItemEntity>) = itemDao.insertItems(items)

    /**
     * Updates an existing item in the database.
     *
     * @param item The [ItemEntity] to update.
     */
    suspend fun updateItem(item: ItemEntity) = itemDao.updateItem(item)

    /**
     * Deletes an item from the database.
     *
     * @param item The [ItemEntity] to delete.
     */
    suspend fun deleteItem(item: ItemEntity) = itemDao.deleteItem(item)
}
