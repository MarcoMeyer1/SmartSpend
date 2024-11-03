package com.example.smartspend

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import androidx.room.*
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import androidx.room.TypeConverter
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val goalID: Int = 0,
    val userID: Int,
    val goalName: String,
    val totalAmount: BigDecimal,
    val savedAmount: BigDecimal = BigDecimal.ZERO,
    val completionDate: String? = null,
    val isSynced: Boolean = false
)
@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGoal(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGoals(goals: List<GoalEntity>)

    @Update
    fun updateGoal(goal: GoalEntity)

    @Delete
    fun deleteGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE userID = :userID")
    fun getGoalsByUser(userID: Int): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE userID = :userID AND isSynced = 0")
    fun getUnsyncedGoals(userID: Int): List<GoalEntity>

    @Query("DELETE FROM goals WHERE userID = :userID AND isSynced = 1")
    fun deleteSyncedGoalsByUser(userID: Int): Int

    @Query("DELETE FROM goals WHERE userID = :userID")
    fun deleteGoalsByUser(userID: Int): Int
}


@Database(entities = [GoalEntity::class, CategoryEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartspend_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration from version 1 to 2 (already existing)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add isSynced column with default value false (0) to goals table
                database.execSQL("ALTER TABLE goals ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migration from version 2 to 3 (new migration for categories)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create categories table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        categoryID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryName TEXT NOT NULL,
                        colorCode TEXT NOT NULL,
                        userID INTEGER NOT NULL,
                        maxBudget TEXT NOT NULL,
                        isSynced INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
    }
}

class Converters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }
}
