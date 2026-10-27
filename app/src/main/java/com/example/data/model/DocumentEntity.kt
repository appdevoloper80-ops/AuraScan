package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val documentType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pageImagePaths: String, // Comma-separated paths
    val extractedText: String = "",
    val language: String = "English",
    val confidence: Float = 1.0f,
    val structuredJson: String = "" // Full JSON payload for smart actions
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long? = null,
    val merchant: String,
    val amount: Double,
    val category: String, // Food, Travel, Shopping, Utilities, Other
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val subtotal: Double = 0.0,
    val gstAmount: Double = 0.0
)
