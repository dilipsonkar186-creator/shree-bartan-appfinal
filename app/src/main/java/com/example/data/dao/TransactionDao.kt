package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY dateMillis ASC, id ASC")
    suspend fun getTransactionsForCustomerList(customerId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE folderId = :folderId ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsForFolder(folderId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE folderId = :folderId ORDER BY dateMillis ASC, id ASC")
    suspend fun getTransactionsForFolderList(folderId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis ASC, id ASC")
    suspend fun getAllTransactionsDirect(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("UPDATE transactions SET folderId = :newFolderId WHERE customerId = :customerId")
    suspend fun updateTransactionsFolderForCustomer(customerId: Long, newFolderId: Long)
}
