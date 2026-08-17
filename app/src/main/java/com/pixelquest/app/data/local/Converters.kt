package com.pixelquest.app.data.local

import androidx.room.TypeConverter
import com.pixelquest.app.domain.model.DifficultyLevel
import com.pixelquest.app.domain.model.RecurrenceType
import com.pixelquest.app.domain.model.TaskCategory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.format(timeFormatter)
    }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it, timeFormatter) }
    }

    @TypeConverter
    fun fromRecurrenceType(type: RecurrenceType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toRecurrenceType(value: String?): RecurrenceType? {
        return value?.let { RecurrenceType.valueOf(it) }
    }

    @TypeConverter
    fun fromTaskCategory(category: TaskCategory?): String? {
        return category?.name
    }

    @TypeConverter
    fun toTaskCategory(value: String?): TaskCategory? {
        return value?.let { TaskCategory.valueOf(it) }
    }

    @TypeConverter
    fun fromDifficultyLevel(level: DifficultyLevel?): String? {
        return level?.name
    }

    @TypeConverter
    fun toDifficultyLevel(value: String?): DifficultyLevel? {
        return value?.let { DifficultyLevel.valueOf(it) }
    }
}
