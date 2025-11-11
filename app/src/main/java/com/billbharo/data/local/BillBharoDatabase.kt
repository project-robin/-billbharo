package com.billbharo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.billbharo.data.local.converters.Converters
import com.billbharo.data.local.dao.*
import com.billbharo.data.local.entities.*

/**
 * The main Room database for the Bill Bharo application.
 *
 * This abstract class defines the database configuration and serves as the main access point
 * to the persisted data. It lists all the entities and provides abstract methods for accessing
 * the Data Access Objects (DAOs).
 *
 * @property invoiceDao Provides access to invoice-related database operations.
 * @property itemDao Provides access to item-related database operations.
 * @property customerDao Provides access to customer-related database operations.
 * @property inventoryDao Provides access to inventory-related database operations.
 */
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
    /**
     * @return An instance of [InvoiceDao] for accessing invoice data.
     */
    abstract fun invoiceDao(): InvoiceDao

    /**
     * @return An instance of [ItemDao] for accessing item data.
     */
    abstract fun itemDao(): ItemDao

    /**
     * @return An instance of [CustomerDao] for accessing customer data.
     */
    abstract fun customerDao(): CustomerDao

    /**
     * @return An instance of [InventoryDao] for accessing inventory data.
     */
    abstract fun inventoryDao(): InventoryDao
}
