package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CustomerDao
import com.example.data.dao.FolderDao
import com.example.data.dao.ProductDao
import com.example.data.dao.TransactionDao
import com.example.data.entity.CustomerEntity
import com.example.data.entity.FolderEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.TransactionEntity

@Database(
    entities = [FolderEntity::class, CustomerEntity::class, TransactionEntity::class, ProductEntity::class],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val CREATE_PRODUCTS_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS `products` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `metalType` TEXT NOT NULL,
                `sizeSpec` TEXT NOT NULL,
                `mrp` REAL NOT NULL,
                `sellingPrice` REAL NOT NULL,
                `description` TEXT NOT NULL,
                `imageUris` TEXT NOT NULL,
                `inStock` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
        """

        // Safe migrations that preserve existing tables and customer data across all versions
        private val MIGRATION_1_10 = object : Migration(1, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_PRODUCTS_TABLE_SQL)
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_PRODUCTS_TABLE_SQL)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "customer_folders_db"
                )
                    .addMigrations(MIGRATION_1_10, MIGRATION_9_10)
                    // NEVER wipe existing data during app updates or edits
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
