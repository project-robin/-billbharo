package com.billbharo.di

import android.content.Context
import androidx.room.Room
import com.billbharo.data.local.BillBharoDatabase
import com.billbharo.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBillBharoDatabase(
        @ApplicationContext context: Context
    ): BillBharoDatabase {
        return Room.databaseBuilder(
            context,
            BillBharoDatabase::class.java,
            "billbharo_database"
        ).build()
    }

    @Provides
    fun provideInvoiceDao(database: BillBharoDatabase): InvoiceDao {
        return database.invoiceDao()
    }

    @Provides
    fun provideItemDao(database: BillBharoDatabase): ItemDao {
        return database.itemDao()
    }

    @Provides
    fun provideCustomerDao(database: BillBharoDatabase): CustomerDao {
        return database.customerDao()
    }

    @Provides
    fun provideInventoryDao(database: BillBharoDatabase): InventoryDao {
        return database.inventoryDao()
    }
}
