package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [Index("folderId")]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long,
    val name: String,
    val phone: String = "",
    val bookNumber: String = "",
    val pageNumber: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val status: String = "Active",
    val photoUri: String? = null,
    val documentType: String = "",
    val documentNumber: String = "",
    val documentPhotoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val smsNotificationsEnabled: Boolean = true,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
