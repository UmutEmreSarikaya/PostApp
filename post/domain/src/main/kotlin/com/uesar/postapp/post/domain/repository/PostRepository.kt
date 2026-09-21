package com.uesar.postapp.post.domain.repository

import com.uesar.postapp.post.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun observePosts(): Flow<List<Post>>
    suspend fun syncPosts(forceRefresh: Boolean = false)
    suspend fun getPost(postId: Int): Post?
    suspend fun updatePost(post: Post)
    suspend fun deletePost(postId: Int)
    suspend fun restorePost(post: Post)
}
