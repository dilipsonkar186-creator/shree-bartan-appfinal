package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.FolderEntity
import com.example.data.model.FolderWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("""
        SELECT f.*, COUNT(c.id) AS customerCount 
        FROM folders f 
        LEFT JOIN customers c ON f.id = c.folderId AND c.isDeleted = 0
        WHERE f.isDeleted = 0
        GROUP BY f.id 
        ORDER BY f.name COLLATE NOCASE ASC
    """)
    fun getFoldersWithCount(): Flow<List<FolderWithCount>>

    @Query("""
        SELECT f.*, COUNT(c.id) AS customerCount 
        FROM folders f 
        LEFT JOIN customers c ON f.id = c.folderId
        WHERE f.isDeleted = 1
        GROUP BY f.id 
        ORDER BY f.deletedAt DESC
    """)
    fun getDeletedFolders(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    fun getFolderById(folderId: Long): Flow<FolderEntity?>

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersDirect(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("UPDATE folders SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :folderId")
    suspend fun softDeleteFolder(folderId: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE folders SET isDeleted = 0, deletedAt = NULL WHERE id = :folderId")
    suspend fun restoreFolder(folderId: Long)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun permanentlyDeleteFolder(folderId: Long)

    @Query("DELETE FROM folders WHERE isDeleted = 1")
    suspend fun emptyDeletedFolders()

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}
