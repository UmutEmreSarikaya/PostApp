package com.uesar.postapp.post.data

import com.uesar.postapp.post.domain.model.Post
import com.uesar.postapp.post.domain.repository.PostRepository
import javax.inject.Inject

internal class PostRepositoryImpl @Inject constructor(
    private val postService: PostService
) : PostRepository {
    override suspend fun getPosts(): List<Post> {
        return postService.getPosts().map { dto ->
            Post(
                id = dto.id,
                title = dto.title,
                body = dto.body
            )
        }
    }
}
