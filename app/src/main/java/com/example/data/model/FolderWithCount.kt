package com.example.data.model

import androidx.room.Embedded
import com.example.data.entity.FolderEntity

data class FolderWithCount(
    @Embedded val folder: FolderEntity,
    val customerCount: Int
)

data class FolderDeletionInfo(
    val canDelete: Boolean,
    val folder: FolderEntity,
    val customerCount: Int,
    val totalPendingDues: Double,
    val totalGoods: Double,
    val totalPaid: Double,
    val customersWithDuesCount: Int,
    val customerDuesList: List<Pair<String, Double>> = emptyList()
)
