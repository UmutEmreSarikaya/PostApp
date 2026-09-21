package com.uesar.postapp.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY id ASC")
    fun observePosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPost(postId: Int): PostEntity?

    @Query(
        "SELECT EXISTS(" +
            "SELECT 1 FROM sync_state WHERE syncId = 'posts_initial_fetch'" +
            ")"
    )
    suspend fun isInitialFetchComplete(): Boolean

    @Upsert
    suspend fun upsertPosts(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity): Int

    @Upsert
    suspend fun upsertPost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: Int)

    @Query("DELETE FROM posts")
    suspend fun deletePosts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncState(syncState: SyncStateEntity)

    @Transaction
    suspend fun cacheInitialPosts(posts: List<PostEntity>) {
        upsertPosts(posts)
        insertSyncState(SyncStateEntity(syncId = "posts_initial_fetch"))
    }

    @Transaction
    suspend fun replacePosts(posts: List<PostEntity>) {
        deletePosts()
        upsertPosts(posts)
        insertSyncState(SyncStateEntity(syncId = "posts_initial_fetch"))
    }
}
