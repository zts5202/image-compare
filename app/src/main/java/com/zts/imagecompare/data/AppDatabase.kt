package com.zts.imagecompare.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ComparisonEntity::class, GeneratedImageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun comparisonDao(): ComparisonDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE comparisons ADD COLUMN originalCropPath TEXT")
                db.execSQL("ALTER TABLE comparisons ADD COLUMN originalCropLeft REAL")
                db.execSQL("ALTER TABLE comparisons ADD COLUMN originalCropTop REAL")
                db.execSQL("ALTER TABLE comparisons ADD COLUMN originalCropRight REAL")
                db.execSQL("ALTER TABLE comparisons ADD COLUMN originalCropBottom REAL")
                db.execSQL("ALTER TABLE comparisons ADD COLUMN originalCropAspect TEXT")
                db.execSQL("ALTER TABLE comparisons ADD COLUMN originalCropWidth INTEGER")
                db.execSQL("ALTER TABLE comparisons ADD COLUMN originalCropHeight INTEGER")
                db.execSQL("ALTER TABLE comparisons ADD COLUMN originalCropBytes INTEGER")
                db.execSQL("ALTER TABLE comparisons ADD COLUMN originalCropSharpness INTEGER")
                db.execSQL("ALTER TABLE generated_images ADD COLUMN cropPath TEXT")
                db.execSQL("ALTER TABLE generated_images ADD COLUMN cropLeft REAL")
                db.execSQL("ALTER TABLE generated_images ADD COLUMN cropTop REAL")
                db.execSQL("ALTER TABLE generated_images ADD COLUMN cropRight REAL")
                db.execSQL("ALTER TABLE generated_images ADD COLUMN cropBottom REAL")
                db.execSQL("ALTER TABLE generated_images ADD COLUMN cropAspect TEXT")
                db.execSQL("ALTER TABLE generated_images ADD COLUMN cropWidth INTEGER")
                db.execSQL("ALTER TABLE generated_images ADD COLUMN cropHeight INTEGER")
                db.execSQL("ALTER TABLE generated_images ADD COLUMN cropBytes INTEGER")
                db.execSQL("ALTER TABLE generated_images ADD COLUMN cropSharpness INTEGER")
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "image_compare.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}
