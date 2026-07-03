package com.example.core_data.mapper

import com.example.core_data.room.SignEntityDb
import com.example.domain.model.SignEntity

object SignMapper {
    fun SignEntityDb.toDomain(): SignEntity = SignEntity(
        id = originalCategoryId.toIntOrNull() ?: originalCategoryId.hashCode(),
        pddCode = originalCategoryId,
        title = title,
        ttsTitle = ttsTitle,
        description = description,
        svgPath = photoPath
    )
}
