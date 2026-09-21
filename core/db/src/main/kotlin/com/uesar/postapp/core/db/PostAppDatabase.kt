package com.uesar.postapp.core.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PostEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = false
)
internal abstract class PostAppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
}
