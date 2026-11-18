package com.graffitistudio.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProjectEntity::class],
    version = 2,
    exportSchema = false
)
abstract class GraffitiDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE projects ADD COLUMN backgroundSurface TEXT NOT NULL DEFAULT 'SOLID'"
                )
            }
        }

        fun create(context: Context): GraffitiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GraffitiDatabase::class.java,
                "graffiti_studio.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
