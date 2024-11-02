package com.example.smartspend

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.math.BigDecimal

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val categoryID: Int = 0,
    val categoryName: String,
    val colorCode: String,
    val userID: Int,
    val maxBudget: BigDecimal,
    val isSynced: Boolean = false
)
@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategories(categories: List<CategoryEntity>)

    @Update
    fun updateCategory(category: CategoryEntity)

    @Delete
    fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE userID = :userID")
    fun getCategoriesByUser(userID: Int): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE userID = :userID AND isSynced = 0")
    fun getUnsyncedCategories(userID: Int): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE userID = :userID AND isSynced = 1")
    fun deleteSyncedCategoriesByUser(userID: Int): Int

    @Query("DELETE FROM categories WHERE userID = :userID")
    fun deleteCategoriesByUser(userID: Int): Int
}
