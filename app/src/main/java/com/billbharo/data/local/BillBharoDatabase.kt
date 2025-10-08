package com.billbharo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.billbharo.data.local.converters.Converters
import com.billbharo.data.local.dao.*
import com.billbharo.data.local.entities.*

@Database(
    entities = [
        InvoiceEntity::class,
        ItemEntity::class,
        CustomerEntity::class,
        InventoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BillBharoDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun itemDao(): ItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun inventoryDao(): InventoryDao
}
