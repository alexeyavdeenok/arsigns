package com.example.core_data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signs")
data class SignEntityDb(
    @PrimaryKey
    @ColumnInfo(name = "Original_Category_ID")
    val originalCategoryId: String,

    @ColumnInfo(name = "GOST_Sign_Number")
    val gostSignNumber: String,

    @ColumnInfo(name = "название")
    val title: String,

    @ColumnInfo(name = "название для прочтения")
    val ttsTitle: String,

    @ColumnInfo(name = "описание")
    val description: String,

    @ColumnInfo(name = "путь к фото")
    val photoPath: String
)
