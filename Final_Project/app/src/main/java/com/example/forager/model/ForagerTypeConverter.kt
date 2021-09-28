package com.example.forager.model

import androidx.room.TypeConverter
import java.util.*

class ForagerTypeConverter {

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(convertToDate: Long?): Date? {
        return convertToDate?.let {
            Date(it)
        }
    }

}