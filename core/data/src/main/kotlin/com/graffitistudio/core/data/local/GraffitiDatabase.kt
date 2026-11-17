package com.graffitistudio.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GraffitiDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        fun create(context: Context): GraffitiDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GraffitiDatabase::class.java,
                "graffiti_studio.db"
            ).build()
        }
    }
}
