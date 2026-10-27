package com.example.data.repository

import com.example.data.local.DocumentDao
import com.example.data.local.ExpenseDao
import com.example.data.model.DocumentEntity
import com.example.data.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val documentDao: DocumentDao) {
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> =
        if (query.isBlank()) documentDao.getAllDocuments() else documentDao.searchDocuments(query.trim())

    fun filterByType(type: String): Flow<List<DocumentEntity>> =
        if (type == "all" || type.isBlank()) documentDao.getAllDocuments() else documentDao.getDocumentsByType(type)

    suspend fun getDocumentById(id: Long): DocumentEntity? = documentDao.getDocumentById(id)

    suspend fun insert(document: DocumentEntity): Long = documentDao.insertDocument(document)

    suspend fun update(document: DocumentEntity) = documentDao.updateDocument(document)

    suspend fun deleteById(id: Long) = documentDao.deleteDocumentById(id)

    suspend fun clearAll() = documentDao.clearAll()
}

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    suspend fun insert(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)

    suspend fun update(expense: ExpenseEntity) = expenseDao.updateExpense(expense)

    suspend fun deleteById(id: Long) = expenseDao.deleteExpenseById(id)

    suspend fun clearAll() = expenseDao.clearAll()
}
