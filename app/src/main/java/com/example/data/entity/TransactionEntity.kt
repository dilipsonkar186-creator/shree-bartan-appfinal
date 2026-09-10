package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index("customerId"), Index("folderId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val folderId: Long,
    val type: String, // "GOODS_PROVIDED" or "PAYMENT_DEPOSIT" or "GOODS_RETURNED"
    val itemDescription: String,
    val quantity: Int = 1,
    val unitType: String = "pcs",
    val quantityDouble: Double = 1.0,
    val unitPrice: Double = 0.0,
    val totalAmount: Double = 0.0,
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)
