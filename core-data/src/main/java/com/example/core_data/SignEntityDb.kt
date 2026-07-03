package com.example.core_data

import androidx.room.ColumnInfo
import com.example.domain.model.SignEntity

data class SignEntityDb(
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
) {
    fun toDomain(): SignEntity = SignEntity(
        id = originalCategoryId.toIntOrNull() ?: originalCategoryId.hashCode(),
        pddCode = originalCategoryId,
        title = title,
        ttsTitle = ttsTitle,
        description = description,
        svgPath = photoPath
    )
}
