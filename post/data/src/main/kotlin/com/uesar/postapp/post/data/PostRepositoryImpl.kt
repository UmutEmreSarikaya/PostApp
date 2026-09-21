package com.uesar.postapp.post.data

import com.uesar.postapp.core.db.PostDao
import com.uesar.postapp.core.db.PostEntity
import com.uesar.postapp.post.domain.model.Post
import com.uesar.postapp.post.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class PostRepositoryImpl @Inject constructor(
    private val postService: PostService,
    private val postDao: PostDao
) : PostRepository {

    override fun observePosts(): Flow<List<Post>> = postDao.observePosts()
        .map { posts -> posts.map { it.toDomain() } }
        .distinctUntilChanged()

    override suspend fun syncPosts(forceRefresh: Boolean) {
        if (!forceRefresh && postDao.isInitialFetchComplete()) return

        val remotePosts = postService.getPosts().map { it.toEntity() }
        if (forceRefresh) {
            postDao.replacePosts(remotePosts)
        } else {
            postDao.cacheInitialPosts(remotePosts)
        }
    }

    override suspend fun getPost(postId: Int): Post? {
        return postDao.getPost(postId)?.toDomain()
    }

    override suspend fun updatePost(post: Post) {
        postDao.updatePost(
            PostEntity(id = post.id, title = post.title, body = post.body)
        )
    }

    override suspend fun deletePost(postId: Int) {
        postDao.deletePost(postId)
    }

    override suspend fun restorePost(post: Post) {
        postDao.upsertPost(PostEntity(id = post.id, title = post.title, body = post.body))
    }

    private fun PostDto.toEntity() = PostEntity(
        id = id,
        title = title,
        body = body
    )

    private fun PostEntity.toDomain() = Post(
        id = id,
        title = title,
        body = body
    )
}
