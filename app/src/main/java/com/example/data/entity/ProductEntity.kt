package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "अन्य", // कुकर, कड़ाही, थाली-सेट, भगोने, नॉन-स्टिक, पूजा सामान, अन्य
    val metalType: String = "स्टेनलेस स्टील", // स्टेनलेस स्टील, पीतल, तांबा, एल्युमिनियम, नॉन-स्टिक, अन्य
    val sizeSpec: String = "", // जैसे: 3L, 5L, 24cm, 1.5kg
    val mrp: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val description: String = "",
    val imageUris: String = "", // Comma-separated list of image URIs/URLs
    val inStock: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
