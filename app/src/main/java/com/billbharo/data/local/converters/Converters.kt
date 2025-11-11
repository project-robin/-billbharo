package com.billbharo.data.local.converters

import androidx.room.TypeConverter
import com.billbharo.data.local.entities.InvoiceItemEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * Type converters for Room to handle data types that are not natively supported.
 *
 * This class provides methods to convert complex data types, such as [Date] and [List],
 * into formats that can be stored in the Room database.
 */
class Converters {
    private val gson = Gson()

    /**
     * Converts a timestamp (Long) from the database to a [Date] object.
     *
     * @param value The timestamp value from the database.
     * @return The corresponding [Date] object, or null if the timestamp is null.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Converts a [Date] object to a timestamp (Long) for storage in the database.
     *
     * @param date The [Date] object to convert.
     * @return The timestamp representation of the date, or null if the date is null.
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    /**
     * Converts a list of [InvoiceItemEntity] objects to a JSON string.
     *
     * @param value The list of [InvoiceItemEntity] objects.
     * @return A JSON string representation of the list.
     */
    @TypeConverter
    fun fromInvoiceItemList(value: List<InvoiceItemEntity>): String {
        return gson.toJson(value)
    }

    /**
     * Converts a JSON string back to a list of [InvoiceItemEntity] objects.
     *
     * @param value The JSON string from the database.
     * @return The deserialized list of [InvoiceItemEntity] objects.
     */
    @TypeConverter
    fun toInvoiceItemList(value: String): List<InvoiceItemEntity> {
        val listType = object : TypeToken<List<InvoiceItemEntity>>() {}.type
        return gson.fromJson(value, listType)
    }

    /**
     * Converts a list of strings to a JSON string.
     *
     * @param value The list of strings.
     * @return A JSON string representation of the list.
     */
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    /**
     * Converts a JSON string back to a list of strings.
     *
     * @param value The JSON string from the database.
     * @return The deserialized list of strings.
     */
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
