package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersDirect(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE folderId = :folderId AND isDeleted = 0 ORDER BY name ASC")
    fun getCustomersForFolder(folderId: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE folderId = :folderId AND isDeleted = 0 ORDER BY name ASC")
    suspend fun getCustomersForFolderDirect(folderId: Long): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun getCustomerById(customerId: Long): Flow<CustomerEntity?>

    @Query("""
        SELECT * FROM customers 
        WHERE folderId = :folderId AND isDeleted = 0 AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR bookNumber LIKE '%' || :query || '%' OR pageNumber LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun searchCustomers(folderId: Long, query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :customerId")
    suspend fun softDeleteCustomer(customerId: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET isDeleted = 0, deletedAt = NULL WHERE id = :customerId")
    suspend fun restoreCustomer(customerId: Long)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun permanentlyDeleteCustomer(customerId: Long)

    @Query("DELETE FROM customers WHERE isDeleted = 1")
    suspend fun emptyDeletedCustomers()

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomerById(customerId: Long)

    @Query("UPDATE customers SET folderId = :newFolderId WHERE id = :customerId")
    suspend fun moveCustomerToFolder(customerId: Long, newFolderId: Long)
}
