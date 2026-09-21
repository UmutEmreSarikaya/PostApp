package com.uesar.postapp.core.db.di

import android.content.Context
import androidx.room.Room
import com.uesar.postapp.core.db.PostAppDatabase
import com.uesar.postapp.core.db.PostDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PostAppDatabase = Room.databaseBuilder(
        context,
        PostAppDatabase::class.java,
        "post-app.db"
    ).build()

    @Provides
    fun providePostDao(database: PostAppDatabase): PostDao = database.postDao()
}
