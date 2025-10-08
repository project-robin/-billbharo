package com.billbharo.data.local.converters

import androidx.room.TypeConverter
import com.billbharo.data.local.entities.InvoiceItemEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromInvoiceItemList(value: List<InvoiceItemEntity>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toInvoiceItemList(value: String): List<InvoiceItemEntity> {
        val listType = object : TypeToken<List<InvoiceItemEntity>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
