package com.billbharo.data.repository

import com.billbharo.data.local.dao.ItemDao
import com.billbharo.data.local.entities.ItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao
) {
    fun getAllItems(): Flow<List<ItemEntity>> = itemDao.getAllItems()

    suspend fun getItemById(id: Long): ItemEntity? = itemDao.getItemById(id)

    fun searchItems(query: String): Flow<List<ItemEntity>> = itemDao.searchItems(query)

    suspend fun insertItem(item: ItemEntity): Long = itemDao.insertItem(item)

    suspend fun insertItems(items: List<ItemEntity>) = itemDao.insertItems(items)

    suspend fun updateItem(item: ItemEntity) = itemDao.updateItem(item)

    suspend fun deleteItem(item: ItemEntity) = itemDao.deleteItem(item)
}
